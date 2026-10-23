package com.quickpick.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.quickpick.QuickPickApp
import com.quickpick.ui.screen.SettingsScreen
import com.quickpick.ui.screen.ThemeSettingsScreen
import com.quickpick.ui.theme.QuickPickTheme
import com.quickpick.ui.viewmodel.SettingsViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 点击交互复现测试（v1.1.2 真机闪退排查）：
 * 非混淆环境下点击「扫描范围」「主题与外观」，若崩溃则证明非 R8 问题而是真 bug。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SettingsClickTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun settingsVm(): SettingsViewModel =
        SettingsViewModel(ApplicationProvider.getApplicationContext())

    @Test
    fun `scan window spinner opens on click`() {
        composeRule.setContent {
            QuickPickTheme { SettingsScreen(settingsVm()) {} }
        }
        composeRule.onNodeWithText("扫描范围").performClick()
        composeRule.waitForIdle()
        // spinner 弹层应出现选项
        val found = composeRule.onAllNodesWithText("一月").fetchSemanticsNodes().isNotEmpty()
        if (!found) {
            // 诊断输出：打印整棵语义树再失败
            composeRule.onAllNodesWithText("一周")[0].assertIsDisplayed()
            throw AssertionError("spinner 弹层未出现「一月」选项")
        }
    }

    @Test
    fun `theme page opens and composes`() {
        // 主题页组合本身（navigation 的 crash 等价物：直接组合页面）
        composeRule.setContent {
            QuickPickTheme { ThemeSettingsScreen(settingsVm(), {}) }
        }
        composeRule.onNodeWithText("主题模式").assertIsDisplayed()
    }
}
