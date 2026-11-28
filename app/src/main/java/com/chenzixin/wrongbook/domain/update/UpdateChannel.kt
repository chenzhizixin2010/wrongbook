package com.chenzixin.wrongbook.domain.update

/**
 * 更新通道（版本计划）。
 *
 * 三档，越往后收到的版本越新、也越可能出问题：
 *
 * | 通道 | 能收到 | 版本号长什么样 |
 * | --- | --- | --- |
 * | 正式版 STABLE | 只收 GitHub 上非 pre-release 的版本 | `0.13.0` |
 * | Beta | 收正式版 + Beta pre-release | `0.13.0` |
 * | Alpha | 全收，包括最新功能内测 | `0.13.A01` |
 *
 * 默认 **Beta**：这个仓库目前所有 0.x 版本都标成 pre-release，
 * 停在「正式版」通道会一直提示「只发布了测试版」。
 *
 * 纯函数，可在 JVM 上直接测。
 */
enum class UpdateChannel(
    val label: String,
    val summary: String,
) {
    STABLE(
        label = "正式版",
        summary = "只接收稳定版本，最省心",
    ),
    BETA(
        label = "Beta",
        summary = "接收测试版本；有新测试版一律弹窗提示",
    ),
    ALPHA(
        label = "Alpha",
        summary = "最新功能内测（0.13.A01 这种），可能不稳定",
    ),
    ;

    /** 这个通道是否接收 [version]（[isPrerelease] 来自 GitHub release 标记）。 */
    fun accepts(version: String, isPrerelease: Boolean): Boolean = when (this) {
        // 正式版通道：既要是非预发布，也不能是 Alpha 编号
        STABLE -> !isPrerelease && !VersionComparator.isAlpha(version)
        // Beta 通道：预发布可以，Alpha 编号不行
        BETA -> !VersionComparator.isAlpha(version)
        ALPHA -> true
    }

    companion object {
        /** 默认通道；老用户的旧 key 找不到时也落到这里。 */
        val DEFAULT: UpdateChannel = BETA

        fun fromKey(key: String?): UpdateChannel =
            entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
