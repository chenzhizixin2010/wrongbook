package com.chenzixin.wrongbook.data.repository

import androidx.room.withTransaction
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.core.runCatchingWb
import com.chenzixin.wrongbook.data.local.WrongBookDatabase
import com.chenzixin.wrongbook.data.local.dao.QuestionTagCrossRefDao
import com.chenzixin.wrongbook.data.local.dao.TagDao
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.tag.TagTreeBuilder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val db: WrongBookDatabase,
    private val tagDao: TagDao,
    private val crossRefDao: QuestionTagCrossRefDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TagRepository {

    override fun observeAll(): Flow<List<TagEntity>> = tagDao.observeAll()

    override fun observeTree(): Flow<List<TagTreeBuilder.TagNode>> =
        tagDao.observeAll().map { TagTreeBuilder.build(it) }

    override suspend fun create(
        name: String,
        color: String,
        isFolder: Boolean,
        parentId: Long?,
    ): WbResult<Long> = withContext(io) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext WbResult.Err("标签名不能为空")
        }
        if (tagDao.findByName(trimmed) != null) {
            return@withContext WbResult.Err("标签「$trimmed」已存在")
        }
        runCatchingWb("标签创建失败") {
            tagDao.insert(
                TagEntity(
                    name = trimmed,
                    color = color,
                    isFolder = isFolder,
                    parentId = parentId,
                ),
            )
        }
    }

    override suspend fun rename(id: Long, newName: String): WbResult<Unit> = withContext(io) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return@withContext WbResult.Err("标签名不能为空")
        }
        val existing = tagDao.findByName(trimmed)
        if (existing != null && existing.id != id) {
            return@withContext WbResult.Err("标签「$trimmed」已存在")
        }
        val current = tagDao.findById(id)
            ?: return@withContext WbResult.Err("标签不存在，可能已被删除")
        runCatchingWb("重命名失败") { tagDao.update(current.copy(name = trimmed)) }
    }

    override suspend fun update(tag: TagEntity): WbResult<Unit> = withContext(io) {
        val trimmed = tag.name.trim()
        if (trimmed.isEmpty()) return@withContext WbResult.Err("标签名不能为空")
        val existing = tagDao.findByName(trimmed)
        if (existing != null && existing.id != tag.id) {
            return@withContext WbResult.Err("标签「$trimmed」已存在")
        }
        runCatchingWb("保存失败") { tagDao.update(tag.copy(name = trimmed)) }
    }

    override suspend fun deleteTag(id: Long): WbResult<Unit> = withContext(io) {
        runCatchingWb("删除标签失败") {
            db.withTransaction {
                crossRefDao.deleteByTagId(id)
                tagDao.deleteById(id)
            }
        }
    }

    override suspend fun countQuestions(tagId: Long): Int = withContext(io) {
        tagDao.countQuestions(tagId)
    }

    override suspend fun tagCounts(limit: Int) = withContext(io) { tagDao.tagCounts(limit) }
}
