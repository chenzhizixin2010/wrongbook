package com.chenzixin.wrongbook.data.repository

import androidx.room.withTransaction
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.core.runCatchingWb
import com.chenzixin.wrongbook.data.local.WrongBookDatabase
import com.chenzixin.wrongbook.data.local.dao.QuestionDao
import com.chenzixin.wrongbook.data.local.dao.QuestionTagCrossRefDao
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.entity.QuestionTagCrossRef
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.review.ReviewScheduler
import com.chenzixin.wrongbook.domain.tag.TagFilter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class QuestionRepositoryImpl @Inject constructor(
    private val db: WrongBookDatabase,
    private val questionDao: QuestionDao,
    private val crossRefDao: QuestionTagCrossRefDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : QuestionRepository {

    override fun observeAll(): Flow<List<QuestionWithTags>> = questionDao.observeAllWithTags()

    override fun observeById(id: Long): Flow<QuestionWithTags?> = questionDao.observeByIdWithTags(id)

    override fun search(keyword: String): Flow<List<QuestionEntity>> =
        questionDao.search(keyword.trim())

    override fun observeByFilter(filter: TagFilter): Flow<List<QuestionEntity>> =
        questionDao.observeByTags(
            tagIds = filter.sortedIds(),
            tagCount = filter.requiredCount(),
        )

    override suspend fun add(question: QuestionEntity, tagIds: List<Long>): WbResult<Long> =
        withContext(io) {
            runCatchingWb("题目保存失败，请重试") {
                db.withTransaction {
                    val id = questionDao.insert(question)
                    if (tagIds.isNotEmpty()) {
                        crossRefDao.insertAll(tagIds.distinct().map { QuestionTagCrossRef(id, it) })
                    }
                    id
                }
            }
        }

    override suspend fun update(question: QuestionEntity): WbResult<Unit> = withContext(io) {
        runCatchingWb("题目更新失败，请重试") {
            questionDao.update(question.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun delete(id: Long): WbResult<Unit> = withContext(io) {
        runCatchingWb("删除失败，请重试") {
            // 交叉表由外键 CASCADE 清理；图片文件由调用方（UseCase）负责删除
            questionDao.deleteById(id)
        }
    }

    override suspend fun setTags(questionId: Long, tagIds: List<Long>): WbResult<Unit> =
        withContext(io) {
            runCatchingWb("标签保存失败，请重试") {
                db.withTransaction {
                    crossRefDao.deleteByQuestionId(questionId)
                    if (tagIds.isNotEmpty()) {
                        crossRefDao.insertAll(tagIds.distinct().map { QuestionTagCrossRef(questionId, it) })
                    }
                }
            }
        }

    override suspend fun due(now: Long): List<QuestionEntity> = withContext(io) {
        questionDao.getDue(now)
    }

    override suspend fun applyReview(
        question: QuestionEntity,
        correct: Boolean,
        now: Long,
    ): WbResult<Unit> = withContext(io) {
        runCatchingWb("复习结果保存失败") {
            val plan = ReviewScheduler.schedule(question.masteryLevel, correct, now)
            questionDao.update(
                question.copy(
                    masteryLevel = plan.masteryLevel,
                    reviewCount = question.reviewCount + 1,
                    lastReviewAt = now,
                    nextReviewAt = plan.nextReviewAt,
                    updatedAt = now,
                ),
            )
        }
    }

    override suspend fun count(): Int = withContext(io) { questionDao.count() }

    override suspend fun masteryCounts() =
        withContext(io) { questionDao.masteryCounts() }
}
