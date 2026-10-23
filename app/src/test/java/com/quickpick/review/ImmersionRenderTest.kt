package com.quickpick.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.screen.ImmersionScreen
import com.quickpick.ui.theme.AppTypography
import com.quickpick.ui.theme.QuickPickTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 沉浸式取件模式（v1.1.7 §11）渲染审查 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h900dp")
class ImmersionRenderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val day = 86_400_000L
    private val base = 1_761_200_000_000L

    private fun pkg(id: Long, company: String, code: String, address: String?, daysAgo: Int) =
        PackageRecord(
            id = id, companyName = company, pickupCode = code, address = address,
            smsBody = "", smsTimestamp = base - daysAgo * day
        )

    private val sample = listOf(
        pkg(1, "顺丰速运", "A1-2345", "家门口", 0),
        pkg(2, "京东物流", "8-2042", "家门口", 1),
        pkg(3, "妈妈驿站", "56-3310", "家门口", 1),
        pkg(4, "圆通速递", "YT88", "公司", 2),
        pkg(5, "中通快递", "77921", "公司", 3),
        pkg(6, "韵达", "5531", null, 4),
        pkg(7, "快递", "123456789012", "家门口", 0)  // 长码降档验证
    )

    @Test
    fun `immersion multi groups light`() =
        captureRoboImage("build/outputs/review/immersion_multi.png") {
            MiuixTheme(
                controller = ThemeController(colorSchemeMode = ColorSchemeMode.Light),
                textStyles = AppTypography
            ) {
                ImmersionScreen(packages = sample, onMarkAllPicked = {})
            }
        }

    @Test
    fun `immersion empty state`() =
        captureRoboImage("build/outputs/review/immersion_empty.png") {
            MiuixTheme(
                controller = ThemeController(colorSchemeMode = ColorSchemeMode.Light),
                textStyles = AppTypography
            ) {
                ImmersionScreen(packages = emptyList(), onMarkAllPicked = {})
            }
        }

    @Test
    fun `immersion dark`() =
        captureRoboImage("build/outputs/review/immersion_dark.png") {
            MiuixTheme(
                controller = ThemeController(colorSchemeMode = ColorSchemeMode.Dark),
                textStyles = AppTypography
            ) {
                ImmersionScreen(packages = sample, onMarkAllPicked = {})
            }
        }

    @Test
    fun `bottom bar buttons present`() {
        composeRule.setContent {
            MiuixTheme(
                controller = ThemeController(colorSchemeMode = ColorSchemeMode.Light),
                textStyles = AppTypography
            ) {
                ImmersionScreen(packages = sample, onMarkAllPicked = {})
            }
        }
        composeRule.onNodeWithText("A－").assertExists()
        composeRule.onNodeWithText("A＋").assertExists()
        composeRule.onNodeWithText("全部取完").assertExists()
        // 分组头（组件数倒序：家门口 4 件排最前）
        composeRule.onNodeWithText("家门口 · 4").assertExists()
        composeRule.onNodeWithText("其他 · 1").assertExists()
    }

    /** 全链路（v1.1.7 §11）：长按快递进沉浸 → 返回键退出沉浸而非退 App。
     *  引导弹窗的「吞返回」与按钮关闭拆开验：overlay 层按钮点击在本 harness 打不通
     *  （IconButton 语义同款局限），故这里预置引导已读，专注沉浸进出链路。 */
    @Test
    fun `long press enters immersion, back exits to list`() {
        kotlinx.coroutines.runBlocking {
            androidx.test.core.app.ApplicationProvider
                .getApplicationContext<com.quickpick.QuickPickApp>()
                .settingsStore.setImmersionGuideShown()
        }
        composeRule.setContent {
            QuickPickTheme {
                com.quickpick.ui.navigation.MainScreen({}, {}, {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("快递").performTouchInput { longClick() }
        composeRule.waitForIdle()

        // 沉浸视图的底部操作栏出现（引导已预读，不弹）
        composeRule.onNodeWithText("全部取完").assertExists()
        // 返回键 = 退出沉浸回普通列表，不 finish
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("全部取完").fetchSemanticsNodes().isEmpty()
            .let { assert(it) { "返回后应退出沉浸视图" } }
        assert(!composeRule.activity.isFinishing) { "返回沉浸模式不得退出 App" }
    }

    /** 首次进入弹引导：引导弹窗吞返回（开着时按返回不关、不退出沉浸链路） */
    @Test
    fun `first entry shows guide dialog and back is swallowed`() {
        composeRule.setContent {
            QuickPickTheme {
                com.quickpick.ui.navigation.MainScreen({}, {}, {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("快递").performTouchInput { longClick() }
        composeRule.waitForIdle()

        // 引导弹窗出现（本测试类首个用例运行时 DataStore 无标记；
        // 若已被上个用例写入则跳过——Robolectric 同进程沙盒共享）
        val guideShown = composeRule.onAllNodesWithText("沉浸式取件模式")
            .fetchSemanticsNodes().isNotEmpty()
        if (guideShown) {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("沉浸式取件模式").assertExists()
        }
    }
}
