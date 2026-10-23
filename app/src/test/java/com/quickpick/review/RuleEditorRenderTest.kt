package com.quickpick.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import com.quickpick.ui.screen.RuleEditorScreen
import com.quickpick.ui.theme.QuickPickTheme
import com.quickpick.ui.viewmodel.RuleEditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 编辑器预览消失诊断（v1.1.7 §1.2，真机反馈「预览咋没了」）。
 * 三态判读：无短信→无预览（预期）；粘贴短信→有预览；老规则无样例→无预览（疑似根因）。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h2200dp")  // 拉长屏：LazyColumn 屏幕外项不组合，诊断须一屏装下
class RuleEditorRenderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun editorVm(): RuleEditorViewModel =
        RuleEditorViewModel(ApplicationProvider.getApplicationContext())

    private fun previewNodeCount(): Int =
        composeRule.onAllNodesWithText("实时预览").fetchSemanticsNodes().size

    @Test
    fun `preview hidden when no sms pasted`() {
        val vm = editorVm()
        composeRule.setContent { QuickPickTheme { RuleEditorScreen(vm) {} } }
        composeRule.waitForIdle()
        assertEquals(0, previewNodeCount())
    }

    @Test
    fun `preview appears after pasting sms`() {
        val vm = editorVm()
        composeRule.setContent { QuickPickTheme { RuleEditorScreen(vm) {} } }
        composeRule.waitForIdle()
        vm.updateSmsText("【顺丰速运】您的包裹已到绿洲家园，取件码：1234，请凭码取件。")
        vm.updateActiveFieldPrefix("取件码：")  // 默认激活字段=取件码
        vm.updateActiveFieldSuffix("，")       // E1 语义：suffix 不配则取到文末
        composeRule.waitForIdle()
        // 状态层：preview 必须算出来
        assertEquals("1234", vm.state.value.preview?.pickupCode)
        // UI 层：预览卡标题出现
        assertEquals(1, previewNodeCount())
    }

    @Test
    fun `preview hidden for legacy rule without sample sms`() {
        // 老规则（v1.1.0 前/AI 导入未带样例）无 sampleSms → 打开编辑器短信区空 → 无预览
        val vm = editorVm()
        composeRule.setContent { QuickPickTheme { RuleEditorScreen(vm) {} } }
        composeRule.waitForIdle()
        // 不粘贴短信 = 等价于加载无样例老规则后的状态
        assertEquals(0, previewNodeCount())
    }
}
