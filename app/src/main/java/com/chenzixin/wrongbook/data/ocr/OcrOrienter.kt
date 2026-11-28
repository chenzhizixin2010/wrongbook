package com.chenzixin.wrongbook.data.ocr

import android.graphics.Bitmap
import com.chenzixin.wrongbook.core.ImageUtils.rotated
import com.chenzixin.wrongbook.core.ImageUtils.scaledToMaxSide
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动摆正：横着拍的照片先转正，再交给 OCR。
 *
 * 判据很朴素但很稳：四个方向上分别跑一遍低分辨率 OCR，**识别出的有效字符最多**的那个就是正的方向。
 * 试卷、答案页上文字远多于噪声，所以这个判据比图像特征更可靠（老图上投影轮廓经常分不出来）。
 *
 * 竖图只比 0°/180°（拍反了的情况），横图比四个方向 —— 少跑两次 OCR，省一半时间。
 */
@Singleton
class OcrOrienter @Inject constructor(
    private val ocr: OcrEngine,
) {

    /** 打分用的缩略图长边；太小会把小字糊掉，太大又慢。 */
    private val probeSide = 1200

    suspend fun upright(bitmap: Bitmap): Bitmap {
        val landscape = bitmap.width > bitmap.height
        val candidates = if (landscape) listOf(0, 90, 180, 270) else listOf(0, 180)
        val small = if (maxOf(bitmap.width, bitmap.height) > probeSide) {
            bitmap.scaledToMaxSide(probeSide)
        } else {
            bitmap
        }

        var bestDeg = 0
        var bestScore = -1
        candidates.forEach { deg ->
            val rotated = if (deg == 0) small else small.rotated(deg.toFloat())
            val score = runCatching {
                ocr.recognize(rotated).lines.sumOf { line -> line.text.count { it.isLetterOrDigit() } }
            }.getOrDefault(0)
            if (score > bestScore) {
                bestScore = score
                bestDeg = deg
            }
        }

        if (small !== bitmap) small.recycle()
        return if (bestDeg == 0) bitmap else bitmap.rotated(bestDeg.toFloat())
    }
}
