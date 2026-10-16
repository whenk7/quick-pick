package com.quickpick.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.Danger
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.Surface

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    dangerConfirm: Boolean = false,   // 删除类操作传 true，确认键用危险色
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // 显式白色容器 + 20dp 圆角：不依赖 colorScheme 槽位，杜绝默认淡紫泄漏
        shape = RoundedCornerShape(20.dp),
        containerColor = Surface,
        tonalElevation = 2.dp,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = if (dangerConfirm) Danger else Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}
