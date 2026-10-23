package com.quickpick.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.core.app.ApplicationProvider
import com.quickpick.QuickPickApp
import com.quickpick.ui.navigation.AppNavigation
import com.quickpick.ui.theme.QuickPickTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 子页返回恢复 tab（v1.1.8，真机反馈：主题页/编辑器返回后落在快递 tab 而非设置 tab）。
 * 根因：currentTab 是 remember——NavHost 前进时主页组合销毁，返回重建重置为 PACKAGES。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class MainTabRestoreTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `back from theme page restores settings tab`() {
        runBlocking {
            ApplicationProvider.getApplicationContext<QuickPickApp>()
                .settingsStore.setOnboardingDone()
        }
        composeRule.setContent { QuickPickTheme { AppNavigation() } }
        composeRule.waitForIdle()

        // 切到设置 tab → 进主题页
        composeRule.onNodeWithText("设置").performTouchInput { click() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("扫描范围").assertIsDisplayed()
        composeRule.onNodeWithText("主题与外观").performTouchInput { click() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("主题模式").assertIsDisplayed()

        // 返回：应落回设置 tab，不是快递 tab
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("扫描范围").assertIsDisplayed()
        assert(!composeRule.activity.isFinishing) { "返回子页不得退出 App" }
    }
}
