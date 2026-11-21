package com.chenzixin.wrongbook.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.chenzixin.wrongbook.core.ImageUtils.scaledToMaxSide
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.core.runCatchingWb
import com.chenzixin.wrongbook.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * 对话附图：把相册/相机给的 Uri 复制进私有目录，发送时再读成 base64。
 *
 * 不直接把 base64 塞进数据库：一条消息几十 KB 的 base64 会把数据库撑爆。
 */
@Singleton
class ChatImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    private val dir: File
        get() = File(context.filesDir, DIR_CHAT).apply { if (!exists()) mkdirs() }

    suspend fun saveFromUri(uri: Uri): WbResult<String> = withContext(io) {
        runCatchingWb("图片读取失败") {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("无法打开图片")
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("图片格式不支持")
            val scaled = original.scaledToMaxSide(MAX_SIDE)
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== original) scaled.recycle()
            original.recycle()
            "$DIR_CHAT/${file.name}"
        }
    }

    /** 发送前读成 base64（不含 data: 前缀）；文件丢失返回 null。 */
    suspend fun readBase64(relativePath: String, maxSide: Int = 1200): String? = withContext(io) {
        runCatching {
            val file = File(context.filesDir, relativePath)
            if (!file.exists()) return@runCatching null
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
            val scaled = bitmap.scaledToMaxSide(maxSide)
            val out = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
        }.getOrNull()
    }

    private companion object {
        const val DIR_CHAT = "chat"
        const val MAX_SIDE = 1600
    }
}
