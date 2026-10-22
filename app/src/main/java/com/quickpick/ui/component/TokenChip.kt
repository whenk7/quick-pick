package com.quickpick.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.*
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class TokenState { NORMAL, MATCHED, SELECTED }

/** 分词块三态：普通无色 / 已被提取字段命中=绿框 / 选中=主色填充白字 */
@Composable
fun TokenChip(
    text: String,
    state: TokenState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when (state) {
        TokenState.NORMAL -> InputBackground
        TokenState.MATCHED -> PrimarySoft   // 语义 token 明暗自适应；硬编码亮底在暗色下文字对比度不可读
        TokenState.SELECTED -> Primary
    }
    val border = when (state) {
        TokenState.NORMAL -> Color.Transparent
        TokenState.MATCHED -> Primary     // 主题色边框表示已被规则匹配
        TokenState.SELECTED -> Primary
    }
    val fg = when (state) {
        TokenState.SELECTED -> Color.White
        else -> TextPrimary
    }

    Text(
        text = text,
        style = MiuixTheme.textStyles.body2,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
