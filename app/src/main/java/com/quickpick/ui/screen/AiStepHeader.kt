package com.quickpick.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextTertiary
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 步骤标题（v1.1.11 AI 页重做）。
 *
 * 用「主色竖条 + 步骤名 + 一句引导」替代原先的 ①②③ 圆圈数字：
 * 圆圈数字在小字号下装饰感重、跟 miuix 的圆角线性风格也不搭。
 * 引导文案紧贴标题，替代掉原来顶部那块占一屏的流程说明卡。
 *
 * @param title   步骤名，如「审查短信样本」
 * @param hint    标题下方的一行引导，说明这一步该干什么
 * @param trailing 可选的右侧动作（如「补齐」）
 */
@Composable
fun AiStepHeader(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主色竖条：视觉上做步骤分隔，比数字角标克制
            Spacer(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MiuixTheme.textStyles.headline1,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hint,
            style = MiuixTheme.textStyles.body2,
            color = TextTertiary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 11.dp)
        )
    }
}
