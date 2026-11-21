package com.chenzixin.wrongbook.domain.update

/**
 * 版本号比较（纯函数，可单测）。
 *
 * 只比较数字段：`v0.3.1` == `0.3.1`，`0.3.0-beta` == `0.3.0`（预发布标识忽略），
 * 段数不同时短的一方补 0：`1.0` == `1.0.0`。
 */
object VersionComparator {

    fun parse(version: String): List<Int> {
        val cleaned = version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
        if (cleaned.isEmpty()) return emptyList()
        return cleaned.split('.').map { it.toIntOrNull() ?: 0 }
    }

    /** candidate 是否比 current 新。 */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate)
        val b = parse(current)
        if (a.isEmpty()) return false
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** 规范化成 `x.y.z` 展示；解析失败时原样返回。 */
    fun normalize(version: String): String {
        val parts = parse(version)
        if (parts.isEmpty()) return version
        return (0..2).joinToString(".") { parts.getOrElse(it) { 0 }.toString() }
    }
}
