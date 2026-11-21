package com.chenzixin.wrongbook.domain.update

/**
 * 更新级别：决定要不要在主页弹窗。
 *
 * 规则（按你的要求）：
 * - **大版本**：主版本号或次版本号变化（0.4.0 → 0.5.0、0.4.x → 1.0.0）→ 主页弹「建议更新」
 * - **小版本**：只有修订号变化（0.4.0 → 0.4.1）→ 不弹窗，只在「检查更新」页可见
 * - 不是更新（相同或更旧）→ 什么都不做
 */
enum class UpdateLevel {
    NONE,
    PATCH,
    MAJOR,
}

object UpdateLevelClassifier {

    fun classify(current: String, latest: String): UpdateLevel {
        if (!VersionComparator.isNewer(latest, current)) return UpdateLevel.NONE

        val a = VersionComparator.parse(current)
        val b = VersionComparator.parse(latest)
        val curMajor = a.getOrElse(0) { 0 }
        val curMinor = a.getOrElse(1) { 0 }
        val newMajor = b.getOrElse(0) { 0 }
        val newMinor = b.getOrElse(1) { 0 }

        return if (newMajor != curMajor || newMinor != curMinor) {
            UpdateLevel.MAJOR
        } else {
            UpdateLevel.PATCH
        }
    }

    fun describe(level: UpdateLevel, version: String): String = when (level) {
        UpdateLevel.MAJOR -> "大版本更新 $version，建议升级"
        UpdateLevel.PATCH -> "小版本更新 $version"
        UpdateLevel.NONE -> "已是最新版本"
    }
}
