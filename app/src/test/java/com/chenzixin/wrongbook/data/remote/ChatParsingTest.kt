package com.chenzixin.wrongbook.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatParsingTest {

    // ---------- SSE ----------

    @Test
    fun `解析普通数据帧`() {
        val event = SseParser.parse("""data: {"choices":[{"delta":{"content":"你好"}}]}""")
        assertTrue(event is SseParser.Event.Chunk)
    }

    @Test
    fun `解析结束帧`() {
        assertTrue(SseParser.parse("data: [DONE]") is SseParser.Event.Done)
    }

    @Test
    fun `忽略空行与注释帧`() {
        assertNull(SseParser.parse(""))
        assertNull(SseParser.parse("   "))
        assertNull(SseParser.parse("event: message"))
        assertNull(SseParser.parse("data:"))
    }

    @Test
    fun `容忍前导空格与CR`() {
        val event = SseParser.parse("  data: {\"choices\":[]}\r")
        assertTrue(event is SseParser.Event.Chunk)
    }

    // ---------- 响应字段 ----------

    @Test
    fun `流式 delta 取内容`() {
        val json = """{"choices":[{"delta":{"content":"解题思路"}}]}"""
        assertEquals("解题思路", ChatJson.contentOf(json))
    }

    @Test
    fun `非流式 message 取内容`() {
        val json = """{"choices":[{"message":{"role":"assistant","content":"答案"}}]}"""
        assertEquals("答案", ChatJson.contentOf(json))
    }

    @Test
    fun `非法 JSON 返回 null 不崩溃`() {
        assertNull(ChatJson.contentOf("<html>502 Bad Gateway</html>"))
        assertNull(ChatJson.contentOf(""))
    }

    @Test
    fun `错误体取 message`() {
        val json = """{"error":{"message":"Incorrect API key provided","type":"invalid_request_error"}}"""
        assertEquals("Incorrect API key provided", ChatJson.errorMessageOf(json))
        assertNull(ChatJson.errorMessageOf("{}"))
    }

    @Test
    fun `token 用量可解析`() {
        assertEquals(123, ChatJson.totalTokensOf("""{"usage":{"total_tokens":123}}"""))
        assertNull(ChatJson.totalTokensOf("""{}"""))
    }

    // ---------- 错误映射 ----------

    @Test
    fun `常见状态码映射成人话`() {
        assertTrue(LlmErrors.forCode(401).contains("Key"))
        assertTrue(LlmErrors.forCode(429).contains("限流"))
        assertTrue(LlmErrors.forCode(503).contains("服务端"))
        assertTrue(LlmErrors.forCode(418).contains("418"))
    }
}
