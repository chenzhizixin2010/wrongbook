package com.chenzixin.wrongbook.domain.update

/**
 * 版本号比较（纯函数，可单测）。
 *
 * 支持三种写法：
 * - 稳定版：`0.12.0` / `v0.12.0`
 * - 预发布：`0.12.0-beta`（后缀不含 Alpha 编号时按同版本处理，避免反复提示）
 * - Alpha 内测：`0.13.A01`（第三段是 `A` + 编号，也可写成 `0.13.0-alpha.1`）
 *
 * 比较规则：
 * 1. 先按数字段逐位比较，短的一方补 0：`1.0` == `1.0.0`；
 * 2. 数字段全相同时，**正式版 > Alpha**，Alpha 之间比编号：`0.13.0` > `0.13.A02` > `0.13.A01`。
 *
 * 也就是说 Alpha 永远排在它所对应的正式版之前，用户从 Alpha 升到正式版能收到更新提示。
 */
object VersionComparator {

    /** 段内形态：`A01` / `a1`。 */
    private val ALPHA_SEGMENT = Regex("^[Aa](\\d{1,3})$")

    /** 预发布后缀形态：`alpha.2` / `a.3`。 */
    private val ALPHA_PRERELEASE = Regex("^[Aa](?:lpha)?\\.?(\\d{1,3})$")

    /** 解析结果：数字段 + Alpha 编号（null 表示正式版或普通预发布）。 */
    data class Version(
        val segments: List<Int>,
        val alpha: Int? = null,
    ) {
        val isEmpty: Boolean get() = segments.isEmpty()
    }

    fun of(version: String): Version {
        val text = version.trim().removePrefix("v").removePrefix("V")
        if (text.isEmpty()) return Version(emptyList())

        // 构建元数据（+build5）不参与比较
        val core = text.substringBefore('+')
        var main = core
        var alpha: Int? = null

        // 预发布后缀：0.13.0-alpha.2 / 0.13.0-A2
        val dash = core.indexOf('-')
        if (dash >= 0) {
            main = core.substring(0, dash)
            val pre = core.substring(dash + 1).trim()
            alpha = ALPHA_PRERELEASE.matchEntire(pre)?.groupValues?.get(1)?.toIntOrNull()
        }

        val segments = mutableListOf<Int>()
        main.split('.').forEach { raw ->
            val seg = raw.trim()
            if (seg.isEmpty()) return@forEach
            val alphaSeg = ALPHA_SEGMENT.matchEntire(seg)
            if (alphaSeg != null) {
                alpha = alphaSeg.groupValues[1].toIntOrNull()
                return@forEach
            }
            segments += seg.toIntOrNull() ?: 0
        }
        return Version(segments, alpha)
    }

    /** 只取数字段（`0.13.A01` → `[0, 13]`），老调用方沿用这个口径。 */
    fun parse(version: String): List<Int> = of(version).segments

    /** 是否为 Alpha 内测版本（`0.13.A01`）。 */
    fun isAlpha(version: String): Boolean = of(version).alpha != null

    fun alphaOf(version: String): Int? = of(version).alpha

    /** candidate 是否比 current 新。 */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = of(candidate)
        if (a.isEmpty) return false
        return compare(a, of(current)) > 0
    }

    private fun compare(a: Version, b: Version): Int {
        val size = maxOf(a.segments.size, b.segments.size)
        for (i in 0 until size) {
            val x = a.segments.getOrElse(i) { 0 }
            val y = b.segments.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return when {
            a.alpha == null && b.alpha == null -> 0
            // 正式版替代同号 Alpha
            a.alpha == null -> 1
            b.alpha == null -> -1
            else -> a.alpha.compareTo(b.alpha)
        }
    }

    /**
     * 规范化成展示用文本；解析失败时原样返回。
     *
     * - 普通版本补足三段：`v1.0` → `1.0.0`
     * - Alpha 保留 `.A0X`：`0.13.A01`、`0.13.0-alpha.2` → `0.13.0.A02`
     */
    fun normalize(version: String): String {
        val v = of(version)
        if (v.isEmpty) return version
        val base = if (v.alpha != null && v.segments.size <= 2) {
            v.segments.joinToString(".")
        } else {
            (0..2).joinToString(".") { v.segments.getOrElse(it) { 0 }.toString() }
        }
        return if (v.alpha != null) "$base.A%02d".format(v.alpha) else base
    }
}
