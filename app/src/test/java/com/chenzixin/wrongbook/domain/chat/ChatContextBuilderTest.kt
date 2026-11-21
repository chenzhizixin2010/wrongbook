package com.chenzixin.wrongbook.domain.chat

import com.chenzixin.wrongbook.data.remote.ChatMsg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContextBuilderTest {

    private fun history(n: Int): List<ChatMsg> =
        (1..n).map { if (it % 2 == 0) ChatMsg.assistant("a$it") else ChatMsg.user("u$it") }

    @Test
    fun `系统提示永远排第一`() {
        val context = ChatContextBuilder.build(history(3), ChatMsg.user("新问题"))
        assertEquals("system", context.first().role)
        assertEquals("新问题", context.last().text)
    }

    @Test
    fun `超出窗口时只保留最近 N 条`() {
        val context = ChatContextBuilder.build(
            history = history(50),
            incoming = ChatMsg.user("最后一条"),
            maxHistory = 20,
        )
        // system + 20 + incoming
        assertEquals(22, context.size)
        assertEquals("u31", context[1].text)
        assertEquals("最后一条", context.last().text)
    }

    @Test
    fun `历史不足时全部保留`() {
        val context = ChatContextBuilder.build(history(5), ChatMsg.user("x"))
        assertEquals(7, context.size)
    }

    @Test
    fun `超过阈值才需要摘要`() {
        assertTrue(!ChatContextBuilder.needsSummary(history(ChatContextBuilder.SUMMARY_THRESHOLD)))
        assertTrue(ChatContextBuilder.needsSummary(history(ChatContextBuilder.SUMMARY_THRESHOLD + 1)))
    }

    @Test
    fun `摘要输入带角色前缀`() {
        val flat = ChatContextBuilder.flattenForSummary(listOf(ChatMsg.user("做题"), ChatMsg.assistant("好的")))
        assertTrue(flat.contains("学生：做题"))
        assertTrue(flat.contains("老师：好的"))
    }
}
