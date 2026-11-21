package com.chenzixin.wrongbook.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `补丁号更大就是新版本`() {
        assertTrue(VersionComparator.isNewer("0.3.1", "0.3.0"))
        assertFalse(VersionComparator.isNewer("0.3.0", "0.3.1"))
    }

    @Test
    fun `支持 v 前缀与段数不一致`() {
        assertTrue(VersionComparator.isNewer("v0.4", "0.3.9"))
        assertFalse(VersionComparator.isNewer("1.0", "1.0.0"))
        assertTrue(VersionComparator.isNewer("1.0.1", "1.0"))
    }

    @Test
    fun `数字段按数值比较而不是字符串`() {
        assertTrue(VersionComparator.isNewer("1.2.10", "1.2.9"))
        assertFalse(VersionComparator.isNewer("1.2.9", "1.2.10"))
    }

    @Test
    fun `预发布后缀被忽略`() {
        // 0.3.0-beta 与 0.3.0 视为同一版本，避免用户被反复提示更新
        assertFalse(VersionComparator.isNewer("0.3.0-beta", "0.3.0"))
        assertFalse(VersionComparator.isNewer("0.3.0", "0.3.0-rc1"))
    }

    @Test
    fun `非法输入不会崩`() {
        assertFalse(VersionComparator.isNewer("", "1.0"))
        assertFalse(VersionComparator.isNewer("abc", "1.0"))
        assertTrue(VersionComparator.isNewer("1.0", "abc"))
    }

    @Test
    fun `规范化用于展示`() {
        assertEquals("0.3.1", VersionComparator.normalize("v0.3.1-beta"))
        assertEquals("1.0.0", VersionComparator.normalize("1.0"))
    }
}
