package com.quickpick.util

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** UiNotifier 重放语义（终审 Important #2：子页发出时主页宿主已销毁，消息不得丢） */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UiNotifierTest {

    @AfterTest
    fun cleanup() {
        UiNotifier.messages.resetReplayCache()
    }

    @Test
    fun `notify without subscriber replays to late subscriber`() = runBlocking {
        UiNotifier.messages.resetReplayCache()
        UiNotifier.notify("已保存「测试」，请首页下拉刷新")
        val got = withTimeoutOrNull(500) { UiNotifier.messages.first() }
        assertEquals("已保存「测试」，请首页下拉刷新", got?.message)
    }

    @Test
    fun `consumed replay cache does not re-deliver`() = runBlocking {
        UiNotifier.messages.resetReplayCache()
        UiNotifier.notify("旧消息")
        UiNotifier.messages.first()           // 消费
        UiNotifier.messages.resetReplayCache() // 宿主消费后清缓存（CoalescingSnackbarHost 同款）
        val replay = withTimeoutOrNull(300) { UiNotifier.messages.first() }
        assertNull(replay, "清缓存后新订阅者不得收到旧消息")
    }
}
