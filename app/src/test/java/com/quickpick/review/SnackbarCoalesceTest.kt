package com.quickpick.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.quickpick.ui.component.CoalescingSnackbarHost
import com.quickpick.ui.theme.QuickPickTheme
import com.quickpick.util.UiNotifier
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 轻提示合并（v1.1.7 §3 用户裁决）：同一时间只有一条；新消息顶掉旧的（原地换文案）。
 * miuix SnackbarHostState 是多条并存模型（entries 列表），故自管理单条状态。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SnackbarCoalesceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `second notify replaces the first in place`() {
        composeRule.setContent { QuickPickTheme { CoalescingSnackbarHost() } }
        composeRule.runOnIdle { UiNotifier.notify("第一条") }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("第一条").assertIsDisplayed()

        composeRule.runOnIdle { UiNotifier.notify("第二条") }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("第二条").assertIsDisplayed()
        composeRule.onNodeWithText("第一条").assertDoesNotExist()
    }

    @Test
    fun `action label shown and dismisses on click`() {
        composeRule.setContent { QuickPickTheme { CoalescingSnackbarHost() } }
        composeRule.runOnIdle { UiNotifier.notify("已删除「测试」，请首页下拉刷新") }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("我知道了").assertIsDisplayed()
        composeRule.onNodeWithText("我知道了").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("已删除「测试」，请首页下拉刷新").assertIsNotDisplayed()
    }
}
