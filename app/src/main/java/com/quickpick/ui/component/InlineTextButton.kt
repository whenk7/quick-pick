package com.quickpick.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextTertiary
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton

/**
 * 行内文字动作按钮（复制/去开启/重新粘贴等）：透明底 + 彩色文字。
 * miuix TextButton 默认是灰底胶囊，行内高频小动作（每张卡片都有「复制」）用胶囊太重，
 * 透明底保持原 M3 TextButton 的轻量观感，也符合单强调色原则。
 */
@Composable
fun InlineTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = Primary
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            color = Color.Transparent,
            disabledColor = Color.Transparent,
            textColor = textColor,
            disabledTextColor = TextTertiary
        )
    )
}
