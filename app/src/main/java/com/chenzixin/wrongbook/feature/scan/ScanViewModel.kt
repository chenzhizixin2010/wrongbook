package com.chenzixin.wrongbook.feature.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                val active = settings.resolveActive()
                var regions: List<QuestionRegion>? = null
                var engine = "本地识别"

                // 第一层：配置了视觉模型就先用它（失败/为空自动降级）
                if (active != null && active.profile.visionModel.isNotBlank()) {
                    regions = vision.segment(active.profile, active.apiKey, bitmap)
                    if (!regions.isNullOrEmpty()) engine = "AI 识图"
                }

                // 第二层：本地 ML Kit OCR + 规则切分
                if (regions.isNullOrEmpty()) {
                    val ocrResult = ocr.recognize(bitmap)
                    regions = QuestionSegmenter.segment(ocrResult)
                    engine = "本地识别"
                }

                if (regions.isEmpty()) {
                    _state.update {
                        it.copy(stage = Stage.Idle, message = "没识别到题目，靠近一点、拍清楚再试")
                    }
                    return@launch
                }
                val selectable = regions.indices.filter { !regions[it].isSectionHeader }.toSet()
                _state.update {
                    it.copy(stage = Stage.Selecting, regions = regions, selected = selectable, engine = engine)
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(stage = Stage.Idle, message = "识别失败：${t.message ?: "未知错误"}")
                }
            }
        }
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
