package com.quickpick.util

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 跨页轻提示总线：规则保存/删除等动作发生在子页，提示要显示在主页
 * （Snackbar 宿主在 MainScreen）。5 秒自动消失，带「我知道了」可点关闭。
 */
object UiNotifier {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun notify(message: String) {
        messages.tryEmit(message)
    }
}
