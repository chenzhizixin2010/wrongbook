package com.chenzixin.wrongbook.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.sr.TilePlan
import com.chenzixin.wrongbook.domain.sr.TilingPlanner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * 题目插图的 AI 超分（EDSR ×4，内置模型）。
 *
 * 选它的理由：
 * - **不做生成式增强**：Real-ESRGAN 这类 GAN 模型会「脑补」笔画，数学图形和公式被改一笔就变成错题，
 *   EDSR 是 PSNR 导向的，只会把已有的边缘拉清楚，不会凭空造细节；
 * - 体积小（7.4 MB）、纯卷积（68 个算子，全是最老的内置算子），CPU 也能跑，不需要 GPU 委托；
 * - 输入固定 128×128 → 输出 512×512，按 [TilingPlanner] 分块使用。
 *
 * 实测参考：Pixel 8a 上 CPU 约 0.5 秒/块（官方 benchmark 同口径）。
 * 因此默认最多 9 块（约 5 秒），超过就返回 null 让调用方退回 Lanczos —— 宁可慢一点，也不能让用户以为卡死。
 *
 * 模型来源：Hugging Face `litert-community/EDSR-x4-LiteRT`（Apache-2.0，权重来自 eugenesiow/edsr-base）。
 */
@Singleton
class SuperResolutionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    /** 超分结果：放大后的位图 + 用了多少块 + 耗时。 */
    data class Upscaled(
        val bitmap: Bitmap,
        val tiles: Int,
        val millis: Long,
    )

    private class Handle(
        val interpreter: Interpreter,
        val tile: Int,
        val scale: Int,
        val inputNchw: Boolean,
        val outputNchw: Boolean,
    )

    private var handle: Handle? = null
    private var loadFailed = false

    /** 模型能不能用（加载失败过一次就不再重试，避免每次点都卡一下）。 */
    fun isAvailable(): Boolean = runCatching { obtain() != null }.getOrDefault(false)

    /** 设置页展示用的一句话说明；加载模型要走 IO，所以是 suspend。 */
    suspend fun describe(): String = withContext(io) {
        if (isAvailable()) {
            "EDSR ×4 · 模型 ${MODEL_MB}MB · 最多 ${TilingPlanner.DEFAULT_MAX_TILES} 块/次"
        } else {
            "模型加载失败${lastError?.let { "（$it）" }.orEmpty()}，会自动退回普通放大"
        }
    }

    /**
     * 把 [source] 放大 4 倍。
     *
     * 返回 null 的情况：模型不可用、图片太大（块数超预算）、推理异常。
     * 调用方**必须**有非超分的兜底路径。
     */
    suspend fun upscale(
        source: Bitmap,
        maxTiles: Int = TilingPlanner.DEFAULT_MAX_TILES,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Upscaled? = withContext(io) {
        val h = obtain() ?: return@withContext null
        val width = source.width
        val height = source.height
        val plan = TilingPlanner.plan(
            width = width,
            height = height,
            tile = h.tile,
            scale = h.scale,
            pad = TilingPlanner.DEFAULT_PAD,
            maxTiles = maxTiles,
        ) ?: return@withContext null

        try {
            runPlan(h, source, plan, onProgress)
        } catch (t: Throwable) {
            // 不吞异常：把原因留给设置页/日志，并丢掉解释器，下次重绘会重新加载
            lastError = t.message ?: t.javaClass.simpleName
            runCatching { h.interpreter.close() }
            handle = null
            null
        }
    }

    /** 最近一次失败原因（给设置页自检与提示用）。 */
    @Volatile
    var lastError: String? = null
        private set

    private fun runPlan(
        h: Handle,
        source: Bitmap,
        plan: TilePlan,
        onProgress: (Int, Int) -> Unit,
    ): Upscaled {
        val srcWidth = plan.srcWidth
        val srcHeight = plan.srcHeight
        val pixels = IntArray(srcWidth * srcHeight)
        source.getPixels(pixels, 0, srcWidth, 0, 0, srcWidth, srcHeight)

        val outPixels = IntArray(plan.outWidth * plan.outHeight)
        val tileSide = h.tile
        val outSide = h.tile * h.scale
        val inBuf = ByteBuffer.allocateDirect(3 * tileSide * tileSide * 4).order(ByteOrder.nativeOrder())
        val outBuf = ByteBuffer.allocateDirect(3 * outSide * outSide * 4).order(ByteOrder.nativeOrder())
        val inFloats = inBuf.asFloatBuffer()
        val outFloats = outBuf.asFloatBuffer()

        val started = SystemClock.elapsedRealtime()
        plan.tiles.forEachIndexed { index, tile ->
            fillInput(inFloats, pixels, srcWidth, srcHeight, tile, tileSide, h.inputNchw)
            inBuf.rewind()
            outBuf.rewind()
            h.interpreter.run(inBuf, outBuf)
            copyOutput(outFloats, outPixels, plan, tile, outSide, h.outputNchw)
            onProgress(index + 1, plan.tiles.size)
        }

        val bitmap = Bitmap.createBitmap(plan.outWidth, plan.outHeight, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(outPixels, 0, plan.outWidth, 0, 0, plan.outWidth, plan.outHeight)
        return Upscaled(bitmap, plan.tiles.size, SystemClock.elapsedRealtime() - started)
    }

    /** 把一块源像素写成模型输入（RGB，0..1，越界按边缘复制）。 */
    private fun fillInput(
        dst: java.nio.FloatBuffer,
        pixels: IntArray,
        srcWidth: Int,
        srcHeight: Int,
        tile: TilePlan.Tile,
        side: Int,
        nchw: Boolean,
    ) {
        val plane = side * side
        for (y in 0 until side) {
            val sy = (tile.readTop + y).coerceIn(0, srcHeight - 1)
            val rowOffset = sy * srcWidth
            for (x in 0 until side) {
                val sx = (tile.readLeft + x).coerceIn(0, srcWidth - 1)
                val p = pixels[rowOffset + sx]
                val r = ((p shr 16) and 0xFF) / 255f
                val g = ((p shr 8) and 0xFF) / 255f
                val b = (p and 0xFF) / 255f
                if (nchw) {
                    val i = y * side + x
                    dst.put(i, r)
                    dst.put(plane + i, g)
                    dst.put(plane * 2 + i, b)
                } else {
                    val i = (y * side + x) * 3
                    dst.put(i, r)
                    dst.put(i + 1, g)
                    dst.put(i + 2, b)
                }
            }
        }
    }

    /** 把模型输出里「干净的那块」写回整图（裁掉 pad 边）。 */
    private fun copyOutput(
        src: java.nio.FloatBuffer,
        outPixels: IntArray,
        plan: TilePlan,
        tile: TilePlan.Tile,
        outSide: Int,
        nchw: Boolean,
    ) {
        val plane = outSide * outSide
        val scale = plan.scale
        val copyWidth = tile.cleanWidth * scale
        val copyHeight = tile.cleanHeight * scale
        val baseX = tile.insetX * scale
        val baseY = tile.insetY * scale

        for (y in 0 until copyHeight) {
            val py = baseY + y
            if (py !in 0 until outSide) continue
            val outRow = (tile.outTop + y) * plan.outWidth
            val inRow = py * outSide
            for (x in 0 until copyWidth) {
                val px = baseX + x
                if (px !in 0 until outSide) continue
                val i = inRow + px
                val r = if (nchw) src.get(i) else src.get(i * 3)
                val g = if (nchw) src.get(plane + i) else src.get(i * 3 + 1)
                val b = if (nchw) src.get(plane * 2 + i) else src.get(i * 3 + 2)
                outPixels[outRow + tile.outLeft + x] = argb(r, g, b)
            }
        }
    }

    private fun argb(r: Float, g: Float, b: Float): Int {
        val ri = clamp255(r)
        val gi = clamp255(g)
        val bi = clamp255(b)
        return (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
    }

    private fun clamp255(v: Float): Int {
        val scaled = (v * 255f).toInt()
        return scaled.coerceIn(0, 255)
    }

    /**
     * 自检：拿一张合成渐变图跑一次，返回耗时毫秒。
     *
     * 用户可以在设置页点一下，确认「这台机器上模型真的能跑」——
     * 比在日志里写「已加载」有用得多。
     */
    suspend fun selfTest(): WbResult<Long> {
        val h = runCatching { obtain() }.getOrNull()
            ?: return WbResult.Err("超分模型加载失败（$ASSET 缺失或与本机 ABI 不兼容）")
        return withContext(io) {
            runCatching {
                val side = h.tile
                val pixels = IntArray(side * side) { i ->
                    val x = i % side
                    val y = i / side
                    val v = ((x + y) * 255) / (side * 2)
                    (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                }
                val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, side, 0, 0, side, side)
                val result = upscale(bitmap, maxTiles = 1)?.millis
                    ?: return@runCatching -1L
                bitmap.recycle()
                result
            }.fold(
                onSuccess = { ms ->
                    if (ms >= 0) WbResult.Ok(ms) else WbResult.Err("模型能加载但推理没结果")
                },
                onFailure = { t -> WbResult.Err("超分自检失败：${t.message ?: "未知错误"}", t) },
            )
        }
    }

    // ---------------------------------------------------------------- 加载

    private fun obtain(): Handle? {
        handle?.let { return it }
        if (loadFailed) return null
        return try {
            val model = mapAsset(ASSET)
            val options = Interpreter.Options().apply {
                setNumThreads(THREADS)
                // 关掉 XNNPACK 之外的猜测：默认就好，缓存目录交给 TFLite 管
                setUseXNNPACK(true)
            }
            val interpreter = Interpreter(model, options)
            val inShape = interpreter.getInputTensor(0).shape()
            val outShape = interpreter.getOutputTensor(0).shape()
            if (inShape.size != 4 || outShape.size != 4) {
                runCatching { interpreter.close() }
                loadFailed = true
                return null
            }
            val inputNchw = inShape[1] == 3
            val tile = if (inputNchw) inShape[2] else inShape[1]
            val outputNchw = outShape[1] == 3
            val outSide = if (outputNchw) outShape[2] else outShape[1]
            val scale = if (outSide > 0 && tile > 0) outSide / tile else 4
            if (scale < 2) {
                runCatching { interpreter.close() }
                loadFailed = true
                return null
            }
            Handle(interpreter, tile, scale, inputNchw, outputNchw).also { handle = it }
        } catch (t: Throwable) {
            lastError = t.message ?: t.javaClass.simpleName
            loadFailed = true
            null
        }
    }

    private fun mapAsset(path: String): MappedByteBuffer {
        context.assets.openFd(path).use { fd ->
            FileInputStream(fd.fileDescriptor).use { input ->
                return input.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength,
                )
            }
        }
    }

    private companion object {
        const val ASSET = "models/edsr_x4.tflite"

        /** 4 核足够；再多会和前台线程抢 CPU，反而更慢。 */
        const val THREADS = 4

        const val MODEL_MB = "7.4"
    }
}
