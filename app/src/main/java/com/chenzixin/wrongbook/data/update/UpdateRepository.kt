package com.chenzixin.wrongbook.data.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 从 GitHub Releases 拉更新。
 *
 * 设计要点：
 * 1. **只读 Releases API**，不依赖任何自建服务；
 * 2. APK 下载支持镜像前缀（`https://ghproxy.net/` 之类），直连失败自动逐个换；
 * 3. 下载完调系统安装器（FileProvider 授权临时读权限），不做静默安装。
 */
@Singleton
class UpdateRepository @Inject constructor(
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 检查最新 release；没有可用 APK 或版本不比当前新时返回 Ok(null)。 */
    suspend fun check(
        owner: String,
        repo: String,
        currentVersion: String,
        includePrerelease: Boolean = false,
    ): WbResult<UpdateInfo?> = withContext(io) {
        // Beta 通道要看全部 release（含 pre-release）；稳定通道只看 latest
        val url = if (includePrerelease) {
            "https://api.github.com/repos/$owner/$repo/releases?per_page=30"
        } else {
            "https://api.github.com/repos/$owner/$repo/releases/latest"
        }
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "WrongBook-Android")
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    // 只有预发布版本时，stable 通道会 404 —— 给一句能指导操作的提示
                    response.code == 404 && !includePrerelease ->
                        return@withContext WbResult.Err(
                            "当前只发布了测试版；在「设置 → 关于 → Beta 计划」开启后即可获取",
                        )
                    response.code == 404 -> return@withContext WbResult.Err("仓库还没有发布任何版本")
                    response.code == 403 -> return@withContext WbResult.Err("GitHub 接口被限流，过一会儿再试")
                    !response.isSuccessful -> return@withContext WbResult.Err("检查更新失败（HTTP ${response.code}）")
                }

                val body = response.body.string()
                val rootElement = runCatching { json.parseToJsonElement(body) }.getOrNull()
                    ?: return@withContext WbResult.Err("GitHub 返回内容无法解析")

                // 两条通道：单对象（latest）或多元素数组（含预发布）
                val candidates = when (rootElement) {
                    is kotlinx.serialization.json.JsonArray -> rootElement.mapNotNull { it as? kotlinx.serialization.json.JsonObject }
                    is kotlinx.serialization.json.JsonObject -> listOf(rootElement)
                    else -> emptyList()
                }

                val infos = candidates.mapNotNull { obj -> toInfo(obj) }
                    .filter { it.apkUrl.isNotBlank() }

                if (infos.isEmpty()) return@withContext WbResult.Ok(null)

                // Beta 通道取「版本号最大」的那个（可能同时存在正式版与预发布）
                val best = if (includePrerelease) {
                    infos.reduce { a, b ->
                        if (com.chenzixin.wrongbook.domain.update.VersionComparator.isNewer(b.version, a.version)) b else a
                    }
                } else {
                    infos.first()
                }

                val newer = com.chenzixin.wrongbook.domain.update.VersionComparator
                    .isNewer(best.version, currentVersion)
                if (newer) WbResult.Ok(best) else WbResult.Ok(null)
            }
        } catch (e: IOException) {
            WbResult.Err("网络不可用，检查更新失败", e)
        } catch (t: Throwable) {
            WbResult.Err("检查更新失败：${t.message ?: "未知错误"}", t)
        }
    }

    /** 把 GitHub 的 release JSON 转成 UpdateInfo；字段缺失就返回 null。 */
    private fun toInfo(root: kotlinx.serialization.json.JsonObject): UpdateInfo? {
        val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (tag.isBlank()) return null
        val apk = root["assets"]?.jsonArray.orEmpty()
            .mapNotNull { it.jsonObject }
            .firstOrNull { asset ->
                asset["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true
            } ?: return null

        return UpdateInfo(
            version = tag,
            notes = root["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            apkUrl = apk["browser_download_url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            pageUrl = root["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            sizeBytes = apk["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
            publishedAt = root["published_at"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            isPrerelease = root["prerelease"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
        )
    }

    /** 候选下载地址：直连 + 镜像，按顺序尝试。 */
    fun candidates(apkUrl: String): List<Pair<String, String>> {
        val label = { url: String ->
            when {
                url == apkUrl -> "直连"
                else -> url.substringBefore("https://", "").ifBlank { "镜像" }
            }
        }
        val mirrors = BuildConfig.UPDATE_MIRRORS.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return buildList {
            add(apkUrl to "直连")
            mirrors.forEach { mirror ->
                val full = mirror.trimEnd('/') + "/" + apkUrl
                add(full to label(mirror))
            }
        }
    }

    /**
     * 并行测速：每个候选源拉 64KB，按耗时排序；失败的排到最后。
     *
     * 公共镜像站速度差异极大（实测 5KB/s ~ 240KB/s），固定顺序会让用户一直等最慢的那条。
     */
    suspend fun rankedCandidates(apkUrl: String): List<Pair<String, String>> = coroutineScope {
        val all = candidates(apkUrl)
        val scored = all.map { (url, label) -> async { Triple(url, label, probe(url)) } }.awaitAll()
        val alive = scored.filter { it.third != null }.sortedBy { it.third }
        val dead = scored.filter { it.third == null }
        (alive + dead).map { it.first to it.second }
    }

    /** 拉一小段测延迟；不可用返回 null。 */
    private suspend fun probe(url: String): Int? = withContext(io) {
        withTimeoutOrNull(PROBE_TIMEOUT_MS) {
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-$PROBE_BYTES")
                    .header("User-Agent", "WrongBook-Android")
                    .build()
                val started = System.currentTimeMillis()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val read = response.body.byteStream().use { input ->
                        // 不能用 InputStream.readNBytes：那是 API 33 才有的方法，
                        // minSdk 24 的机器上会抛 NoSuchMethodError（之前点下载就崩在这）
                        val buffer = ByteArray(8 * 1024)
                        var total = 0
                        var remaining = PROBE_BYTES + 1
                        while (remaining > 0) {
                            val n = input.read(buffer, 0, minOf(buffer.size, remaining))
                            if (n <= 0) break
                            total += n
                            remaining -= n
                        }
                        total
                    }
                    if (read < 1024) null else (System.currentTimeMillis() - started).toInt()
                }
            }.getOrNull()
        }
    }

    /**
     * 下载 APK；先测速挑最快源，失败再换下一个。进度通过 [DownloadProgress] 回传。
     */
    fun download(info: UpdateInfo): Flow<DownloadProgress> = flow {
        val dir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
        val target = File(dir, "wrongbook-${info.version}.apk")

        var lastError: Throwable? = null
        emit(DownloadProgress(0, info.sizeBytes, "正在测速选择最快的源…"))
        for ((url, label) in rankedCandidates(info.apkUrl)) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WrongBook-Android")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

                    val body = response.body
                    val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                    var downloaded = 0L
                    var lastEmit = System.currentTimeMillis()
                    var lastBytes = 0L
                    var speed = 0L
                    emit(DownloadProgress(0, total, label))

                    body.byteStream().use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                val now = System.currentTimeMillis()
                                val elapsed = now - lastEmit
                                if (elapsed >= 150) {
                                    val instant = ((downloaded - lastBytes) * 1000L) / elapsed
                                    // 指数平滑，避免速度数字剧烈跳动
                                    speed = if (speed == 0L) instant else (speed * 0.6f + instant * 0.4f).toLong()
                                    lastEmit = now
                                    lastBytes = downloaded
                                    emit(DownloadProgress(downloaded, total, label, speed))
                                }
                            }
                        }
                    }
                    emit(DownloadProgress(downloaded, total, label, speed))
                }
                // 校验：至少 1MB，且不是 HTML 错误页
                if (target.length() < 1024 * 1024) throw IOException("下载内容不完整")
                return@flow
            } catch (t: Throwable) {
                lastError = t
                target.delete()
                continue
            }
        }
        throw IOException(lastError?.message ?: "所有下载源都失败")
    }.flowOn(io)

    fun downloadedApk(info: UpdateInfo): File =
        File(File(context.cacheDir, "updates"), "wrongbook-${info.version}.apk")

    /** 拉起系统安装器；Android 8+ 需要用户允许「安装未知应用」。 */
    fun install(apk: File): WbResult<Unit> {
        if (!apk.exists()) return WbResult.Err("安装包不存在，请重新下载")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return WbResult.Err("需要先允许「安装未知应用」，请在系统设置里开启后重试")
        }
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            WbResult.Ok(Unit)
        } catch (t: Throwable) {
            WbResult.Err("拉起安装器失败：${t.message ?: "未知错误"}", t)
        }
    }

    /** 跳转「安装未知应用」设置页（部分 ROM 需要用户手动找）。 */
    fun openInstallSettings(): WbResult<Unit> = try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(android.net.Uri.parse("package:${context.packageName}"))
        } else {
            Intent(android.provider.Settings.ACTION_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        WbResult.Ok(Unit)
    } catch (t: Throwable) {
        WbResult.Err("打不开设置页", t)
    }

    private companion object {
        const val PROBE_BYTES = 64 * 1024
        const val PROBE_TIMEOUT_MS = 4_000L
    }
}
