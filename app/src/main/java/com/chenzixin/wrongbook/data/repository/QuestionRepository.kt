package com.chenzixin.wrongbook.data.repository

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.domain.tag.TagFilter
import kotlinx.coroutines.flow.Flow

/**
 * 题目仓储。UI / domain 只依赖这个接口，不直接碰 Room。
 *
 * 约定：查询返回 Flow（随表变化自动刷新）；写入返回 [WbResult]，失败不抛异常。
 */
interface QuestionRepository {

    fun observeAll(): Flow<List<QuestionWithTags>>

    fun observeById(id: Long): Flow<QuestionWithTags?>

    fun search(keyword: String): Flow<List<QuestionEntity>>

    fun observeByFilter(filter: TagFilter): Flow<List<QuestionEntity>>

    /** 入库一道题并绑定标签；返回新题 id。 */
    suspend fun add(question: QuestionEntity, tagIds: List<Long> = emptyList()): WbResult<Long>

    suspend fun update(question: QuestionEntity): WbResult<Unit>

    suspend fun delete(id: Long): WbResult<Unit>

    /** 覆盖式设置标签集合（先删后插，同一事务）。 */
    suspend fun setTags(questionId: Long, tagIds: List<Long>): WbResult<Unit>

    suspend fun due(now: Long): List<QuestionEntity>

    /** 复习结果落库：更新掌握度与下次复习时间。 */
    suspend fun applyReview(question: QuestionEntity, correct: Boolean, now: Long): WbResult<Unit>

    suspend fun count(): Int

    /** 掌握度分布（0~5）。 */
    suspend fun masteryCounts(): List<com.chenzixin.wrongbook.data.local.model.MasteryCount>
}
