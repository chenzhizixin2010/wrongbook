package com.chenzixin.wrongbook.domain.tag

import com.chenzixin.wrongbook.data.local.entity.TagEntity

/**
 * 把扁平的标签列表组装成两级树，供筛选栏与标签管理页使用。
 *
 * 脏数据兜底（这几种情况都必须有测试）：
 * - parentId 指向不存在的标签 → 当作根节点；
 * - 出现环（A.parent=B, B.parent=A）→ 截断环，涉及节点降级为根，绝不死循环；
 * - 超过两级 → 第三级起不再展开（UI 只做两级，避免筛选栏无限缩进）。
 */
object TagTreeBuilder {

    data class TagNode(
        val tag: TagEntity,
        val children: List<TagNode>,
    )

    private fun sorter(a: TagEntity, b: TagEntity): Int =
        compareValuesBy(a, b, { it.sortOrder }, { it.name })

    fun build(tags: List<TagEntity>): List<TagNode> {
        if (tags.isEmpty()) return emptyList()
        val byId = tags.associateBy { it.id }

        fun parentOf(tag: TagEntity): TagEntity? =
            tag.parentId?.let { byId[it] }?.takeIf { it.id != tag.id }

        /** 沿 parent 链向上走；走回自己说明成环，返回 null（降级为根）。 */
        fun safeParent(tag: TagEntity): TagEntity? {
            val first = parentOf(tag) ?: return null
            var cursor: TagEntity? = first
            var guard = 0
            while (cursor != null && guard++ <= tags.size) {
                if (cursor.id == tag.id) return null
                cursor = parentOf(cursor)
            }
            return if (guard > tags.size) null else first
        }

        val parentById = tags.associateWith { safeParent(it) }
        val roots = tags.filter { parentById[it] == null }.sortedWith(::sorter)
        val childrenByParent = tags
            .mapNotNull { child -> parentById[child]?.let { parent -> parent.id to child } }
            .groupBy({ it.first }, { it.second })

        fun nodes(list: List<TagEntity>, depth: Int): List<TagNode> =
            list.sortedWith(::sorter).map { tag ->
                val kids = if (depth >= 1) emptyList() else childrenByParent[tag.id].orEmpty()
                TagNode(tag, nodes(kids, depth + 1))
            }

        return nodes(roots, 0)
    }
}
