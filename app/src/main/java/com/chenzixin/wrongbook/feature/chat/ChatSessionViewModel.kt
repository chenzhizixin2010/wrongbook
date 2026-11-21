package com.chenzixin.wrongbook.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.ChatImageStore
import com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import com.chenzixin.wrongbook.data.remote.ChatMsg
import com.chenzixin.wrongbook.data.remote.LlmClient
import com.chenzixin.wrongbook.data.remote.LlmErrors
import com.chenzixin.wrongbook.data.remote.LlmException
import com.chenzixin.wrongbook.data.repository.ChatRepository
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import com.chenzixin.wrongbook.domain.chat.ChatContextBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChatSessionViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val imageStore: ChatImageStore,
    private val settings: SettingsRepository,
    private val llm: LlmClient,
    savedStateHandle: SavedStateHandle,
    private val questionRepo: com.chenzixin.wrongbook.data.repository.QuestionRepository,
    private val tagRepo: com.chenzixin.wrongbook.data.repository.TagRepository,) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    val session: StateFlow<ChatSessionEntity?> = chatRepo.observeSession(sessionId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val messages: StateFlow<List<ChatMessageEntity>> = chatRepo.observeMessages(sessionId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** 流式过程中的增量文本；null 表示当前没有在生成。 */
    private val _streaming = MutableStateFlow<String?>(null)
    val streaming: StateFlow<String?> = _streaming.asStateFlow()

    /** 推理模型的思考过程（DeepSeek V4 等）。正文开始输出后清空。 */
    private val _thinking = MutableStateFlow("")
    val thinking: StateFlow<String> = _thinking.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private var job: Job? = null

    val isGenerating: Boolean get() = job?.isActive == true

    /** 把相册选的图存进私有目录，返回可发送的相对路径。 */
    suspend fun stageImage(uri: android.net.Uri): String? =
        when (val r = imageStore.saveFromUri(uri)) {
            is WbResult.Ok -> r.value
            is WbResult.Err -> {
                _status.value = r.message
                null
            }
        }

    fun clearStatus() {
        _status.value = null
    }


    /** 对话里把某条回答存成错题。 */
    fun saveAsQuestion(stem: String, answer: String, analysis: String, tagIds: List<Long>) {
        if (stem.isBlank()) {
            _status.value = "题干是空的，先填一点内容"
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = com.chenzixin.wrongbook.data.local.entity.QuestionEntity(
                imagePath = "",
                thumbPath = "",
                rawOcrText = stem.trim(),
                refinedText = stem.trim(),
                answer = answer.trim().ifBlank { null },
                analysis = analysis.trim().ifBlank { null },
                subject = "",
                source = "CHAT",
                createdAt = now,
            )
            when (val r = questionRepo.add(entity, tagIds)) {
                is WbResult.Ok -> {
                    _status.value = "已加入错题本"
                    allTagsRefresh()
                }
                is WbResult.Err -> _status.value = r.message
            }
        }
    }

    private fun allTagsRefresh() {
        viewModelScope.launch {
            runCatching { tagRepo.observeAll().first() }.onSuccess { _allTags.value = it }
        }
    }

    private val _allTags = MutableStateFlow<List<com.chenzixin.wrongbook.data.local.entity.TagEntity>>(emptyList())
    val allTags: StateFlow<List<com.chenzixin.wrongbook.data.local.entity.TagEntity>> = _allTags.asStateFlow()

    init {
        allTagsRefresh()
    }

    fun stop() {
        job?.cancel()
        job = null
        _streaming.value = null
        _thinking.value = ""
    }

    fun send(text: String, imagePaths: List<String> = emptyList()) {
        val input = text.trim()
        if (input.isEmpty() && imagePaths.isEmpty()) return
        if (isGenerating) return
        job = viewModelScope.launch {
            val active = settings.resolveActive()
            if (active == null) {
                _status.value = LlmErrors.NO_KEY
                return@launch
            }

            chatRepo.appendMessage(sessionId, "user", input, imagePaths)

            val imageBase64 = imagePaths.mapNotNull { imageStore.readBase64(it) }

            val history = chatRepo.messagesOnce(sessionId)
                .dropLast(1) // 去掉刚落库的这一条，后面单独作为 incoming
                // 历史消息只带文字：每轮都重发图片既贵又容易超限
                .map { ChatMsg(it.role, it.content) }

            val context = if (ChatContextBuilder.needsSummary(history)) {
                val older = history.dropLast(ChatContextBuilder.MAX_HISTORY)
                val recent = history.takeLast(ChatContextBuilder.MAX_HISTORY).toMutableList()
                summarize(active, older)?.let { summary ->
                    recent.add(0, ChatMsg.system("以下是更早对话的摘要，供你参考：\n$summary"))
                }
                ChatContextBuilder.build(recent, ChatMsg.user(input, imageBase64))
            } else {
                ChatContextBuilder.build(history, ChatMsg.user(input, imageBase64))
            }

            _streaming.value = ""
            _thinking.value = ""
            val buffer = StringBuilder()
            val thoughts = StringBuilder()
            try {
                llm.stream(active.profile, active.apiKey, context).collect { chunk ->
                    if (chunk.reasoningDelta.isNotEmpty()) {
                        thoughts.append(chunk.reasoningDelta)
                        _thinking.value = thoughts.toString()
                    }
                    if (chunk.delta.isNotEmpty()) {
                        if (thoughts.isNotEmpty()) _thinking.value = ""
                        buffer.append(chunk.delta)
                        _streaming.value = buffer.toString()
                    }
                }
                if (buffer.isNotEmpty()) {
                    chatRepo.appendMessage(sessionId, "assistant", buffer.toString())
                }
            } catch (e: LlmException) {
                _status.value = e.userMessage
                // 已经吐出来的内容不要丢，落库后用户还能看到
                if (buffer.isNotEmpty()) {
                    chatRepo.appendMessage(sessionId, "assistant", buffer.toString())
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _status.value = "生成失败：${t.message ?: "未知错误"}"
            } finally {
                _streaming.value = null
                _thinking.value = ""
                job = null
            }

            // 首轮结束后用首句当标题
            if (chatRepo.messagesOnce(sessionId).size <= 2) {
                chatRepo.renameSession(sessionId, input.take(20))
            }
        }
    }

    private suspend fun summarize(
        active: SettingsRepository.ActiveProfile,
        older: List<ChatMsg>,
    ): String? {
        if (older.isEmpty()) return null
        val prompt = ChatMsgs.summaryPrompt(ChatContextBuilder.flattenForSummary(older))
        return when (val r = llm.complete(active.profile, active.apiKey, prompt, maxTokens = 400)) {
            is WbResult.Ok -> r.value
            is WbResult.Err -> null // 摘要失败就退化为直接截断
        }
    }
}

private object ChatMsgs {
    fun summaryPrompt(flattened: String): List<ChatMsg> = listOf(
        ChatMsg.system("你是对话摘要助手。用不超过 150 字总结下面的师生对话，保留题目、知识点和未解决的问题。"),
        ChatMsg.user(flattened),
    )
}
