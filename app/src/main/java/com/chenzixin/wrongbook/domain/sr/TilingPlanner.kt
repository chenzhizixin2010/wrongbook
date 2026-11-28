package com.chenzixin.wrongbook.domain.sr

/**
 * 超分分块计划（纯函数，可在 JVM 上单测）。
 *
 * 为什么要分块：内置的 EDSR×4 模型输入固定为 128×128，整张图塞不进去。
 * 又因为卷积核在块边界看到的是补齐出来的假像素，直接按 128 切会有明显接缝——
 * 所以每块只「认领」中间 [step] = 128 - 2*pad 的范围，向外多读一圈 pad，算完把 pad 丢掉。
 */
data class TilePlan(
    val srcWidth: Int,
    val srcHeight: Int,
    val tile: Int,
    val scale: Int,
    val pad: Int,
    val step: Int,
    val cols: Int,
    val rows: Int,
    val tiles: List<Tile>,
) {
    val outWidth: Int get() = srcWidth * scale
    val outHeight: Int get() = srcHeight * scale
    val count: Int get() = tiles.size

    /**
     * 一块的几何信息。
     *
     * - [readLeft]/[readTop]：送给模型的 tile×tile 区域在源图上的左上角（越界由引擎按边复制）
     * - [insetX]/[insetY]：这块里「真正要的干净区域」相对读取区域的偏移（像素）
     * - [cleanWidth]/[cleanHeight]：干净区域尺寸（≤ step）
     * - [outLeft]/[outTop]：干净区域放大后落在输出图上的左上角
     */
    data class Tile(
        val readLeft: Int,
        val readTop: Int,
        val insetX: Int,
        val insetY: Int,
        val cleanWidth: Int,
        val cleanHeight: Int,
        val outLeft: Int,
        val outTop: Int,
    )
}

object TilingPlanner {

    /** 每块输入的边长。模型要求固定 128。 */
    const val DEFAULT_TILE = 128

    /** 每块向外多读的像素，算完裁掉，接缝落在丢弃区。 */
    const val DEFAULT_PAD = 8

    /** 默认最多算多少块：手机上约 0.4~0.6 秒/块，9 块 ≈ 5 秒，再多用户会以为卡死。 */
    const val DEFAULT_MAX_TILES = 9

    /**
     * 生成分块计划。
     *
     * 返回 null 表示不该用超分：尺寸非法，或者块数超过 [maxTiles]（太慢，调用方应退回 Lanczos）。
     */
    fun plan(
        width: Int,
        height: Int,
        tile: Int = DEFAULT_TILE,
        scale: Int = 4,
        pad: Int = DEFAULT_PAD,
        maxTiles: Int = DEFAULT_MAX_TILES,
    ): TilePlan? {
        if (width <= 0 || height <= 0 || tile <= 0 || scale <= 0) return null
        if (pad < 0 || pad * 2 >= tile) return null
        if (maxTiles <= 0) return null

        val step = tile - pad * 2
        val cols = (width + step - 1) / step
        val rows = (height + step - 1) / step
        if (cols.toLong() * rows.toLong() > maxTiles) return null

        val tiles = ArrayList<TilePlan.Tile>(cols * rows)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cleanLeft = col * step
                val cleanTop = row * step
                val cleanWidth = minOf(step, width - cleanLeft)
                val cleanHeight = minOf(step, height - cleanTop)
                if (cleanWidth <= 0 || cleanHeight <= 0) continue

                // 读取区域尽可能带上 pad，但不能越出图片（越界部分由引擎按边复制）
                val readLeft = (cleanLeft - pad)
                    .coerceAtLeast(0)
                    .let { left -> if (width >= tile) minOf(left, width - tile) else left }
                val readTop = (cleanTop - pad)
                    .coerceAtLeast(0)
                    .let { top -> if (height >= tile) minOf(top, height - tile) else top }

                tiles += TilePlan.Tile(
                    readLeft = readLeft,
                    readTop = readTop,
                    insetX = cleanLeft - readLeft,
                    insetY = cleanTop - readTop,
                    cleanWidth = cleanWidth,
                    cleanHeight = cleanHeight,
                    outLeft = cleanLeft * scale,
                    outTop = cleanTop * scale,
                )
            }
        }
        return TilePlan(
            srcWidth = width,
            srcHeight = height,
            tile = tile,
            scale = scale,
            pad = pad,
            step = step,
            cols = cols,
            rows = rows,
            tiles = tiles,
        )
    }
}
