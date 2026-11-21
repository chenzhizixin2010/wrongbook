package com.chenzixin.wrongbook.domain.review

/**
 * 简化版 SM-2 复习调度（纯函数，不依赖 Android，可直接 JVM 单测）。
 *
 * 规则（刻意做得比原版 SM-2 简单，因为错题本用户不会每天做几十道题）：
 * - 答对：掌握度 +1（封顶 5），下次复习间隔按掌握度取 1 / 2 / 4 / 7 / 15 天；
 *   already 5 级再答对 → 30 天（视为已掌握，只做低频回访）。
 * - 答错：掌握度归 0，10 分钟后重做（当天就应该再见一次）。
 *
 * 所有时间都是毫秒时间戳，便于存 Room。
 */
object ReviewScheduler {

    const val MAX_LEVEL = 5
    const val MIN_LEVEL = 0

    /** 10 分钟：答错后的重做间隔 */
    const val WRONG_RETRY_MILLIS: Long = 10L * 60 * 1000

    private const val DAY_MILLIS: Long = 24L * 60 * 60 * 1000

    /** 下标 = 新的掌握度（1~4），5 级单独按 30 天处理 */
    private val LEVEL_INTERVAL_DAYS = intArrayOf(1, 1, 2, 4, 7)

    data class Plan(
        val masteryLevel: Int,
        val nextReviewAt: Long,
        val intervalMillis: Long,
    )

    /**
     * @param currentLevel 当前掌握度，任意 int 都会被夹到 0..5（脏数据兜底）
     * @param correct      本次是否答对
     * @param now          当前时间戳（毫秒）
     */
    fun schedule(currentLevel: Int, correct: Boolean, now: Long): Plan {
        val level = currentLevel.coerceIn(MIN_LEVEL, MAX_LEVEL)

        if (!correct) {
            return Plan(
                masteryLevel = MIN_LEVEL,
                nextReviewAt = now + WRONG_RETRY_MILLIS,
                intervalMillis = WRONG_RETRY_MILLIS,
            )
        }

        val newLevel = (level + 1).coerceAtMost(MAX_LEVEL)
        val intervalMillis = intervalDaysFor(newLevel) * DAY_MILLIS
        return Plan(
            masteryLevel = newLevel,
            nextReviewAt = now + intervalMillis,
            intervalMillis = intervalMillis,
        )
    }

    /** 掌握度对应的间隔天数（供测试与 UI 文案复用）。 */
    fun intervalDaysFor(level: Int): Int {
        val l = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        return if (l >= MAX_LEVEL) 30 else LEVEL_INTERVAL_DAYS[l]
    }

    /** 是否到了复习时间；nextReviewAt 为 null 表示还没排期（新录入的题）。 */
    fun isDue(nextReviewAt: Long?, now: Long): Boolean =
        nextReviewAt != null && nextReviewAt <= now
}
