package com.chenzixin.wrongbook.domain.generate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionQualityTest {

    private fun item(
        stem: String,
        answer: String = "C",
        analysis: String = "把已知量代入公式，先算中间量再合并，注意单位换算与符号方向，最后代回验算。",
    ) = PracticeItem(index = 1, stem = stem, answer = answer, analysis = analysis)

    private val choiceStem = """
        已知函数 f(x)=x^2+1，则 f(2) 的值为
        A. 3
        B. 4
        C. 5
        D. 6
    """.trimIndent()

    @Test
    fun `完整的题能过闸门`() {
        val report = QuestionQuality.check(item(choiceStem))
        assertTrue(report.issues.toString(), report.accepted)
        assertEquals(100, report.score)
    }

    @Test
    fun `题号会被剥掉`() {
        val report = QuestionQuality.check(item("12. 已知 f(x)=x^2，求 f(3)"))
        assertTrue(report.sanitized.stem.startsWith("已知"))
    }

    @Test
    fun `依赖插图的题直接判死`() {
        val report = QuestionQuality.check(item("如图所示，物块沿斜面下滑，求加速度大小"))
        assertFalse(report.accepted)
        assertTrue(report.issues.any { it.code == "figure_missing" })
    }

    @Test
    fun `答案写略等于没给`() {
        listOf("略", "见解析", "无").forEach { placeholder ->
            val report = QuestionQuality.check(item("求下列函数的定义域", answer = placeholder))
            assertFalse("$placeholder 应该被拒", report.accepted)
            assertTrue(report.issues.any { it.code == "answer_placeholder" })
        }
    }

    @Test
    fun `缺解析要被拒`() {
        val report = QuestionQuality.check(item(choiceStem, analysis = ""))
        assertFalse(report.accepted)
        assertTrue(report.issues.any { it.code == "analysis_missing" })
    }

    @Test
    fun `和原题一模一样算重复`() {
        val report = QuestionQuality.check(
            item(choiceStem),
            sources = listOf(choiceStem),
        )
        assertFalse(report.accepted)
        assertTrue(report.issues.any { it.code == "duplicate_source" })
    }

    @Test
    fun `和本次另一道题重复`() {
        val other = item(choiceStem)
        val report = QuestionQuality.check(item(choiceStem), accepted = listOf(other))
        assertFalse(report.accepted)
        assertTrue(report.issues.any { it.code == "duplicate_sibling" })
    }

    @Test
    fun `只换数字的变式不算重复`() {
        val source = "已知函数 f(x)=x^2+1，则 f(2) 的值为多少？请写出计算过程。"
        val variant = "已知函数 f(x)=x^2+3，则 f(4) 的值为多少？请写出计算过程。"
        val report = QuestionQuality.check(item(variant), sources = listOf(source))
        assertFalse(report.issues.any { it.code == "duplicate_source" })
    }

    @Test
    fun `答案不在选项里要拒`() {
        val report = QuestionQuality.check(item(choiceStem, answer = "E"))
        assertFalse(report.accepted)
        assertTrue(report.issues.any { it.code == "choice_answer_invalid" })
    }

    @Test
    fun `答案写成一句话只扣分不判死`() {
        val report = QuestionQuality.check(
            item(choiceStem, answer = "把二代入得五，选 C。"),
        )
        assertTrue(report.accepted)
        assertTrue(report.issues.any { it.code == "choice_answer_unclear" })
    }

    @Test
    fun `选项只有两个算残缺`() {
        val report = QuestionQuality.check(
            item("下列说法正确的是\nA. 甲\nB. 乙", answer = "A"),
        )
        assertTrue(report.issues.any { it.code == "options_incomplete" })
        // 判断题常见两个选项，允许放行
        assertTrue(report.accepted)
    }

    @Test
    fun `选项只有一个说明格式崩了`() {
        val report = QuestionQuality.check(item("下列说法正确的是\nA. 甲", answer = "A"))
        assertFalse(report.accepted)
    }

    @Test
    fun `题干截断要拒`() {
        val report = QuestionQuality.check(item("已知函数 f(x)=x^2…"))
        assertFalse(report.accepted)
        assertTrue(report.issues.any { it.code == "stem_truncated" })
    }

    @Test
    fun `一行里塞四个选项也能识别`() {
        val single = "已知 f(x)=x^2+1，则 f(2)=  A. 3  B. 4  C. 5  D. 6"
        val report = QuestionQuality.check(item(single))
        assertTrue(report.accepted)
        // 换行后 B/C/D 会各占一行，打印时不会从选项中间折断
        assertTrue(report.sanitized.stem.contains("\nB."))
        assertTrue(report.sanitized.stem.contains("\nD."))
    }

    @Test
    fun `filter 按需要的数量收工`() {
        val items = (1..5).map { n ->
            item(choiceStem.replace("f(2)", "f(${n + 1})").replace("C. 5", "C. ${n + 10}"))
        }
        val outcome = QuestionQuality.filter(items, needed = 3)
        assertEquals(3, outcome.accepted.size)
        assertEquals(0, outcome.rejectedCount)
    }

    @Test
    fun `filter 记录被拒原因`() {
        val outcome = QuestionQuality.filter(
            listOf(
                item("如图所示，求物块加速度"),
                item("求函数的定义域", answer = "略"),
                item(choiceStem),
            ),
            needed = 5,
        )
        assertEquals(1, outcome.accepted.size)
        assertEquals(2, outcome.rejectedCount)
        assertTrue(outcome.rejected.all { it.second.isNotEmpty() })
    }

    @Test
    fun `相似度对完全相同的文本为 1`() {
        assertEquals(1.0, QuestionQuality.similarity("甲乙丙丁", "甲乙丙丁"), 0.0001)
        assertTrue(QuestionQuality.similarity("甲乙丙丁", "完全不同") < 0.2)
    }

    @Test
    fun `shape 把数字抹成井号`() {
        assertEquals("质量为#的物体，速度#", QuestionQuality.shape("质量为3的物体，速度5"))
        assertEquals(
            QuestionQuality.shape("质量为3的物体，速度5"),
            QuestionQuality.shape("质量为7的物体，速度9"),
        )
    }

    @Test
    fun `数字提取`() {
        assertEquals(listOf("0.5", "12"), QuestionQuality.numbers("浓度 0.5 mol/L，体积 12 mL"))
    }
}
