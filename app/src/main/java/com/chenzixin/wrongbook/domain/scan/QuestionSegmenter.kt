package com.chenzixin.wrongbook.domain.scan

/**
 * 本地题目切分（纯函数，JVM 可测）。
 *
 * 思路：
 * 1. 行按 Y 排序，算行高中位数；
 * 2. 找题号锚点（`12.`、`（3）`、`三、`）；
 * 3. 有锚点 → 从当前锚点行到下一个锚点行 = 一道题；
 * 4. 没有锚点（图片没有题号）→ 按行间距突变（> 1.6 倍中位行高）切段，每段算一道题；
 * 5. 输出归一化坐标，UI 直接乘显示尺寸画框。
 *
 * 刻意不做「视觉模型兜底」：那属于上层编排，切分本身要保持确定性、可单测。
 */
object QuestionSegmenter {

    private const val GAP_FACTOR = 1.6f

    fun segment(ocr: OcrResult): List<QuestionRegion> {
        if (ocr.isEmpty || ocr.imageWidth <= 0 || ocr.imageHeight <= 0) return emptyList()

        val lines = ocr.lines.sortedBy { it.top }
        val medianHeight = medianHeightOf(lines)
        val anchors = lines.mapNotNull { line ->
            AnchorParser.parse(line.text)?.let { anchor -> line to anchor }
        }

        val regions = if (anchors.isEmpty()) {
            segmentByGaps(lines, ocr, medianHeight)
        } else {
            segmentByAnchors(anchors, lines, ocr)
        }

        return regions
            .map { it.copy(rect = it.rect.clampToUnit()) }
            .filter { it.rect.width > 0.01f && it.rect.height > 0.005f && it.text.isNotBlank() }
    }

    private fun segmentByAnchors(
        anchors: List<Pair<OcrLine, AnchorParser.Anchor>>,
        allLines: List<OcrLine>,
        ocr: OcrResult,
    ): List<QuestionRegion> {
        val result = mutableListOf<QuestionRegion>()
        anchors.forEachIndexed { i, (anchorLine, anchor) ->
            val nextTop = anchors.getOrNull(i + 1)?.first?.top ?: ocr.imageHeight.toFloat()
            val blockLines = allLines.filter { it.centerY >= anchorLine.top && it.centerY < nextTop }
            if (blockLines.isEmpty()) return@forEachIndexed
            val isSection = anchor is AnchorParser.Section
            // 大题标题只有后面紧跟着小题时才保留，否则它就是个孤立的标题行
            if (isSection && blockLines.size == 1) return@forEachIndexed

            val bounds = boundsOf(blockLines, ocr)
            result += QuestionRegion(
                index = (anchor as? AnchorParser.Numbered)?.index ?: -1,
                label = anchor.label,
                rect = bounds,
                text = blockLines.joinToString("\n") { it.text },
                confidence = 0.9f,
                isSectionHeader = isSection,
            )
        }
        return result
    }

    private fun segmentByGaps(
        lines: List<OcrLine>,
        ocr: OcrResult,
        medianHeight: Float,
    ): List<QuestionRegion> {
        if (lines.isEmpty()) return emptyList()
        val blocks = mutableListOf<MutableList<OcrLine>>()
        var current = mutableListOf(lines.first())

        for (i in 1 until lines.size) {
            val prev = lines[i - 1]
            val line = lines[i]
            val gap = line.top - prev.bottom
            if (gap > medianHeight * GAP_FACTOR) {
                blocks += current
                current = mutableListOf(line)
            } else {
                current += line
            }
        }
        blocks += current

        return blocks.map { block ->
            QuestionRegion(
                index = -1,
                label = "",
                rect = boundsOf(block, ocr),
                text = block.joinToString("\n") { it.text },
                confidence = 0.5f,
                isSectionHeader = false,
            )
        }
    }

    private fun boundsOf(lines: List<OcrLine>, ocr: OcrResult): NormRect {
        val left = lines.minOf { it.left }
        val top = lines.minOf { it.top }
        val right = lines.maxOf { it.right }
        val bottom = lines.maxOf { it.bottom }
        val w = ocr.imageWidth.toFloat()
        val h = ocr.imageHeight.toFloat()
        return NormRect(left / w, top / h, right / w, bottom / h)
    }

    private fun medianHeightOf(lines: List<OcrLine>): Float {
        if (lines.isEmpty()) return 0f
        val heights = lines.map { it.height }.sorted()
        return heights[heights.size / 2].coerceAtLeast(1f)
    }
}
