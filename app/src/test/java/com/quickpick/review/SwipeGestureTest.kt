package com.quickpick.review

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.quickpick.ui.component.SwipeToReveal
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.QuickPickTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 左滑已取件手势（v1.1.7 §4.3）：过阈值触发、未过回弹不触发、垂直/斜滑不触发。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SwipeGestureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var fired = false

    private val rootW: Float get() = composeRule.onRoot().fetchSemanticsNode().size.width.toFloat()
    private val rootH: Float get() = composeRule.onRoot().fetchSemanticsNode().size.height.toFloat()

    private fun setSwipeContent() {
        fired = false
        composeRule.setContent {
            QuickPickTheme {
                SwipeToReveal(
                    actionLabel = "✓ 已取件",
                    actionColor = Primary,
                    onTrigger = { fired = true }
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp))
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `swipe past threshold triggers`() {
        setSwipeContent()
        val w = rootW
        composeRule.onRoot().performTouchInput {
            swipe(centerRight, Offset(centerRight.x - w * 0.6f, centerRight.y), 300)
        }
        composeRule.waitForIdle()
        assertTrue("左滑过 35% 阈值应触发", fired)
    }

    @Test
    fun `swipe under threshold does not trigger`() {
        setSwipeContent()
        val w = rootW
        composeRule.onRoot().performTouchInput {
            swipe(centerRight, Offset(centerRight.x - w * 0.2f, centerRight.y), 300)
        }
        composeRule.waitForIdle()
        assertFalse("左滑 20% 未过阈值不得触发", fired)
    }

    @Test
    fun `vertical swipe does not trigger`() {
        setSwipeContent()
        val h = rootH
        composeRule.onRoot().performTouchInput {
            swipe(center, Offset(center.x, center.y - h * 0.4f), 300)
        }
        composeRule.waitForIdle()
        assertFalse("垂直滑动不得触发", fired)
    }

    @Test
    fun `diagonal swipe mostly vertical does not trigger`() {
        setSwipeContent()
        val w = rootW
        val h = rootH
        composeRule.onRoot().performTouchInput {
            // dx 10%、dy 40%：斜向但主轴垂直
            swipe(center, Offset(center.x - w * 0.1f, center.y - h * 0.4f), 300)
        }
        composeRule.waitForIdle()
        assertFalse("斜向（主轴垂直）滑动不得触发", fired)
    }

    @Test
    fun `right swipe does nothing`() {
        setSwipeContent()
        val w = rootW
        composeRule.onRoot().performTouchInput {
            swipe(centerLeft, Offset(centerLeft.x + w * 0.5f, centerLeft.y), 300)
        }
        composeRule.waitForIdle()
        assertFalse("右滑无动作（用户裁决）", fired)
    }
}
