package com.chenzixin.wrongbook.ui.component

import android.graphics.Color
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

/**
 * Markdown + LaTeX 渲染。
 *
 * Flow 之外的唯一非 Compose 组件：Markwon 只在 TextView 上工作，
 * 用 AndroidView 桥接，避免自己实现一套 Markdown 解析。
 * 数学公式（$...$、$$...$$）由 JLatexMathPlugin 离线渲染，不联网。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Int = Color.BLACK,
    textSizeSp: Float = 16f,
) {
    val context = LocalContext.current
    val markwon = remember(context) {
        val density = context.resources.displayMetrics.scaledDensity
        Markwon.builder(context)
            .usePlugin(JLatexMathPlugin.create(textSizeSp * density))
            .build()
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
            markwon.setMarkdown(view, markdown)
        },
    )
}
