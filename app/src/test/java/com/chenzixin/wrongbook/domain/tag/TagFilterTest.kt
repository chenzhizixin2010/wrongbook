package com.chenzixin.wrongbook.domain.tag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagFilterTest {

    @Test
    fun `空筛选命中所有题`() {
        val filter = TagFilter.NONE

        assertTrue(filter.isEmpty)
        assertTrue(filter.matches(emptyList()))
        assertTrue(filter.matches(listOf(1L, 2L)))
        assertEquals(0, filter.requiredCount())
    }

    @Test
    fun `AND 语义要求全部命中`() {
        val filter = TagFilter(tagIds = setOf(1L, 2L), matchAll = true)

        assertTrue(filter.matches(listOf(1L, 2L)))
        assertTrue(filter.matches(listOf(3L, 1L, 2L)))
        assertFalse(filter.matches(listOf(1L)))
        assertEquals(2, filter.requiredCount())
    }

    @Test
    fun `OR 语义命中任一即可`() {
        val filter = TagFilter(tagIds = setOf(1L, 2L), matchAll = false)

        assertTrue(filter.matches(listOf(1L)))
        assertTrue(filter.matches(listOf(2L, 9L)))
        assertFalse(filter.matches(listOf(9L)))
        assertEquals(1, filter.requiredCount())
    }

    @Test
    fun `非法 id 被丢弃且去重排序`() {
        val filter = TagFilter(tagIds = setOf(3L, -1L, 0L, 3L, 1L))

        assertEquals(listOf(1L, 3L), filter.sortedIds())
        assertEquals(2, filter.requiredCount())
    }
}
