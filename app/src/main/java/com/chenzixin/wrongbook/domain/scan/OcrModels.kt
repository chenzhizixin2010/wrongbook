package com.chenzixin.wrongbook.domain.scan

/**
 * OCR 与题目切分的领域模型。
 *
 * 全部用纯 Kotlin 类型（不用 android.graphics.RectF / Bitmap），
 * 这样切分算法可以在 JVM 单测里跑，不需要真机或 Robolectric。
 */

/** 一行 OCR 结果，坐标是原图像素。 */
data class OcrLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float = 0.5f,
) {
    val height: Float get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2f
}

data class OcrResult(
    val imageWidth: Int,
    val imageHeight: Int,
    val lines: List<OcrLine>,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

/** 归一化矩形（0..1），与显示尺寸无关。 */
data class NormRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun clampToUnit(): NormRect = NormRect(
        left = left.coerceIn(0f, 1f),
        top = top.coerceIn(0f, 1f),
        right = right.coerceIn(0f, 1f),
        bottom = bottom.coerceIn(0f, 1f),
    )

    companion object {
        val ZERO = NormRect(0f, 0f, 0f, 0f)
    }
}

/** 切出来的一道题。 */
data class QuestionRegion(
    /** 题号：解析到就是 1..999；大题号或无题号时为 -1 */
    val index: Int,
    /** 原始题号文本，如 "12." / "（3）" / "三、" */
    val label: String,
    val rect: NormRect,
    val text: String,
    val confidence: Float,
    /** 是否是「一、二、三」这种大题标题（默认不勾选，仅用作分隔） */
    val isSectionHeader: Boolean = false,
)

/** 题号锚点识别。正则集中在这里，方便单测与后续按学科调整。 */
object AnchorParser {

    sealed interface Anchor {
        val label: String
    }

    data class Numbered(val index: Int, override val label: String) : Anchor

    data class Section(override val label: String) : Anchor

    // 12.  12、 12． 12)  (12)  （12）
    // 注意末尾的 (?!\d)：避免把 "3.14" 这种小数值误判成题号 3
    private val numbered = Regex("""^\s*(?:[（(]\s*(\d{1,3})\s*[）)]|(\d{1,3})\s*[.、．)）](?!\d))\s*""")

    // 一、 二. 三． (1~3 个汉字数字)
    private val section = Regex("""^\s*([一二三四五六七八九十]{1,3})\s*[、.．]\s*""")

    fun parse(text: String): Anchor? {
        val line = text.take(24)
        val numberedMatch = numbered.find(line)
        if (numberedMatch != null) {
            val value = numberedMatch.groupValues[1].ifEmpty { numberedMatch.groupValues[2] }
            val index = value.toIntOrNull() ?: return null
            return Numbered(index, numberedMatch.value.trim())
        }
        val sectionMatch = section.find(line)
        if (sectionMatch != null) {
            return Section(sectionMatch.value.trim())
        }
        return null
    }
}
