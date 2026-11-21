package com.chenzixin.wrongbook.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val sampleQuestions = listOf(
        QuestionDto(
            id = 1,
            imagePath = "questions/a.webp",
            thumbPath = "questions/a_thumb.webp",
            rawOcrText = "求极限",
            refinedText = "求极限 lim(x→0) sinx/x",
            answer = "1",
            subject = "数学",
            tagIds = listOf(2, 3),
            createdAt = 1_700_000_000_000,
        ),
    )

    @Test
    fun `题目 JSON 往返一致`() {
        val text = BackupCodec.encodeQuestions(sampleQuestions)
        val decoded = BackupCodec.decodeQuestions(text)
        assertEquals(1, decoded.size)
        assertEquals("求极限 lim(x→0) sinx/x", decoded[0].refinedText)
        assertEquals(listOf(2L, 3L), decoded[0].tagIds)
    }

    @Test
    fun `标签与对话往返一致`() {
        val tags = listOf(TagDto(id = 1, name = "函数", isFolder = true))
        val chats = listOf(
            ChatSessionDto(
                title = "AI 讲解",
                messages = listOf(ChatMessageDto(role = "user", content = "这道题怎么做")),
            ),
        )
        assertEquals("函数", BackupCodec.decodeTags(BackupCodec.encodeTags(tags)).single().name)
        assertEquals(
            "这道题怎么做",
            BackupCodec.decodeChats(BackupCodec.encodeChats(chats)).single().messages.single().content,
        )
    }

    @Test
    fun `损坏的 JSON 不抛异常`() {
        assertTrue(BackupCodec.decodeQuestions("{not json").isEmpty())
        assertTrue(BackupCodec.decodeTags("[]").isEmpty())
        assertNull(BackupCodec.decodeManifest("乱码"))
    }

    @Test
    fun `校验备份版本与来源`() {
        assertNotNull(BackupCodec.validate(null))
        assertNotNull(BackupCodec.validate(BackupManifest(app = "other")))
        assertNull(BackupCodec.validate(BackupManifest(version = 1)))
        assertNotNull(BackupCodec.validate(BackupManifest(version = 99)))
    }

    @Test
    fun `manifest 往返一致`() {
        val m = BackupManifest(exportedAt = 123L, questionCount = 5, tagCount = 2, chatSessionCount = 1)
        val decoded = BackupCodec.decodeManifest(BackupCodec.encodeManifest(m))
        assertEquals(5, decoded?.questionCount)
        assertEquals("wrongbook", decoded?.app)
    }
}
