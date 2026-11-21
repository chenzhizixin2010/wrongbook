package com.chenzixin.wrongbook.domain.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateLevelClassifierTest {

    @Test
    fun `只有修订号变化是小版本`() {
        assertEquals(UpdateLevel.PATCH, UpdateLevelClassifier.classify("0.4.0", "0.4.1"))
        assertEquals(UpdateLevel.PATCH, UpdateLevelClassifier.classify("0.4.1", "0.4.9"))
        assertEquals(UpdateLevel.PATCH, UpdateLevelClassifier.classify("v0.4.0", "0.4.2"))
    }

    @Test
    fun `次版本号变化是大版本`() {
        assertEquals(UpdateLevel.MAJOR, UpdateLevelClassifier.classify("0.4.0", "0.5.0"))
        assertEquals(UpdateLevel.MAJOR, UpdateLevelClassifier.classify("0.4.3", "0.5.0"))
    }

    @Test
    fun `主版本号变化是大版本`() {
        assertEquals(UpdateLevel.MAJOR, UpdateLevelClassifier.classify("0.4.9", "1.0.0"))
        assertEquals(UpdateLevel.MAJOR, UpdateLevelClassifier.classify("1.2.3", "2.0.0"))
    }

    @Test
    fun `相同或更旧没有更新`() {
        assertEquals(UpdateLevel.NONE, UpdateLevelClassifier.classify("0.4.1", "0.4.1"))
        assertEquals(UpdateLevel.NONE, UpdateLevelClassifier.classify("0.4.1", "0.4.0"))
        assertEquals(UpdateLevel.NONE, UpdateLevelClassifier.classify("1.0.0", "0.9.9"))
        assertEquals(UpdateLevel.NONE, UpdateLevelClassifier.classify("0.4", "0.4.0"))
    }

    @Test
    fun `预发布后缀不影响判定`() {
        assertEquals(UpdateLevel.PATCH, UpdateLevelClassifier.classify("0.4.0", "0.4.1-beta"))
        assertEquals(UpdateLevel.NONE, UpdateLevelClassifier.classify("0.4.0", "0.4.0-beta"))
    }

    @Test
    fun `文案能区分大小版本`() {
        assertEquals("小版本更新 0.4.1", UpdateLevelClassifier.describe(UpdateLevel.PATCH, "0.4.1"))
        assertEquals(
            "大版本更新 0.5.0，建议升级",
            UpdateLevelClassifier.describe(UpdateLevel.MAJOR, "0.5.0"),
        )
        assertEquals("已是最新版本", UpdateLevelClassifier.describe(UpdateLevel.NONE, "0.4.0"))
    }
}
