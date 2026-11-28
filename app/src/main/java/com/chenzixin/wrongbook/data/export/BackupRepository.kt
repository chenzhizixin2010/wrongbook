package com.chenzixin.wrongbook.data.export

import android.content.Context
import android.net.Uri
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.dao.ChatDao
import com.chenzixin.wrongbook.data.local.dao.QuestionDao
import com.chenzixin.wrongbook.data.local.dao.QuestionTagCrossRefDao
import com.chenzixin.wrongbook.data.local.dao.TagDao
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.entity.QuestionImageEntity
import com.chenzixin.wrongbook.data.local.entity.QuestionTagCrossRef
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 备份导入导出（SAF Uri，不申请存储权限）。
 *
 * 冲突策略：按 (rawOcrText, createdAt) 判重，重复的题跳过；
 * 同一个包导入两次是幂等的。
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val questionDao: QuestionDao,
    private val tagDao: TagDao,
    private val crossRefDao: QuestionTagCrossRefDao,
    private val questionImageDao: com.chenzixin.wrongbook.data.local.dao.QuestionImageDao,
    private val imageStore: com.chenzixin.wrongbook.data.local.QuestionImageStore,
    private val chatDao: ChatDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    data class ResultInfo(val importedQuestions: Int, val skipped: Int)

    suspend fun export(uri: Uri): WbResult<Int> = withContext(io) {
        try {
            val questions = questionDao.observeAllWithTags().first()
            val tags = tagDao.observeAll().first()
            val sessions = chatDao.observeSessions().first()

            val questionDtos = questions.map { item ->
                val q = item.question
                QuestionDto(
                    id = q.id,
                    imagePath = q.imagePath,
                    thumbPath = q.thumbPath,
                    rawOcrText = q.rawOcrText,
                    refinedText = q.refinedText,
                    answer = q.answer,
                    analysis = q.analysis,
                    subject = q.subject,
                    source = q.source,
                    masteryLevel = q.masteryLevel,
                    reviewCount = q.reviewCount,
                    lastReviewAt = q.lastReviewAt,
                    nextReviewAt = q.nextReviewAt,
                    createdAt = q.createdAt,
                    updatedAt = q.updatedAt,
                    tagIds = item.tags.map { it.id },
                    figures = questionImageDao.listByQuestion(q.id).map { it.path },
                )
            }
            val chatDtos = sessions.map { session ->
                ChatSessionDto(
                    title = session.title,
                    pinned = session.pinned,
                    createdAt = session.createdAt,
                    updatedAt = session.updatedAt,
                    messages = chatDao.messagesOnce(session.id).map { m ->
                        ChatMessageDto(m.role, m.content, m.imagePaths, m.createdAt)
                    },
                )
            }
            val manifest = BackupManifest(
                exportedAt = System.currentTimeMillis(),
                questionCount = questionDtos.size,
                tagCount = tags.size,
                chatSessionCount = chatDtos.size,
            )

            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output.buffered()).use { zip ->
                    fun writeText(name: String, text: String) {
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(text.toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                    writeText("manifest.json", BackupCodec.encodeManifest(manifest))
                    writeText("questions.json", BackupCodec.encodeQuestions(questionDtos))
                    writeText("tags.json", BackupCodec.encodeTags(tags.map { it.toDto() }))
                    writeText("chats.json", BackupCodec.encodeChats(chatDtos))

                    // 插图打包到 figures/，方便导入时按文件名回填
                    questions.forEach { item ->
                        questionImageDao.listByQuestion(item.question.id).forEach { figure ->
                            val file = File(context.filesDir, figure.path)
                            if (!file.exists()) return@forEach
                            zip.putNextEntry(ZipEntry("figures/${file.name}"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }

                    questions.forEach { item ->
                        listOf(item.question.imagePath, item.question.thumbPath)
                            .filter { it.isNotBlank() }
                            .forEach { relative ->
                                val file = File(context.filesDir, relative)
                                if (!file.exists()) return@forEach
                                zip.putNextEntry(ZipEntry("images/${file.name}"))
                                file.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                    }
                }
            } ?: return@withContext WbResult.Err("无法写入这个位置，换个目录试试")
            WbResult.Ok(questionDtos.size)
        } catch (t: Throwable) {
            WbResult.Err("导出失败：${t.message ?: "未知错误"}", t)
        }
    }

    suspend fun import(uri: Uri): WbResult<ResultInfo> = withContext(io) {
        try {
            val texts = mutableMapOf<String, String>()
            val images = mutableMapOf<String, ByteArray>()
            val figures = mutableMapOf<String, ByteArray>()

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val bytes = zip.readBytes()
                        val name = entry.name
                        if (name.startsWith("images/")) {
                            images[name.removePrefix("images/")] = bytes
                        } else if (name.startsWith("figures/")) {
                            figures[name.removePrefix("figures/")] = bytes
                        } else if (name.endsWith(".json")) {
                            texts[name] = String(bytes, Charsets.UTF_8)
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext WbResult.Err("无法读取这个文件")

            val manifest = texts["manifest.json"]?.let(BackupCodec::decodeManifest)
            BackupCodec.validate(manifest)?.let { return@withContext WbResult.Err(it) }

            val tagDtos = texts["tags.json"]?.let(BackupCodec::decodeTags).orEmpty()
            val questionDtos = texts["questions.json"]?.let(BackupCodec::decodeQuestions).orEmpty()
            val chatDtos = texts["chats.json"]?.let(BackupCodec::decodeChats).orEmpty()

            // 1) 标签：按名字复用已有标签
            val tagIdMap = mutableMapOf<Long, Long>()
            tagDtos.forEach { dto ->
                val existing = tagDao.findByName(dto.name)
                val newId = existing?.id ?: tagDao.insert(
                    TagEntity(
                        name = dto.name,
                        color = dto.color,
                        isFolder = dto.isFolder,
                        sortOrder = dto.sortOrder,
                    ),
                )
                tagIdMap[dto.id] = newId
            }

            // 2) 题目：判重 + 图片落地 + 关联
            var imported = 0
            var skipped = 0
            questionDtos.forEach { dto ->
                if (questionDao.countByContent(dto.rawOcrText, dto.createdAt) > 0) {
                    skipped++
                    return@forEach
                }
                val newImage = copyImage(dto.imagePath, images)
                val newThumb = copyImage(dto.thumbPath, images)
                val newId = questionDao.insert(
                    QuestionEntity(
                        imagePath = newImage,
                        thumbPath = newThumb,
                        rawOcrText = dto.rawOcrText,
                        refinedText = dto.refinedText,
                        answer = dto.answer,
                        analysis = dto.analysis,
                        subject = dto.subject,
                        source = dto.source,
                        masteryLevel = dto.masteryLevel,
                        reviewCount = dto.reviewCount,
                        lastReviewAt = dto.lastReviewAt,
                        nextReviewAt = dto.nextReviewAt,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                    ),
                )
                val refs = dto.tagIds.mapNotNull { tagIdMap[it] }
                    .map { QuestionTagCrossRef(newId, it) }
                if (refs.isNotEmpty()) crossRefDao.insertAll(refs)

                // 插图：解码后走 store 重新压缩并生成缩略图
                dto.figures.forEach { relative ->
                    val bytes = figures[relative.substringAfterLast('/')] ?: return@forEach
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: return@forEach
                    when (val saved = imageStore.saveFigure(bitmap)) {
                        is WbResult.Ok -> questionImageDao.insert(
                            QuestionImageEntity(
                                questionId = newId,
                                path = saved.value.imagePath,
                                thumbPath = saved.value.thumbPath,
                            ),
                        )
                        is WbResult.Err -> Unit
                    }
                    bitmap.recycle()
                }
                imported++
            }

            // 3) 对话
            chatDtos.forEach { sessionDto ->
                val sessionId = chatDao.insertSession(
                    ChatSessionEntity(
                        title = sessionDto.title,
                        pinned = sessionDto.pinned,
                        createdAt = sessionDto.createdAt,
                        updatedAt = sessionDto.updatedAt,
                    ),
                )
                sessionDto.messages.forEach { m ->
                    chatDao.insertMessage(
                        com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity(
                            sessionId = sessionId,
                            role = m.role,
                            content = m.content,
                            imagePaths = m.imagePaths,
                            createdAt = m.createdAt,
                        ),
                    )
                }
            }

            WbResult.Ok(ResultInfo(importedQuestions = imported, skipped = skipped))
        } catch (t: Throwable) {
            WbResult.Err("导入失败：${t.message ?: "未知错误"}", t)
        }
    }

    /** 把 zip 里的图片写成新的文件名，避免和已有文件冲突。 */
    private fun copyImage(relativePath: String, images: Map<String, ByteArray>): String {
        if (relativePath.isBlank()) return ""
        val fileName = relativePath.substringAfterLast('/')
        val bytes = images[fileName] ?: return ""
        val dir = File(context.filesDir, "questions").apply { if (!exists()) mkdirs() }
        val newName = "${UUID.randomUUID()}.webp"
        File(dir, newName).writeBytes(bytes)
        return "questions/$newName"
    }

    private fun TagEntity.toDto() = TagDto(
        id = id,
        name = name,
        color = color,
        parentId = parentId,
        isFolder = isFolder,
        sortOrder = sortOrder,
    )
}
