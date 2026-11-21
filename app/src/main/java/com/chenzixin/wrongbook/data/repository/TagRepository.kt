package com.chenzixin.wrongbook.data.repository

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.domain.tag.TagTreeBuilder
import kotlinx.coroutines.flow.Flow

/**
 * 标签 / 收藏夹仓储。
 *
 * 业务约束（P5 验收项）：
 * - 标签名唯一，重复时返回明确的 Err 文案；
 * - 删除标签只解除与题目的关联，绝不删除题目。
 */
interface TagRepository {

    fun observeAll(): Flow<List<TagEntity>>

    fun observeTree(): Flow<List<TagTreeBuilder.TagNode>>

    suspend fun create(
        name: String,
        color: String = "#007AFF",
        isFolder: Boolean = false,
        parentId: Long? = null,
    ): WbResult<Long>

    suspend fun rename(id: Long, newName: String): WbResult<Unit>

    /** 通用更新：改色、改排序、改是否收藏夹。 */
    suspend fun update(tag: TagEntity): WbResult<Unit>

    /** 只解除分类关系，题目保留。 */
    suspend fun deleteTag(id: Long): WbResult<Unit>

    /** 删除前给用户看的提示：会影响多少道题。 */
    suspend fun countQuestions(tagId: Long): Int

    /** 标签分布（题目数倒序，最多 limit 条）。 */
    suspend fun tagCounts(limit: Int = 8): List<com.chenzixin.wrongbook.data.local.model.TagCount>
}
