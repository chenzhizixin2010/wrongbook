package com.chenzixin.wrongbook.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // ---------------------------------------------------------------- Alpha 通道

    @Test
    fun `Alpha 版本比上一大版本新`() {
        assertTrue(VersionComparator.isNewer("0.13.A01", "0.12.0"))
        assertTrue(VersionComparator.isNewer("v0.13.A01", "0.12.0"))
        assertTrue(VersionComparator.isNewer("0.14.A01", "0.13.0"))
    }

    @Test
    fun `Alpha 之间比编号`() {
        assertTrue(VersionComparator.isNewer("0.13.A02", "0.13.A01"))
        assertTrue(VersionComparator.isNewer("0.13.A10", "0.13.A09"))
        assertFalse(VersionComparator.isNewer("0.13.A01", "0.13.A02"))
        assertFalse(VersionComparator.isNewer("0.13.A01", "0.13.A01"))
    }

    @Test
    fun `正式版替代同号 Alpha`() {
        // 用户从 0.13.A05 升到 0.13.0 必须收到提示
        assertTrue(VersionComparator.isNewer("0.13.0", "0.13.A05"))
        assertFalse(VersionComparator.isNewer("0.13.A05", "0.13.0"))
    }

    @Test
    fun `能识别 Alpha 编号`() {
        assertTrue(VersionComparator.isAlpha("0.13.A01"))
        assertTrue(VersionComparator.isAlpha("v0.13.A1"))
        assertTrue(VersionComparator.isAlpha("0.13.0-alpha.2"))
        assertFalse(VersionComparator.isAlpha("0.13.0"))
        assertFalse(VersionComparator.isAlpha("0.13.0-beta"))
        assertFalse(VersionComparator.isAlpha(""))
        assertEquals(3, VersionComparator.alphaOf("0.13.A03") ?: -1)
        assertNull(VersionComparator.alphaOf("0.13.0"))
    }

    @Test
    fun `Alpha 版本的数字段只算前两段`() {
        assertEquals(listOf(0, 13), VersionComparator.parse("0.13.A01"))
        assertEquals(listOf(0, 13, 0), VersionComparator.parse("0.13.0"))
    }

    @Test
    fun `Alpha 版本保留 A0X 便于展示`() {
        assertEquals("0.13.A01", VersionComparator.normalize("v0.13.A01"))
        assertEquals("0.13.0.A02", VersionComparator.normalize("0.13.0-alpha.2"))
    }
}
