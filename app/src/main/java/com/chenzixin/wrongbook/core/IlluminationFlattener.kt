package com.chenzixin.wrongbook.core

/**
 * 纸面提亮：保留墨迹颜色，只把"纸"推到接近纯白。
 *
 * 做法是光照归一化：
 * 1. 分块取**最大亮度**当作这一块的纸面（墨迹比纸暗，取最大能避开墨迹）；
 * 2. 对小图做一次模糊，得到平滑的光照场（模拟灯光/手影）；
 * 3. 每个像素按 `255 / 光照` 放大，最后做一次轻微 gamma。
 *
 * 效果：阴影下的灰纸变白、铅笔的浅灰仍在、红笔蓝笔仍是彩色。
 * 纯 JVM 实现，可以直接在单测里验证。
 */
object IlluminationFlattener {

    /** 分块降采样时每块取的边长（原图像素）。 */
    private const val BLOCK = 8

    fun flatten(argb: IntArray, width: Int, height: Int, strength: Float = 1f): IntArray {
        if (width <= 0 || height <= 0 || argb.size < width * height) return argb

        val smallW = (width + BLOCK - 1) / BLOCK
        val smallH = (height + BLOCK - 1) / BLOCK
        val light = FloatArray(smallW * smallH)

        // 1) 每块取最大亮度（近似纸面）
        for (by in 0 until smallH) {
            for (bx in 0 until smallW) {
                var maxLum = 0f
                val x0 = bx * BLOCK
                val y0 = by * BLOCK
                val x1 = minOf(width, x0 + BLOCK)
                val y1 = minOf(height, y0 + BLOCK)
                for (y in y0 until y1) {
                    for (x in x0 until x1) {
                        val c = argb[y * width + x]
                        val lum = 0.299f * ((c shr 16) and 0xFF) +
                            0.587f * ((c shr 8) and 0xFF) +
                            0.114f * (c and 0xFF)
                        if (lum > maxLum) maxLum = lum
                    }
                }
                light[by * smallW + bx] = maxLum
            }
        }

        // 2) 小图模糊，得到平滑光照场
        val blurred = boxBlur(light, smallW, smallH, radius = 2)

        // 3) 逐像素按光照归一化
        val out = IntArray(width * height)
        for (y in 0 until height) {
            val fy = y.toFloat() / BLOCK - 0.5f
            val y0 = fy.toInt().coerceIn(0, smallH - 1)
            val y1 = (y0 + 1).coerceAtMost(smallH - 1)
            val ty = (fy - y0).coerceIn(0f, 1f)

            for (x in 0 until width) {
                val fx = x.toFloat() / BLOCK - 0.5f
                val x0 = fx.toInt().coerceIn(0, smallW - 1)
                val x1 = (x0 + 1).coerceAtMost(smallW - 1)
                val tx = (fx - x0).coerceIn(0f, 1f)

                val bg = lerp(
                    lerp(blurred[y0 * smallW + x0], blurred[y0 * smallW + x1], tx),
                    lerp(blurred[y1 * smallW + x0], blurred[y1 * smallW + x1], tx),
                    ty,
                ).coerceAtLeast(28f)

                val c = argb[y * width + x]
                val a = (c ushr 24) and 0xFF
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                // 目标纸面 252：比纯白略低一点，避免过曝后细节发死；
                // 本来就接近纯白的区域不再放大，免得把 255 压成 252
                val scale = if (bg >= 250f) 1f else 252f / bg
                out[y * width + x] = (a shl 24) or
                    (adjust(r, scale, strength) shl 16) or
                    (adjust(g, scale, strength) shl 8) or
                    adjust(b, scale, strength)
            }
        }
        return out
    }

    /** strength 用来让用户选"轻提亮 / 强提亮"：1 = 完全归一化到白纸。 */
    private fun adjust(channel: Int, scale: Float, strength: Float): Int {
        val effective = 1f + (scale - 1f) * strength
        return (channel * effective).toInt().coerceIn(0, 255)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun boxBlur(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        if (radius <= 0) return src
        val tmp = FloatArray(w * h)
        val out = FloatArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                var count = 0
                for (dx in -radius..radius) {
                    val nx = x + dx
                    if (nx in 0 until w) {
                        sum += src[y * w + nx]
                        count++
                    }
                }
                tmp[y * w + x] = sum / count
            }
        }
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                var count = 0
                for (dy in -radius..radius) {
                    val ny = y + dy
                    if (ny in 0 until h) {
                        sum += tmp[ny * w + x]
                        count++
                    }
                }
                out[y * w + x] = sum / count
            }
        }
        return out
    }
}
