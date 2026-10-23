package com.quickpick.review

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.click
import com.quickpick.ui.component.BottomTab
import com.quickpick.ui.component.glass.LiquidGlassNavigationBar
import com.quickpick.ui.component.glass.rememberGlassBackdrop
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.QuickPickTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 底栏长按压测（v1.1.7 §11 + Review Focus #2）：detectTapGestures 与自定义 DampedDrag
 * 共存验证——长按快递 tab 必须触发 onTabLongClick，普通点击仍走切换，互不抢手势。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class NavBarLongPressTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var longPressed: BottomTab? = null
    private var clicked: BottomTab? = null

    private fun setBar() {
        longPressed = null
        clicked = null
        composeRule.setContent {
            QuickPickTheme {
                val backdrop = rememberGlassBackdrop(scrimColor = Background)
                Box(modifier = Modifier.fillMaxSize()) {
                    LiquidGlassNavigationBar(
                        currentTab = BottomTab.PACKAGES,
                        onTabClick = { clicked = it },
                        backdrop = backdrop,
                        onTabLongClick = { longPressed = it },
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `long press on packages tab fires long click`() {
        setBar()
        composeRule.onNodeWithText("快递").performTouchInput { longClick() }
        composeRule.waitForIdle()
        assertEquals(BottomTab.PACKAGES, longPressed)
    }

    @Test
    fun `normal click still switches tab`() {
        setBar()
        composeRule.onNodeWithText("设置").performTouchInput { click() }
        composeRule.waitForIdle()
        assertEquals(BottomTab.SETTINGS, clicked)
        assertNull("普通点击不得触发长按", longPressed)
    }
}
