package com.chenzixin.wrongbook.domain.tag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagRecommenderTest {

    @Test
    fun `数学题推荐数学与知识点`() {
        val text = "已知函数 f(x)=x^2+2x，求它的最小值和单调区间。"
        val tags = TagRecommender.suggest(text).map { it.name }
        assertTrue(tags.contains("数学"))
        assertTrue(tags.contains("函数"))
    }

    @Test
    fun `物理题命中电学`() {
        val text = "如图，电阻 R=10Ω，电压 U=5V，求电流大小。"
        val tags = TagRecommender.suggest(text).map { it.name }
        assertTrue(tags.contains("物理"))
        assertTrue(tags.contains("电学"))
    }

    @Test
    fun `化学题命中方程式`() {
        val text = "配平下列化学方程式：Fe + O2 -> Fe3O4"
        val tags = TagRecommender.suggest(text).map { it.name }
        assertTrue(tags.contains("化学"))
        assertTrue(tags.contains("化学方程式"))
    }

    @Test
    fun `空文本不推荐`() {
        assertTrue(TagRecommender.suggest("").isEmpty())
        assertTrue(TagRecommender.suggest("   ").isEmpty())
    }

    @Test
    fun `最多返回五个`() {
        val text = "函数 导数 三角函数 数列 概率 立体几何 电学 反应 细胞 文言文 sin cos tan"
        assertTrue(TagRecommender.suggest(text).size <= 5)
    }

    @Test
    fun `能识别学科`() {
        assertEquals("数学", TagRecommender.detectSubject("求函数 f(x) 的导数"))
        assertEquals("英语", TagRecommender.detectSubject("Choose the correct tense for the sentence"))
    }

    @Test
    fun `无关文本不返回学科`() {
        assertEquals(null, TagRecommender.detectSubject("今天天气不错"))
    }
}
