package com.chenzixin.wrongbook.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.entity.QuestionTagCrossRef
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO 行为测试（仪器测试，需要真机或模拟器）。
 *
 * 覆盖三个最容易出错的约束：
 * 1. 删除标签只解除关联，题目必须还在；
 * 2. 删除题目后交叉表记录会被外键级联清理；
 * 3. 多标签 AND 筛选只命中同时具备所有标签的题。
 */
@RunWith(AndroidJUnit4::class)
class QuestionDaoTest {

    private lateinit var db: WrongBookDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WrongBookDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    // in-memory 库默认不启用外键约束，必须显式打开才能验证 CASCADE
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            })
            .build()
        // 确保连接建立并执行 onOpen（Room 是懒打开）
        db.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun question(text: String) = QuestionEntity(
        imagePath = "questions/${text.hashCode()}.webp",
        thumbPath = "questions/${text.hashCode()}_thumb.webp",
        rawOcrText = text,
    )

    @Test
    fun 删除标签只解除关联不删题() = runTest {
        val questionId = db.questionDao().insert(question("二次函数求最值"))
        val tagId = db.tagDao().insert(TagEntity(name = "数学"))
        db.questionTagCrossRefDao().insertAll(listOf(QuestionTagCrossRef(questionId, tagId)))

        db.questionTagCrossRefDao().deleteByTagId(tagId)
        db.tagDao().deleteById(tagId)

        assertEquals(1, db.questionDao().count())
        assertTrue(db.questionTagCrossRefDao().tagIdsOf(questionId).isEmpty())
    }

    @Test
    fun 删除题目后关联记录被清理() = runTest {
        val questionId = db.questionDao().insert(question("数列求和"))
        val tagId = db.tagDao().insert(TagEntity(name = "数列"))
        db.questionTagCrossRefDao().insertAll(listOf(QuestionTagCrossRef(questionId, tagId)))

        db.questionDao().deleteById(questionId)

        assertEquals(0, db.questionDao().count())
        assertEquals(0, db.tagDao().countQuestions(tagId))
    }

    @Test
    fun 多标签AND筛选只命中同时具备所有标签的题() = runTest {
        val math = db.tagDao().insert(TagEntity(name = "数学"))
        val hard = db.tagDao().insert(TagEntity(name = "难题"))

        val both = db.questionDao().insert(question("既数学又难"))
        val onlyMath = db.questionDao().insert(question("只数学"))

        db.questionTagCrossRefDao().insertAll(
            listOf(
                QuestionTagCrossRef(both, math),
                QuestionTagCrossRef(both, hard),
                QuestionTagCrossRef(onlyMath, math),
            ),
        )

        val andResult = db.questionDao()
            .observeByTags(tagIds = listOf(math, hard), tagCount = 2)
            .first()
        assertEquals(listOf(both), andResult.map { it.id })

        val orResult = db.questionDao()
            .observeByTags(tagIds = listOf(math, hard), tagCount = 1)
            .first()
        assertEquals(setOf(both, onlyMath), orResult.map { it.id }.toSet())
    }
}
