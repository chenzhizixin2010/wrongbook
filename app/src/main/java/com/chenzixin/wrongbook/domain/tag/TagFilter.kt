package com.chenzixin.wrongbook.domain.tag

/**
 * 标签筛选条件：多标签 AND / OR。
 *
 * 抽成纯数据 + 纯函数，有两个用途：
 * 1) Repository 把 [sortedIds] 与 [requiredCount] 交给 SQL（AND 用 HAVING COUNT 判定）；
 * 2) 导出/导入、AI 出题时在内存里过滤，逻辑与 SQL 保持同一套语义（有单测）。
 */
data class TagFilter(
    val tagIds: Set<Long> = emptySet(),
    val matchAll: Boolean = true,
) {
    val isEmpty: Boolean get() = tagIds.isEmpty()

    /** 去掉非法 id（<=0）并排序，保证 SQL / 内存两条路径拿到同样的参数。 */
    fun sortedIds(): List<Long> = tagIds.filter { it > 0L }.sorted()

    /** AND 语义下需要命中的标签数；OR 语义下恒为 1（命中任一即可）。 */
    fun requiredCount(): Int {
        val n = sortedIds().size
        if (n == 0) return 0
        return if (matchAll) n else 1
    }

    /** 内存过滤：与 SQL 语义一致，用于导出、AI 选题等非查询场景。 */
    fun matches(questionTagIds: Collection<Long>): Boolean {
        val wanted = sortedIds()
        if (wanted.isEmpty()) return true
        val owned = questionTagIds.toSet()
        return if (matchAll) owned.containsAll(wanted) else wanted.any { it in owned }
    }

    companion object {
        val NONE = TagFilter()
    }
}
