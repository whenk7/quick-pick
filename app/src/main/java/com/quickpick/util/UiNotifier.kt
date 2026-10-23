package com.quickpick.util

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 跨页轻提示总线：规则保存/删除等动作发生在子页，提示显示在主页
 * （宿主是 MainScreen 的 CoalescingSnackbarHost——同一时间只有一条，新顶旧）。
 * 5 秒自动消失；actionLabel 非空时显示动作按钮（如「我知道了」），点击即关。
 */
object UiNotifier {

    /** @param actionLabel 动作按钮文案；null = 纯文本无按钮（如「再按一次退出」） */
    data class Toast(val message: String, val actionLabel: String? = "我知道了", val nonce: Long = 0L)

    /**
     * replay=1：子页（编辑器保存）发出时主页宿主已被 NavHost 销毁，无订阅者的发射会丢弃
     * （终审 Important #2——保存提示曾因此不可达）。重放给迟到订阅者，消费后立即清缓存。
     */
    val messages = MutableSharedFlow<Toast>(replay = 1, extraBufferCapacity = 8)

    fun notify(message: String, actionLabel: String? = "我知道了") {
        messages.tryEmit(Toast(message, actionLabel))
    }
}
