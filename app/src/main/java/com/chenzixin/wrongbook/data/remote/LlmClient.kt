package com.chenzixin.wrongbook.data.remote

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * 大模型客户端抽象。
 *
 * 只保留一层实现：OpenAI 兼容协议（`POST {baseUrl}/chat/completions`）。
 * DeepSeek、通义、智谱、Ollama、自建网关都兼容这套协议，因此不再单独引入厂商 SDK，
 * 少一层依赖 = 少一处版本冲突（见 DECISIONS.md D17）。
 */
interface LlmClient {

    /** 流式对话；异常用 [LlmException] 抛出，message 可直接展示给用户。 */
    fun stream(
        profile: ApiProfileEntity,
        apiKey: String,
        messages: List<ChatMsg>,
    ): Flow<ChatChunk>

    /** 非流式补全。 */
    suspend fun complete(
        profile: ApiProfileEntity,
        apiKey: String,
        messages: List<ChatMsg>,
        jsonMode: Boolean = false,
        maxTokens: Int? = null,
    ): WbResult<String>

    /** 测试连接：返回往返耗时（毫秒）。 */
    suspend fun testConnection(profile: ApiProfileEntity, apiKey: String): WbResult<Long>
}
