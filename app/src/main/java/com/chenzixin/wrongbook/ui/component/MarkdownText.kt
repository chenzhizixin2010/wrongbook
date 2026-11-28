package com.chenzixin.wrongbook.ui.component

import android.text.util.Linkify
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin

/**
 * Markdown + LaTeX 渲染。
 *
 * Flow 之外的唯一非 Compose 组件：Markwon 只在 TextView 上工作，
 * 用 AndroidView 桥接，避免自己实现一套 Markdown 解析。
 * 数学公式（`$...$`、`$$...$$`）由 JLatexMathPlugin 离线渲染，不联网。
 *
 * 两个必须同时在位的插件（踩过坑，见 DECISIONS D60）：
 * - `JLatexMathPlugin` 本来只渲染块级公式，`inlinesEnabled(true)` 才管 `$...$`；
 * - 但一旦打开行内，它内部会 `registry.require(MarkwonInlineParserPlugin.class)`，
 *   **缺了 inline-parser 插件就在 build() 时 NPE** —— 表现为「点开任意题目就闪退」。
 *
 * 兜底：Markwon 构建失败时退化成纯文本，绝不让整页崩掉。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Int,
    textSizeSp: Float = 16f,
) {
    val context = LocalContext.current
    val markwon: Markwon? = remember(context, textSizeSp) {
        runCatching {
            val density = context.resources.displayMetrics.scaledDensity
            Markwon.builder(context)
                // 行内公式（$...$）的前置条件，必须排在 latex 插件前面
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(
                    JLatexMathPlugin.create(textSizeSp * density) { builder ->
                        builder.inlinesEnabled(true)
                        builder.blocksEnabled(true)
                    },
                )
                .build()
        }.getOrNull()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextIsSelectable(true)
                setLineSpacing(0f, 1.15f)
                Linkify.addLinks(this, Linkify.WEB_URLS)
            }
        },
        update = { view ->
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            view.setTextColor(textColor)
            val text = com.chenzixin.wrongbook.core.LatexSanitizer.sanitize(markdown)
            val renderer = markwon
            if (renderer != null) {
                renderer.setMarkdown(view, text)
            } else {
                // 插件装配失败（例如依赖缺件）：至少把原文显示出来，别整页崩
                view.text = text
            }
        },
    )
}
