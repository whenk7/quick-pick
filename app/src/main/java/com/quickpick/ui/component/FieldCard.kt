package com.quickpick.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.*

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
    val elevation = when (state) {
        FieldCardState.ACTIVE -> 6.dp
        else -> 2.dp
    }
    val offsetX = if (state == FieldCardState.ACTIVE) 2.dp else 0.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = offsetX)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state == FieldCardState.EMPTY) Surface.copy(alpha = 0.7f) else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state == FieldCardState.ACTIVE) Primary else TextPrimary,
                    fontWeight = if (state == FieldCardState.ACTIVE)
                        androidx.compose.ui.text.font.FontWeight.SemiBold
                    else androidx.compose.ui.text.font.FontWeight.Normal
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = when (state) {
                        FieldCardState.CONFIGURED -> "已配置"
                        FieldCardState.ACTIVE -> "编辑中"
                        FieldCardState.EMPTY -> "可选"
                    },
                    style = MaterialTheme.typography.bodySmall,
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
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = onPrefixChange,
                        placeholder = { Text("前缀", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = MaterialTheme.shapes.large   // 输入框 16dp 圆角（设计系统）
                    )
                    Text("→", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = suffix,
                        onValueChange = onSuffixChange,
                        placeholder = { Text("后缀", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = MaterialTheme.shapes.large
                    )
                }
            }
        }
        }
    }
}
