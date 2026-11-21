package com.chenzixin.wrongbook.data.remote

/** 一条待发送的消息。图片以 base64（不含 data: 前缀）形式随消息发送。 */
data class ChatMsg(
    val role: String,
    val text: String,
    val imagesBase64: List<String> = emptyList(),
) {
    companion object {
        fun system(text: String) = ChatMsg("system", text)
        fun user(text: String, images: List<String> = emptyList()) = ChatMsg("user", text, images)
        fun assistant(text: String) = ChatMsg("assistant", text)
    }
}

/** 流式返回的一段增量。reasoningDelta 是「思考过程」增量（DeepSeek V4 等推理模型会先吐这个）。 */
data class ChatChunk(
    val delta: String,
    val done: Boolean = false,
    val reasoningDelta: String = "",
    /** 原生 finish_reason；"length" 表示被输出上限截断 */
    val finishReason: String = "",
)

/** 面向用户的错误：message 可直接展示。 */
class LlmException(
    val userMessage: String,
    val httpCode: Int = 0,
    cause: Throwable? = null,
) : Exception(userMessage, cause)

object LlmErrors {

    fun forCode(code: Int): String = when (code) {
        400 -> "请求格式有误（400），检查模型名与参数"
        401, 403 -> "API Key 无效或无权限（$code）"
        404 -> "接口地址不对（404），确认 baseUrl 以 /v1 结尾"
        413 -> "请求内容过大（413），题目图片太多或太长"
        429 -> "请求太频繁被限流（429），稍后再试"
        in 500..599 -> "服务端错误（$code），稍后重试"
        else -> "请求失败（$code）"
    }

    const val NETWORK = "网络不可用或超时，请检查网络后重试"
    const val PARSE = "模型返回的内容无法解析，已切换本地处理"
    const val NO_KEY = "还没有配置 API Key，请先到「设置 → API 配置」添加"
}
