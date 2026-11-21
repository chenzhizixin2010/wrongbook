package com.chenzixin.wrongbook.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 备份文件格式（纯 JVM，可单测）。
 *
 * 结构：
 * ```
 * wrongbook-backup.zip
 * ├── manifest.json
 * ├── questions.json
 * ├── tags.json
 * ├── chats.json
 * └── images/<name>.webp
 * ```
 * 注意：备份里**绝不包含 API Key**（Key 只在本机 Keystore 里）。
 */
@Serializable
data class BackupManifest(
    val app: String = "wrongbook",
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = 0L,
    val questionCount: Int = 0,
    val tagCount: Int = 0,
    val chatSessionCount: Int = 0,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class QuestionDto(
    val id: Long,
    val imagePath: String = "",
    val thumbPath: String = "",
    val rawOcrText: String = "",
    val refinedText: String? = null,
    val answer: String? = null,
    val analysis: String? = null,
    val subject: String? = null,
    val source: String = "SCAN",
    val masteryLevel: Int = 0,
    val reviewCount: Int = 0,
    val lastReviewAt: Long? = null,
    val nextReviewAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val tagIds: List<Long> = emptyList(),
)

@Serializable
data class TagDto(
    val id: Long,
    val name: String,
    val color: String = "#007AFF",
    val parentId: Long? = null,
    val isFolder: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
    val imagePaths: String = "",
    val createdAt: Long = 0L,
)

@Serializable
data class ChatSessionDto(
    val title: String,
    val pinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val messages: List<ChatMessageDto> = emptyList(),
)

@Serializable
data class BackupBundle(
    val manifest: BackupManifest,
    val questions: List<QuestionDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val chats: List<ChatSessionDto> = emptyList(),
)

object BackupCodec {

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeManifest(manifest: BackupManifest): String = json.encodeToString(manifest)

    fun encodeQuestions(list: List<QuestionDto>): String = json.encodeToString(list)

    fun encodeTags(list: List<TagDto>): String = json.encodeToString(list)

    fun encodeChats(list: List<ChatSessionDto>): String = json.encodeToString(list)

    fun decodeManifest(text: String): BackupManifest? =
        runCatching { json.decodeFromString<BackupManifest>(text) }.getOrNull()

    fun decodeQuestions(text: String): List<QuestionDto> =
        runCatching { json.decodeFromString<List<QuestionDto>>(text) }.getOrDefault(emptyList())

    fun decodeTags(text: String): List<TagDto> =
        runCatching { json.decodeFromString<List<TagDto>>(text) }.getOrDefault(emptyList())

    fun decodeChats(text: String): List<ChatSessionDto> =
        runCatching { json.decodeFromString<List<ChatSessionDto>>(text) }.getOrDefault(emptyList())

    /** 校验备份能不能导入：版本号必须 ≤ 当前版本。 */
    fun validate(manifest: BackupManifest?): String? = when {
        manifest == null -> "备份文件损坏：缺少 manifest.json"
        manifest.app != "wrongbook" -> "这不是 Errata 导出的备份文件"
        manifest.version > BackupManifest.CURRENT_VERSION ->
            "备份来自更新版本的 App（v${manifest.version}），请先升级再导入"
        else -> null
    }
}
