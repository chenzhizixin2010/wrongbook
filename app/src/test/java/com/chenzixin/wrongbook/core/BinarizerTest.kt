package com.chenzixin.wrongbook.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BinarizerTest {

    /** 生成一张「左亮右暗」的渐变图，模拟拍摄时的阴影。 */
    private fun gradient(width: Int, height: Int): IntArray = IntArray(width * height) { i ->
        val x = i % width
        (230 - x * 180 / width).coerceIn(0, 255)
    }

    @Test
    fun `均匀白底上的黑字被分开`() {
        val w = 100
        val h = 100
        val gray = IntArray(w * h) { 245 }
        // 中间画一个 20x20 的黑块当作字
        for (y in 40 until 60) for (x in 40 until 60) gray[y * w + x] = 20

        val out = Binarizer.sauvola(gray, w, h, radius = 10)
        assertEquals(255, out[10 * w + 10])
        assertEquals(0, out[50 * w + 50])
    }

    @Test
    fun `阴影下仍能分出笔画`() {
        val w = 200
        val h = 100
        val gray = gradient(w, h)
        // 在右侧（本来就暗）再加几道更黑的笔画
        for (y in 40 until 60) for (x in 150 until 170) gray[y * w + x] = 10

        val out = Binarizer.sauvola(gray, w, h, radius = 12)
        assertEquals(0, out[50 * w + 160])
        // 右侧的「纸面」仍应是白的
        assertEquals(255, out[10 * w + 160])
    }

    @Test
    fun `局部均匀的区域一律当背景`() {
        // Sauvola 的语义：比邻域均值亮 → 纸面；整片同色时全是纸面
        val w = 60
        val h = 60
        val gray = IntArray(w * h) { 15 }
        val out = Binarizer.sauvola(gray, w, h, radius = 8)
        assertTrue(out.all { it == 255 })
    }

    @Test
    fun `黑底输出会被反相成白底`() {
        // 构造一张「大部分判成黑」的结果，ensureWhiteBackground 应该整体反相
        val mostlyBlack = IntArray(100) { if (it < 70) 0 else 255 }
        val fixed = Binarizer.ensureWhiteBackground(mostlyBlack)
        assertEquals(0.3f, Binarizer.blackRatio(fixed), 0.02f)
        assertEquals(255, fixed[0])
    }

    @Test
    fun `黑像素比例用于判断二值化是否失败`() {
        val allWhite = IntArray(100) { 255 }
        assertEquals(0f, Binarizer.blackRatio(allWhite), 0.0001f)

        val allBlack = IntArray(100) { 0 }
        assertEquals(1f, Binarizer.blackRatio(allBlack), 0.0001f)

        val half = IntArray(100) { if (it < 50) 0 else 255 }
        assertEquals(0.5f, Binarizer.blackRatio(half), 0.0001f)
    }

    @Test
    fun `空输入不会崩`() {
        assertEquals(0, Binarizer.sauvola(IntArray(0), 0, 0).size)
        assertTrue(Binarizer.blackRatio(IntArray(0)) == 0f)
    }
}
