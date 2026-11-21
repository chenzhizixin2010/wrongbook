package com.chenzixin.wrongbook.feature.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.ImageUtils.rotated
import com.chenzixin.wrongbook.core.ImageUtils.scaledToMaxSide
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.QuestionImageStore
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.data.ocr.OcrEngine
import com.chenzixin.wrongbook.data.ocr.VisionQuestionSegmenter
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import com.chenzixin.wrongbook.data.repository.TagRepository
import com.chenzixin.wrongbook.domain.scan.QuestionRegion
import com.chenzixin.wrongbook.domain.scan.QuestionSegmenter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val ocr: OcrEngine,
    private val vision: VisionQuestionSegmenter,
    private val settings: SettingsRepository,
    private val imageStore: QuestionImageStore,
    private val questionRepo: QuestionRepository,
    private val tagRepo: TagRepository,
) : ViewModel() {

    enum class Stage { Idle, Recognizing, Selecting, Saving, Done }

    data class UiState(
        val stage: Stage = Stage.Idle,
        val bitmap: Bitmap? = null,
        val regions: List<QuestionRegion> = emptyList(),
        val selected: Set<Int> = emptySet(),
        val tags: List<TagEntity> = emptyList(),
        val selectedTagIds: Set<Long> = emptySet(),
        val message: String? = null,
        val savedCount: Int = 0,
        val progress: String? = null,
        /** 本次识别用的引擎：AI 识图 / 本地识别 */
        val engine: String = "",
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { tagRepo.observeAll().first() }
                .onSuccess { tags -> _state.update { it.copy(tags = tags) } }
        }
    }

    fun onCaptured(bitmap: Bitmap) {
        if (_state.value.stage == Stage.Recognizing) return
        _state.update { it.copy(stage = Stage.Recognizing, bitmap = bitmap, message = null) }
        viewModelScope.launch {
            try {
                // 横着拍竖版试卷时，先自动转正再识别，否则 OCR 全是竖排碎片
                val upright = autoOrient(bitmap)
                val active = settings.resolveActive()
                val hasProfile = active == null && runCatching {
                    settings.observeProfiles().first().isNotEmpty()
                }.getOrDefault(false)
                var regions: List<QuestionRegion>? = null
                var engine = "本地识别"

                // 第一层：配置了视觉模型就先用它（失败/为空自动降级，但要把原因说出来）
                var visionTried = false
                var visionError: String? = null
                if (active != null && active.profile.visionModel.isNotBlank()) {
                    visionTried = true
                    val outcome = runCatching {
                        vision.segment(active.profile, active.apiKey, upright)
                    }.getOrNull()
                    when (outcome) {
                        is WbResult.Ok -> {
                            val value = outcome.value
                            if (value.any { it.text.isNotBlank() }) {
                                regions = value
                            } else {
                                visionError = "模型返回了题目框但没有文字"
                            }
                        }
                        is WbResult.Err -> visionError = outcome.message
                        null -> visionError = "调用过程抛出异常"
                    }
                    if (!regions.isNullOrEmpty()) engine = "AI 识图"
                }

                // 第二层：本地 ML Kit OCR + 规则切分
                var fallbackHint: String? = null
                if (regions.isNullOrEmpty()) {
                    val ocrResult = ocr.recognize(upright)
                    regions = QuestionSegmenter.segment(ocrResult)
                    engine = "本地识别"
                    // 降级要让用户知道原因，否则只会觉得「识别很烂」
                    fallbackHint = when {
                        visionTried ->
                            "AI 识图失败：${visionError ?: "模型没返回可解析的题目"}。已改用本地识别，文字可在详情页手动改。"
                        // 有配置但读不到 Key：多半是输了 Key 却忘了点保存
                        hasProfile ->
                            "有 API 配置但没读到 Key，AI 识图用不了。去「设置 → 模型与 API」重新贴 Key 并点保存。"
                        else ->
                            "当前用本地识别。想更准可以在设置里配置支持图片的模型（如 deepseek-flash、豆包）。"
                    }
                }

                if (regions.isEmpty()) {
                    _state.update {
                        it.copy(stage = Stage.Idle, message = "没识别到题目，靠近一点、拍清楚再试")
                    }
                    return@launch
                }
                val selectable = regions.indices.filter { !regions[it].isSectionHeader }.toSet()
                _state.update {
                    it.copy(
                        stage = Stage.Selecting,
                        regions = regions,
                        selected = selectable,
                        engine = engine,
                        message = fallbackHint,
                        bitmap = upright,
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(stage = Stage.Idle, message = "识别失败：${t.message ?: "未知错误"}")
                }
            }
        }
    }

    /**
     * 自动摆正：竖图直接返回；横图用低分辨率试 0/90/270 三个方向，
     * 取「识别出的有效字符最多」的那个（试卷文字远多于噪声，这个判据很稳）。
     */
    private suspend fun autoOrient(bitmap: Bitmap): Bitmap {
        val landscape = bitmap.width > bitmap.height
        // 竖图也可能上下颠倒（拍反了），所以竖图比 0°/180°；横图比四个方向
        val candidates = if (landscape) listOf(0, 90, 180, 270) else listOf(0, 180)
        val small = bitmap.scaledToMaxSide(1200)
        var bestDeg = 0
        var bestScore = -1
        candidates.forEach { deg ->
            val rotated = if (deg == 0) small else small.rotated(deg.toFloat())
            val score = runCatching {
                ocr.recognize(rotated).lines.sumOf { it.text.trim().count { c -> c.isLetterOrDigit() } }
            }.getOrDefault(0)
            if (score > bestScore) {
                bestScore = score
                bestDeg = deg
            }
        }
        return if (bestDeg == 0) bitmap else bitmap.rotated(bestDeg.toFloat())
    }

    fun toggleRegion(index: Int) = _state.update { s ->
        val next = if (index in s.selected) s.selected - index else s.selected + index
        s.copy(selected = next)
    }

    fun selectAll() = _state.update { s ->
        s.copy(selected = s.regions.indices.filter { !s.regions[it].isSectionHeader }.toSet())
    }

    fun invertSelection() = _state.update { s ->
        val all = s.regions.indices.filter { !s.regions[it].isSectionHeader }.toSet()
        s.copy(selected = all - s.selected)
    }

    fun toggleTag(id: Long) = _state.update { s ->
        s.copy(selectedTagIds = if (id in s.selectedTagIds) s.selectedTagIds - id else s.selectedTagIds + id)
    }

    /** 现场新建标签并自动勾选：标签数量不设上限。 */
    fun createTag(name: String) {
        viewModelScope.launch {
            when (val r = tagRepo.create(name = name)) {
                is WbResult.Ok -> {
                    val latest = runCatching { tagRepo.observeAll().first() }.getOrDefault(emptyList())
                    _state.update {
                        it.copy(tags = latest, selectedTagIds = it.selectedTagIds + r.value)
                    }
                }
                is WbResult.Err -> _state.update { it.copy(message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun onError(message: String) = _state.update { it.copy(stage = Stage.Idle, message = message) }

    /** 放弃当前照片，回到取景。 */
    fun discard() = _state.update { s ->
        s.copy(stage = Stage.Idle, bitmap = null, regions = emptyList(), selected = emptySet())
    }

    fun save() {
        val s = _state.value
        val bitmap = s.bitmap ?: return
        val chosen = s.regions.filterIndexed { i, r -> i in s.selected && !r.isSectionHeader }
        if (chosen.isEmpty()) {
            _state.update { it.copy(message = "至少选一道题") }
            return
        }
        _state.update { it.copy(stage = Stage.Saving, progress = "0/${chosen.size}") }
        viewModelScope.launch {
            var saved = 0
            var failed = 0
            chosen.forEachIndexed { i, region ->
                _state.update { it.copy(progress = "${i + 1}/${chosen.size}") }
                when (val image = imageStore.saveCrop(bitmap, region.rect)) {
                    is WbResult.Ok -> {
                        val entity = QuestionEntity(
                            imagePath = image.value.imagePath,
                            thumbPath = image.value.thumbPath,
                            rawOcrText = region.text,
                            source = "SCAN",
                        )
                        when (questionRepo.add(entity, s.selectedTagIds.toList())) {
                            is WbResult.Ok -> saved++
                            is WbResult.Err -> failed++
                        }
                    }
                    is WbResult.Err -> failed++
                }
            }
            _state.update {
                it.copy(
                    stage = Stage.Done,
                    savedCount = saved,
                    progress = null,
                    message = if (failed == 0) null else "有 $failed 道题保存失败",
                )
            }
        }
    }

    fun reset() {
        _state.update { UiState(tags = it.tags) }
    }
}
