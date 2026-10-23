package com.quickpick.ui.component

import androidx.activity.compose.BackHandler
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
 * 调用方式：**常驻组合**（不要用 if (visible) 条件包裹），显隐交给 [visible]——
 * OverlayDialog 内部靠 show 状态驱动进出场动画；条件组合离场会跳过退出动画（瞬间消失）。
 *
 * 返回语义（v1.1.7 §2 用户裁决）：miuix OverlayDialog 不消费系统返回键，这里自己拦——
 * 普通弹窗（swallowBack=false）：返回 = 关闭；裁决弹窗（swallowBack=true，如导入同名冲突）：
 * 返回无效、点外部也不关（onDismissRequest 吞掉），必须点按钮明确裁决。
 */
@Composable
fun ConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    dangerConfirm: Boolean = false,   // 删除类操作传 true，确认键用危险色
    swallowBack: Boolean = false,     // 裁决弹窗传 true：返回/点外部无效
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible) { if (!swallowBack) onDismiss() }
    OverlayDialog(
        show = visible,
        title = title,
        summary = message,
        onDismissRequest = if (swallowBack) ({}) else onDismiss
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
