package com.chenzixin.wrongbook.data.update

/** 一条可用的更新。 */
data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val pageUrl: String,
    val sizeBytes: Long,
    val publishedAt: String,
)

/** 下载进度；[sourceLabel] 用于告诉用户这次走的是直连还是哪个镜像。 */
data class DownloadProgress(
    val downloaded: Long,
    val total: Long,
    val sourceLabel: String,
) {
    val percent: Int
        get() = if (total <= 0L) 0 else ((downloaded * 100) / total).toInt().coerceIn(0, 100)
}
