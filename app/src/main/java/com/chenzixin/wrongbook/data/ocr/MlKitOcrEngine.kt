package com.chenzixin.wrongbook.data.ocr

import android.graphics.Bitmap
import com.chenzixin.wrongbook.core.awaitTask
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.scan.OcrLine
import com.chenzixin.wrongbook.domain.scan.OcrResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** OCR 抽象：真机用 ML Kit，测试/预览可以注入假实现。 */
interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): OcrResult
}

/**
 * ML Kit 中文识别（捆绑版模型，随 APK 安装，无需 GMS 下载）。
 *
 * 注意：TextRecognizer 是重对象，必须复用同一个实例；close 只在进程结束时发生。
 */
@Singleton
class MlKitOcrEngine @Inject constructor(
    @IoDispatcher private val io: CoroutineDispatcher,
) : OcrEngine {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    override suspend fun recognize(bitmap: Bitmap): OcrResult = withContext(io) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).awaitTask()

        val lines = mutableListOf<OcrLine>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                lines += OcrLine(
                    text = line.text,
                    left = box.left.toFloat(),
                    top = box.top.toFloat(),
                    right = box.right.toFloat(),
                    bottom = box.bottom.toFloat(),
                    // ML Kit 不暴露行级置信度，给一个中性值，切分算法不依赖它做判断
                    confidence = 0.8f,
                )
            }
        }
        OcrResult(imageWidth = bitmap.width, imageHeight = bitmap.height, lines = lines)
    }
}
