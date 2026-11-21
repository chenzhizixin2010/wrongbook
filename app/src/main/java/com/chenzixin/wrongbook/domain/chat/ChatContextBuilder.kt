package com.chenzixin.wrongbook.domain.chat

import com.chenzixin.wrongbook.data.remote.ChatMsg

/**
 * 上下文窗口管理（纯函数，可单测）。
 *
 * 策略：保留「系统提示 + 最近 N 条」。
 * 超过 [SUMMARY_THRESHOLD] 条时，由调用方先做一次摘要（额外请求），
 * 摘要失败就退化为直接截断 —— 宁可丢早期上下文，也不能让请求超限失败。
 */
object ChatContextBuilder {

    const val MAX_HISTORY = 20
    const val SUMMARY_THRESHOLD = 40

    const val SYSTEM_PROMPT = """
你是学生的错题辅导老师。要求：
1. 先给结论，再给推导过程；公式用 LaTeX（$...$ 行内，$$...$$ 独立成行）。
2. 步骤写清楚，每步说明理由；学生问「为什么」时优先解释思路，而不是直接给答案。
3. 不确定的地方明确说不确定，不要编造。
4. 回答用简体中文，语气简洁、不要客套。
"""

    fun needsSummary(history: List<ChatMsg>): Boolean = history.size > SUMMARY_THRESHOLD

    /**
     * @param history 不含最新一条的对话（按时间正序）
     * @param incoming 本次要发送的消息
     */
    fun build(
        history: List<ChatMsg>,
        incoming: ChatMsg,
        systemPrompt: String = SYSTEM_PROMPT,
        maxHistory: Int = MAX_HISTORY,
    ): List<ChatMsg> {
        val kept = if (history.size > maxHistory) history.takeLast(maxHistory) else history
        return buildList {
            add(ChatMsg.system(systemPrompt))
            addAll(kept)
            add(incoming)
        }
    }

    /** 需要摘要时，把早期消息压成一段纯文本交给模型。 */
    fun flattenForSummary(history: List<ChatMsg>): String =
        history.joinToString("\n") { msg ->
            val who = if (msg.role == "user") "学生" else "老师"
            "$who：${msg.text.take(400)}"
        }
}
