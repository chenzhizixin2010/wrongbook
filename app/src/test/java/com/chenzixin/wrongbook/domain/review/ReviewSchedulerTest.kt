package com.chenzixin.wrongbook.domain.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_700_000_000_000L

    @Test
    fun `答对时掌握度加一并按间隔排期`() {
        val plan = ReviewScheduler.schedule(currentLevel = 0, correct = true, now = now)

        assertEquals(1, plan.masteryLevel)
        assertEquals(now + 1 * day, plan.nextReviewAt)
        assertEquals(1 * day, plan.intervalMillis)
    }

    @Test
    fun `三级答对升到四级 间隔七天`() {
        val plan = ReviewScheduler.schedule(currentLevel = 3, correct = true, now = now)

        assertEquals(4, plan.masteryLevel)
        assertEquals(now + 7 * day, plan.nextReviewAt)
    }

    @Test
    fun `满级答对保持五级 间隔三十天`() {
        val plan = ReviewScheduler.schedule(currentLevel = ReviewScheduler.MAX_LEVEL, correct = true, now = now)

        assertEquals(ReviewScheduler.MAX_LEVEL, plan.masteryLevel)
        assertEquals(now + 30 * day, plan.nextReviewAt)
    }

    @Test
    fun `答错归零 十分钟后重做`() {
        val plan = ReviewScheduler.schedule(currentLevel = 4, correct = false, now = now)

        assertEquals(0, plan.masteryLevel)
        assertEquals(now + ReviewScheduler.WRONG_RETRY_MILLIS, plan.nextReviewAt)
        assertEquals(ReviewScheduler.WRONG_RETRY_MILLIS, plan.intervalMillis)
    }

    @Test
    fun `脏的掌握度会被夹到合法区间`() {
        assertEquals(1, ReviewScheduler.schedule(-10, correct = true, now).masteryLevel)
        assertEquals(5, ReviewScheduler.schedule(99, correct = true, now).masteryLevel)
        assertEquals(30, ReviewScheduler.intervalDaysFor(99))
        assertEquals(1, ReviewScheduler.intervalDaysFor(-3))
    }

    @Test
    fun `到期判断只认已排期的题`() {
        assertFalse(ReviewScheduler.isDue(nextReviewAt = null, now = now))
        assertFalse(ReviewScheduler.isDue(nextReviewAt = now + 1, now = now))
        assertTrue(ReviewScheduler.isDue(nextReviewAt = now, now = now))
        assertTrue(ReviewScheduler.isDue(nextReviewAt = now - 1, now = now))
    }
}
