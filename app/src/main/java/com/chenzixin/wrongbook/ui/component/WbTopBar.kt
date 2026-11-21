package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ChevronBackward

/** 二级页面顶栏：返回箭头 + 居中标题 + 右侧操作。 */
@Composable
fun WbTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = wbColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            CupertinoIcon(
                imageVector = CupertinoIcons.Outlined.ChevronBackward,
                contentDescription = "返回",
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onBack)
                    .padding(4.dp),
                tint = colors.tint,
            )
        } else {
            Spacer(Modifier.width(28.dp))
        }
        CupertinoText(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.label,
        )
        actions()
    }
}

/** 页面内的大标题（iOS Large Title）。 */
@Composable
fun WbLargeTitle(text: String, modifier: Modifier = Modifier) {
    val colors = wbColors()
    Column(modifier = modifier) {
        CupertinoText(
            text = text,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = colors.label,
        )
    }
}
