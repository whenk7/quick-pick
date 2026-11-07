package com.quickpick.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // 水平内边距收紧到 24dp：48dp 会吃掉近半可显示宽度，中文引导文案容易折成碎行
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // emoji 大字：原 M3 displayMedium(45sp) 无对应 miuix token，直接给字号
        Text(text = emoji, fontSize = 45.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MiuixTheme.textStyles.headline1,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MiuixTheme.textStyles.body2,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(20.dp))
            action()
        }
    }
}
