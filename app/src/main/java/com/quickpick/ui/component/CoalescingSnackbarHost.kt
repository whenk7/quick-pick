package com.quickpick.ui.component

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickpick.util.UiNotifier
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Snackbar
import top.yukonga.miuix.kmp.basic.SnackbarData
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarVisuals

/**
 * 合并式轻提示宿主（v1.1.7 §3 用户裁决）：同一时间只有一条；新消息顶掉旧的——
 * 原地换文案、计时重启。miuix SnackbarHostState 是多条并存模型（entries 列表），
 * 不满足「禁止新建」语义，故自管理单条状态：nonce 保证同文案连发也重新计时。
 */
@Composable
fun CoalescingSnackbarHost(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<UiNotifier.Toast?>(null) }

    LaunchedEffect(Unit) {
        UiNotifier.messages.collect { toast ->
            current = toast.copy(nonce = System.nanoTime())
        }
    }
    // 5 秒自动消失；nonce 变化（含同文案重发）重启计时
    LaunchedEffect(current?.nonce) {
        if (current != null) {
            delay(5000)
            current = null
        }
    }

    current?.let { toast ->
        val data = remember(toast.nonce) {
            object : SnackbarData {
                override val visuals = SnackbarVisuals(
                    message = toast.message,
                    actionLabel = toast.actionLabel,
                    withDismissAction = false,
                    duration = SnackbarDuration.Custom(5000)
                )

                override suspend fun dismiss() {}
                override suspend fun performAction() {}
            }
        }
        // action 点击（「我知道了」）= 关闭：替换 data 的 performAction 目标
        val wired = remember(toast.nonce) {
            object : SnackbarData by data {
                override suspend fun performAction() { current = null }
            }
        }
        Snackbar(data = wired, modifier = modifier.navigationBarsPadding().padding(bottom = 96.dp))
    }
}
