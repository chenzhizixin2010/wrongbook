package com.chenzixin.wrongbook.data.maintenance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 缩略图路径推导。
 *
 * 这里锁的是一个**丢数据**的坑：旧实现用 `path.replace(".webp", "_thumb.webp")`，
 * 遇到非 `.webp` 的老图片（比如 `.jpg`）会得到和主图一模一样的路径，
 * 于是重处理时拿 200px 的缩略图把主图覆盖了。
 */
class ThumbPathTest {

    @Test
    fun `webp 主图推出同名缩略图`() {
        assertEquals(
            "images/q_1_thumb.webp",
            ImageMaintenance.thumbPathOf("images/q_1.webp"),
        )
    }

    @Test
    fun `非 webp 老图按扩展名正确推导`() {
        assertEquals(
            "images/q_1_thumb.webp",
            ImageMaintenance.thumbPathOf("images/q_1.jpg"),
        )
        assertEquals(
            "images/q_1_thumb.webp",
            ImageMaintenance.thumbPathOf("images/q_1.png"),
        )
    }

    @Test
    fun `平铺在根目录的旧路径也能推导`() {
        assertEquals("q_1_thumb.webp", ImageMaintenance.thumbPathOf("q_1.webp"))
    }

    @Test
    fun `没有扩展名就放弃重建缩略图`() {
        assertNull(ImageMaintenance.thumbPathOf("images/noext"))
        assertNull(ImageMaintenance.thumbPathOf("noext"))
        assertNull(ImageMaintenance.thumbPathOf(""))
    }

    @Test
    fun `点号在目录名里不会被误当成扩展名`() {
        assertNull(ImageMaintenance.thumbPathOf("images.d/2026/q1"))
    }

    @Test
    fun `推导结果绝不会等于主图路径`() {
        listOf("a.webp", "dir/a.webp", "dir/a.jpg", "dir.with.dot/a").forEach { path ->
            val thumb = ImageMaintenance.thumbPathOf(path)
            if (thumb != null) {
                assertEquals(false, thumb == path)
            }
        }
    }
}
