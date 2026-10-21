package com.quickpick.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.Danger
import com.quickpick.ui.theme.TextSecondary
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 确认弹窗（miuix OverlayDialog 实现）。
 * 依赖 miuix Scaffold 的 popupHost——调用页面必须处于 miuix Scaffold 内（全站页面都满足）。
 * 调用方式保持条件组合（if (visible) ConfirmDialog(...)）：离场时 OverlayDialog 的
 * DialogState 由 popupHost 侧托管，退出动画照常播放。
 */
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
    OverlayDialog(
        show = true,
        title = title,
        summary = message,
        onDismissRequest = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    textColor = TextSecondary,
                    disabledTextColor = TextSecondary
                )
            )
            TextButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = if (dangerConfirm) {
                    ButtonDefaults.textButtonColors(
                        color = Danger,
                        disabledColor = Danger.copy(alpha = 0.4f),
                        textColor = Color.White,
                        disabledTextColor = Color.White
                    )
                } else {
                    ButtonDefaults.textButtonColorsPrimary()
                }
            )
        }
    }
}
