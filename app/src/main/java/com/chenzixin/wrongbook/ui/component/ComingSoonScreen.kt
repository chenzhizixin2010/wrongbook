package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.component.WbText

/** 占位页：功能未实现时给出明确说明，而不是点了没反应。 */
@Composable
fun ComingSoonScreen(title: String, hint: String, onBack: () -> Unit) {
    val colors = wbColors()
    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(title = title, onBack = onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WbText(text = hint, fontSize = 15.sp, color = colors.secondaryLabel)
                WbText(
                    text = "（该功能在后续阶段实现）",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp,
                    color = colors.secondaryLabel,
                )
            }
        }
    }
}
