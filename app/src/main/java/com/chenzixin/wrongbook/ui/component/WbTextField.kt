package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * HyperOS 风格输入框：浅色 surfaceContainer 填充、无描边、无渐变。
 * 用 BasicTextField 包一层是为了保留现有的 String + placeholder + leadingIcon API。
 */
@Composable
fun WbTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val md3e = style == UiStyle.MD3E
    val c = MiuixTheme.colorScheme
    val wb = wbColors()
    val body = MiuixTheme.textStyles.body1
    if (md3e) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            textStyle = M3MaterialTheme.typography.bodyLarge,
            placeholder = {
                M3Text(
                    text = placeholder,
                    style = M3MaterialTheme.typography.bodyLarge,
                    color = M3MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = if (leadingIcon != null) {
                {
                    WbIcon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = M3MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            trailingIcon = trailing,
            shape = M3MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = M3MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = M3MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContainerColor = M3MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
        )
        return
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = TextStyle(
            color = if (ios) wb.label else c.onSurface,
            fontSize = body.fontSize,
            fontFamily = body.fontFamily,
            lineHeight = body.lineHeight,
        ),
        cursorBrush = SolidColor(if (ios) wb.accent else c.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (v090) 18.dp else if (md3e) 16.dp else if (ios) 10.dp else 12.dp))
                    .background(
                        if (v090) {
                            wb.glassFill
                        } else if (md3e) {
                            wb.groupedBackground
                        } else if (ios) {
                            wb.cardBackground
                        } else {
                            if (enabled) c.secondaryContainer else c.surfaceContainerHigh
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .heightIn(min = 22.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (leadingIcon != null) {
                    WbIcon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (v090 || md3e) wb.accent else if (ios) wb.secondaryLabel else c.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        WbText(
                            text = placeholder,
                            fontSize = 16.sp,
                            color = if (ios) wb.secondaryLabel else c.onSurfaceVariantSummary,
                            maxLines = if (singleLine) 1 else 2,
                        )
                    }
                    innerTextField()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        },
    )
}
