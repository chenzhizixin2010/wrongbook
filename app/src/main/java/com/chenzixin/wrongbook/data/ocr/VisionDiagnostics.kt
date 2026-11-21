package com.chenzixin.wrongbook.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 识图自检：在设备上现画一张「两道题」的图，走一遍真实的视觉切题流程。
 *
 * 目的：把「AI 识图到底通不通」这件事变成可观测的结果，
 * 而不是扫描完只看到一句「本地识别」，分不清是没配置、Key 不对还是模型不支持图片。
 */
@Singleton
class VisionDiagnostics @Inject constructor(
    private val segmenter: VisionQuestionSegmenter,
) {

    suspend fun run(profile: ApiProfileEntity, apiKey: String): WbResult<String> {
        val sample = buildSample()
        return when (val result = segmenter.segment(profile, apiKey, sample)) {
            is WbResult.Ok -> {
                val count = result.value.size
                val preview = result.value.firstOrNull()?.text?.take(40).orEmpty()
                WbResult.Ok("识图成功：识别到 $count 道题。示例文本「$preview」")
            }
            is WbResult.Err -> result
        }
    }

    /** 白底黑字，模拟一页试卷；不依赖任何资源文件。 */
    private fun buildSample(): Bitmap {
        val width = 1080
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 44f
        }
        val lines = listOf(
            "1. 已知函数 f(x)=x^2+2x，求它的最小值。",
            "A. -1    B. 0    C. 1    D. 2",
            "",
            "2. 计算 12 × 8 的值。",
            "A. 86    B. 96    C. 108    D. 112",
        )
        var y = 140f
        lines.forEach { line ->
            if (line.isNotEmpty()) canvas.drawText(line, 80f, y, paint)
            y += 90f
        }

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 90, 90)
            textSize = 30f
        }
        canvas.drawText("识图自检样张", 80f, 70f, title)
        return bitmap
    }
}
