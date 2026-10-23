package com.quickpick.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.quickpick.QuickPickApp
import com.quickpick.data.db.entity.MatchRuleEntity
import com.quickpick.ui.component.JsonPreviewDialog
import com.quickpick.ui.screen.RuleListScreen
import com.quickpick.ui.theme.QuickPickTheme
import com.quickpick.ui.viewmodel.RuleListViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 规则页 `</>` 图标闪退（v1.1.2 真机报告）的复核：
 * 根因是 miuix 弹层缺 NavigationEventDispatcherOwner（v1.1.3 已在主题根部修复，
 * 同根的设置页 spinner 路径有 SettingsClickTest 绿例证明）。
 * 本类守住两点：弹窗本体直渲不崩 + 图标有可定位的无障碍语义。
 * （compose-ui-test 的语义动作点击 miuix IconButton 在此 harness 下不触发——
 * 删/看两个图标同样不触发，属 Robolectric 原始触摸注入局限，非产品缺陷。）
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RuleListClickTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `rule cards expose accessible json and delete icons`() {
        val app = ApplicationProvider.getApplicationContext<QuickPickApp>()
        runBlocking {
            app.database.matchRuleDao().insert(
                MatchRuleEntity(name = "顺丰速运", isPreset = true, codePrefix = "取件码：", codeSuffix = "，")
            )
        }
        composeRule.setContent {
            QuickPickTheme {
                RuleListScreen(
                    viewModel = RuleListViewModel(app),
                    onAddRule = {}, onEditRule = {}, onExportRule = {}, onOpenAi = {}
                )
            }
        }
        composeRule.waitForIdle()
        val jsonIcons = composeRule.onAllNodes(hasContentDescription("查看规则 JSON"))
            .fetchSemanticsNodes()
        val deleteIcons = composeRule.onAllNodes(hasContentDescription("删除规则"))
            .fetchSemanticsNodes()
        assert(jsonIcons.size == 3 && deleteIcons.size == 3) {
            "三张卡各应有 JSON/删除两个可定位图标，实际 json=${jsonIcons.size} delete=${deleteIcons.size}"
        }
    }

    @Test
    fun `json preview dialog renders when show true directly`() {
        // 弹窗本体直渲：设备端崩溃点是弹层本体还是布线，此测试切开两者
        composeRule.setContent {
            QuickPickTheme {
                top.yukonga.miuix.kmp.basic.Scaffold {
                    JsonPreviewDialog(
                        show = true,
                        json = "{\"name\":\"顺丰\",\"fields\":{\"pickupCode\":{\"prefix\":\"取件码：\",\"suffix\":\"，\"}}}",
                        onDismiss = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/review/json_dialog_direct.png")
        val found = composeRule.onAllNodesWithText("取件码：", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        assert(found) { "JsonPreviewDialog show=true 未渲染出 JSON 内容" }
    }

    @Test
    fun `import and export dialogs render`() {
        composeRule.setContent {
            QuickPickTheme {
                top.yukonga.miuix.kmp.basic.Scaffold {
                    com.quickpick.ui.component.ImportRulesDialog(
                        show = true,
                        onPickFile = {}, onParse = { null }, onConfirmImport = {},
                        onDismiss = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("导入规则", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty().let { assert(it) { "导入弹窗未渲染" } }
        // v1.1.7 §10：竖排「本地文件」按钮存在（多行 Text 的换行符在语义树里保留）
        composeRule.onAllNodesWithText("本\n地\n文\n件", useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty().let { assert(it) { "竖排本地文件按钮未渲染" } }
    }

    @Test
    fun `export dialog renders`() {
        composeRule.setContent {
            QuickPickTheme {
                top.yukonga.miuix.kmp.basic.Scaffold {
                    com.quickpick.ui.component.ExportRulesDialog(
                        show = true, onCopy = {}, onSaveFile = {}, onDismiss = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/review/dialog_export.png")
        composeRule.onAllNodesWithText("复制到剪贴板", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty().let { assert(it) { "导出弹窗未渲染" } }
    }

    @Test
    fun `sms detail dialog renders with sender time and body`() {
        composeRule.setContent {
            QuickPickTheme {
                top.yukonga.miuix.kmp.basic.Scaffold {
                    com.quickpick.ui.component.SmsDetailDialog(
                        show = true,
                        record = com.quickpick.domain.model.PackageRecord(
                            companyName = "圆通速递", pickupCode = "44651320",
                            smsBody = "【圆通速递】取货码44651320，您有圆通包裹已到绿洲家园西南门袋鼠柜A01",
                            smsTimestamp = 1700000000000L, smsSender = "10684110405676900"
                        ),
                        onDismiss = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/review/dialog_sms_detail.png")
        composeRule.onAllNodesWithText("10684110405676900", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty().let { assert(it) { "短信原文弹窗缺发件人" } }
        composeRule.onAllNodesWithText("取货码44651320", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty().let { assert(it) { "短信原文弹窗缺正文" } }
    }
}
