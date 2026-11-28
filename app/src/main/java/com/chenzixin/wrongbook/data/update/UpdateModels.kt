package com.chenzixin.wrongbook.data.update

/** 一条可用的更新。 */
data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val pageUrl: String,
    val sizeBytes: Long,
    val publishedAt: String,
    /** GitHub 上标记为 pre-release 的版本（Beta / Alpha 通道） */
    val isPrerelease: Boolean = false,
    /** 版本号带 Alpha 编号（`0.13.A01`），只有 Alpha 通道会收到 */
    val isAlpha: Boolean = false,
)

/** 下载进度；[sourceLabel] 用于告诉用户这次走的是直连还是哪个镜像。 */
data class DownloadProgress(
    val downloaded: Long,
    val total: Long,
    val sourceLabel: String,
    /** 近似瞬时速度（字节/秒），做了平滑，UI 上不会跳来跳去 */
    val bytesPerSecond: Long = 0L,
) {
    val percent: Int
        get() = if (total <= 0L) 0 else ((downloaded * 100) / total).toInt().coerceIn(0, 100)

    /** 剩余秒数；总大小或速度未知时返回 null。 */
    val etaSeconds: Long?
        get() {
            if (bytesPerSecond <= 0L || total <= 0L || downloaded >= total) return null
            return ((total - downloaded) / bytesPerSecond).coerceAtLeast(0L)
        }

    fun speedText(): String = when {
        bytesPerSecond <= 0L -> "计算中…"
        bytesPerSecond < 1024 -> "${bytesPerSecond} B/s"
        bytesPerSecond < 1024 * 1024 -> String.format("%.0f KB/s", bytesPerSecond / 1024f)
        else -> String.format("%.1f MB/s", bytesPerSecond / 1024f / 1024f)
    }

    fun sizeText(): String = String.format(
        "%.1f / %.1f MB",
        downloaded / 1024f / 1024f,
        total / 1024f / 1024f,
    )
}
