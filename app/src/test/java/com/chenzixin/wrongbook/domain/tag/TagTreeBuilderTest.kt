package com.chenzixin.wrongbook.domain.tag

import com.chenzixin.wrongbook.data.local.entity.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagTreeBuilderTest {

    private fun tag(id: Long, name: String, parent: Long? = null, order: Int = 0) =
        TagEntity(id = id, name = name, parentId = parent, sortOrder = order)

    @Test
    fun `空列表返回空树`() {
        assertTrue(TagTreeBuilder.build(emptyList()).isEmpty())
    }

    @Test
    fun `两级树按 parentId 组装并按 sortOrder 排序`() {
        val tags = listOf(
            tag(1, "数学", order = 1),
            tag(2, "语文", order = 0),
            tag(3, "函数", parent = 1, order = 1),
            tag(4, "数列", parent = 1, order = 0),
        )

        val tree = TagTreeBuilder.build(tags)

        assertEquals(listOf("语文", "数学"), tree.map { it.tag.name })
        val math = tree.first { it.tag.id == 1L }
        assertEquals(listOf("数列", "函数"), math.children.map { it.tag.name })
    }

    @Test
    fun `父节点不存在的标签降级为根`() {
        val tree = TagTreeBuilder.build(listOf(tag(1, "孤儿", parent = 999)))

        assertEquals(1, tree.size)
        assertEquals("孤儿", tree.single().tag.name)
    }

    @Test
    fun `成环的标签不会死循环 且都被降级为根`() {
        val tags = listOf(
            tag(1, "A", parent = 2),
            tag(2, "B", parent = 1),
        )

        val tree = TagTreeBuilder.build(tags)

        assertEquals(2, tree.size)
        assertTrue(tree.all { it.children.isEmpty() })
    }

    @Test
    fun `自己的 parent 指向自己时当作根`() {
        val tree = TagTreeBuilder.build(listOf(tag(1, "自环", parent = 1)))

        assertEquals(1, tree.size)
        assertEquals("自环", tree.single().tag.name)
    }
}
