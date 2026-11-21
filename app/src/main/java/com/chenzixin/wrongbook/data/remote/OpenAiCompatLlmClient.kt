package com.chenzixin.wrongbook.data.remote

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class OpenAiCompatLlmClient @Inject constructor(
    private val client: OkHttpClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LlmClient {

    override fun stream(
        profile: ApiProfileEntity,
        apiKey: String,
        messages: List<ChatMsg>,
    ): Flow<ChatChunk> = flow {
        val request = buildRequest(profile, apiKey, messages, stream = true, jsonMode = false, maxTokens = null)
        val call = client.newCall(request)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw LlmException(
                        userMessage = ChatJson.errorMessageOf(response.body.string().orEmpty())
                            ?: LlmErrors.forCode(response.code),
                        httpCode = response.code,
                    )
                }
                val source = response.body.source()
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    when (val event = SseParser.parse(line)) {
                        null -> Unit
                        is SseParser.Event.Chunk -> {
                            val delta = ChatJson.contentOf(event.json)
                            val reasoning = ChatJson.reasoningOf(event.json)
                            if (!delta.isNullOrEmpty() || !reasoning.isNullOrEmpty()) {
                                emit(
                                    ChatChunk(
                                        delta = delta.orEmpty(),
                                        reasoningDelta = reasoning.orEmpty(),
                                    ),
                                )
                            }
                        }
                        SseParser.Event.Done -> break
                    }
                }
                emit(ChatChunk(delta = "", done = true))
            }
        } catch (e: LlmException) {
            throw e
        } catch (e: IOException) {
            throw LlmException(LlmErrors.NETWORK, cause = e)
        } finally {
            call.cancel()
        }
    }.flowOn(io)

    override suspend fun complete(
        profile: ApiProfileEntity,
        apiKey: String,
        messages: List<ChatMsg>,
        jsonMode: Boolean,
        maxTokens: Int?,
    ): WbResult<String> = withContext(io) {
        try {
            val request = buildRequest(profile, apiKey, messages, stream = false, jsonMode = jsonMode, maxTokens = maxTokens)
            client.newCall(request).execute().use { response ->
                val text = response.body.string()
                if (!response.isSuccessful) {
                    return@withContext WbResult.Err(
                        ChatJson.errorMessageOf(text) ?: LlmErrors.forCode(response.code),
                    )
                }
                val content = ChatJson.contentOf(text)
                    ?: return@withContext WbResult.Err(LlmErrors.PARSE)
                WbResult.Ok(content)
            }
        } catch (e: IOException) {
            WbResult.Err(LlmErrors.NETWORK, e)
        } catch (t: Throwable) {
            WbResult.Err(LlmErrors.PARSE, t)
        }
    }

    override suspend fun testConnection(
        profile: ApiProfileEntity,
        apiKey: String,
    ): WbResult<Long> {
        val started = System.currentTimeMillis()
        val result = complete(
            profile = profile,
            apiKey = apiKey,
            messages = listOf(ChatMsg.user("ping")),
            maxTokens = 8,
        )
        return when (result) {
            is WbResult.Ok -> WbResult.Ok(System.currentTimeMillis() - started)
            is WbResult.Err -> result
        }
    }

    private fun buildRequest(
        profile: ApiProfileEntity,
        apiKey: String,
        messages: List<ChatMsg>,
        stream: Boolean,
        jsonMode: Boolean,
        maxTokens: Int?,
    ): Request {
        val url = endpointOf(profile.baseUrl)
        val payload = buildJsonObject {
            put("model", profile.chatModel)
            put("stream", stream)
            put("temperature", profile.temperature.toDouble())
            put("max_tokens", maxTokens ?: profile.maxTokens.coerceAtLeast(MIN_MAX_TOKENS))
            if (jsonMode) {
                putJsonObject("response_format") { put("type", "json_object") }
            }
            put("messages", buildJsonArray {
                messages.forEach { msg -> add(messageJson(msg)) }
            })
        }
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()
    }

    private fun messageJson(msg: ChatMsg): JsonObject = buildJsonObject {
        put("role", msg.role)
        if (msg.imagesBase64.isEmpty()) {
            put("content", msg.text)
        } else {
            put("content", buildJsonArray {
                if (msg.text.isNotBlank()) {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", msg.text)
                    })
                }
                msg.imagesBase64.forEach { b64 ->
                    add(buildJsonObject {
                        put("type", "image_url")
                        putJsonObject("image_url") { put("url", "data:image/jpeg;base64,$b64") }
                    })
                }
            })
        }
    }

    private fun endpointOf(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        require(base.isNotEmpty()) { "baseUrl 不能为空" }
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        /** 老配置里存的是 2048，长回答必被截断；统一抬到 4096 起步。 */
        const val MIN_MAX_TOKENS = 4096
    }
}
