package com.chenzixin.wrongbook.data.local.model

/** 掌握度分布：0~5 各有多少道题。 */
data class MasteryCount(
    val masteryLevel: Int,
    val count: Int,
)

/** 标签分布：某个标签下有多少道题。 */
data class TagCount(
    val name: String,
    val count: Int,
)
