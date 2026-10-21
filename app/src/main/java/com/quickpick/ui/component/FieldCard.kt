package com.quickpick.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.*
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

enum class FieldCardState { CONFIGURED, ACTIVE, EMPTY }

@Composable
fun FieldCard(
    fieldName: String,
    dotColor: androidx.compose.ui.graphics.Color,
    state: FieldCardState,
    prefix: String,
    suffix: String,
    onPrefixChange: (String) -> Unit,
    onSuffixChange: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 选中抬高阴影（原 M3 cardElevation → Modifier.shadow）
    val elevation = when (state) {
        FieldCardState.ACTIVE -> 6.dp
        else -> 2.dp
    }
    val offsetX = if (state == FieldCardState.ACTIVE) 2.dp else 0.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = offsetX)
            .shadow(elevation, RoundedCornerShape(16.dp)),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = if (state == FieldCardState.EMPTY) Surface.copy(alpha = 0.7f) else Surface
        ),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // 左侧强调色线：选中态/已配置态显示，空字段不显示
            if (state != FieldCardState.EMPTY) {
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp, top = 14.dp, bottom = 14.dp)
                        .fillMaxHeight()
                        .width(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Primary)
                )
            }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fieldName,
                    style = MiuixTheme.textStyles.body1,
                    color = if (state == FieldCardState.ACTIVE) Primary else TextPrimary,
                    fontWeight = if (state == FieldCardState.ACTIVE)
                        FontWeight.SemiBold
                    else FontWeight.Normal
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = when (state) {
                        FieldCardState.CONFIGURED -> "已配置"
                        FieldCardState.ACTIVE -> "编辑中"
                        FieldCardState.EMPTY -> "可选"
                    },
                    style = MiuixTheme.textStyles.body2,
                    color = when (state) {
                        FieldCardState.CONFIGURED -> Done
                        FieldCardState.ACTIVE -> Primary
                        FieldCardState.EMPTY -> TextTertiary
                    }
                )
            }

            if (state != FieldCardState.EMPTY || prefix.isNotEmpty() || suffix.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = prefix,
                        onValueChange = onPrefixChange,
                        label = "前缀",
                        useLabelAsPlaceholder = true,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MiuixTheme.textStyles.body1,
                        colors = InputFieldColors
                    )
                    Text("→", color = TextTertiary, style = MiuixTheme.textStyles.body1)
                    TextField(
                        value = suffix,
                        onValueChange = onSuffixChange,
                        label = "后缀",
                        useLabelAsPlaceholder = true,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MiuixTheme.textStyles.body1,
                        colors = InputFieldColors
                    )
                }
            }
        }
        }
    }
}
