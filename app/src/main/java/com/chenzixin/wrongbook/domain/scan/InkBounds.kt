package com.chenzixin.wrongbook.domain.scan

/** 归一化内容框（0..1），用于拍照后自动框住有文字的区域。 */
data class ContentBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * 从「是不是墨迹」的采样里找出内容外接框。
 *
 * 与 Android 解耦：调用方传入采样函数，因此可以在 JVM 上直接测。
 * 判据很简单——有墨迹的最小/最大行列，再留一点点边；
 * 对「白纸黑字 + 四周大片空白」的试卷照片足够用，比固定 4% 内缩准得多。
 */
object InkBounds {

    fun detect(
        width: Int,
        height: Int,
        step: Int = 2,
        isInk: (x: Int, y: Int) -> Boolean,
    ): ContentBox? {
        if (width <= 0 || height <= 0) return null
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        var inkCount = 0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                if (isInk(x, y)) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    inkCount++
                }
                x += step
            }
            y += step
        }

        if (inkCount < MIN_INK_SAMPLES || maxX < minX || maxY < minY) return null

        // 留 3% 边，避免框贴着字
        val padX = width * 0.03f
        val padY = height * 0.03f
        return ContentBox(
            left = ((minX - padX) / width).coerceIn(0f, 1f),
            top = ((minY - padY) / height).coerceIn(0f, 1f),
            right = ((maxX + padX) / width).coerceIn(0f, 1f),
            bottom = ((maxY + padY) / height).coerceIn(0f, 1f),
        )
    }

    /** 采样点太少说明整张图都是空白，别用它当初始框。 */
    const val MIN_INK_SAMPLES = 8
}
