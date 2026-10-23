package com.quickpick.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.quickpick.QuickPickApp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.component.DayGroupHeader
import com.quickpick.ui.component.EmptyState
import com.quickpick.ui.component.PackageCard
import com.quickpick.ui.component.TokenChip
import com.quickpick.ui.component.TokenState
import com.quickpick.ui.screen.RuleListScreen
import com.quickpick.ui.screen.SettingsScreen
import com.quickpick.ui.screen.ThemeSettingsScreen
import com.quickpick.ui.theme.AppTypography
import com.quickpick.ui.theme.Background
import com.quickpick.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * 界面渲染审查：无头环境把关键界面渲成 PNG（app/build/outputs/roborazzi/），
 * 供无显示器环境人工看图验收。不进 APK。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h900dp")
class VisualReviewTest {

    private fun fixed(mode: ColorSchemeMode) = ThemeController(colorSchemeMode = mode)

    private fun monet(
        mode: ColorSchemeMode,
        seed: Color,
        style: ThemePaletteStyle = ThemePaletteStyle.TonalSpot,
        spec: ThemeColorSpec = ThemeColorSpec.Spec2025
    ) = ThemeController(colorSchemeMode = mode, keyColor = seed, paletteStyle = style, colorSpec = spec)

    private val sampleUnpicked = PackageRecord(
        id = 1, companyName = "圆通速递", trackingNumber = "YT0073828884344",
        pickupCode = "44651320", address = "绿洲家园西南门袋鼠柜A01",
        smsBody = "body", smsTimestamp = System.currentTimeMillis()
    )
    private val samplePicked = PackageRecord(
        id = 2, companyName = "菜鸟驿站", trackingNumber = null,
        pickupCode = "4-5-0877", address = "绿洲家园西门店",
        smsBody = "body", smsTimestamp = System.currentTimeMillis(), isPickedUp = true
    )

    /** 组件合集板：卡片双态 + 词块三态 + 分组头 + 空状态 + 按钮 */
    @Composable
    private fun Board() {
        Column(
            modifier = Modifier
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DayGroupHeader(
                dateLabel = "今天", dateMs = 0L, collapsed = false,
                hasUnpicked = true, onToggleCollapse = {}, onMarkDayPickedUp = {}
            )
            PackageCard(record = sampleUnpicked, onTogglePickedUp = {})
            PackageCard(record = samplePicked, onTogglePickedUp = {})
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TokenChip(text = "取件码", state = TokenState.NORMAL, onClick = {})
                TokenChip(text = "44651320", state = TokenState.MATCHED, onClick = {})
                TokenChip(text = "选中态", state = TokenState.SELECTED, onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, colors = ButtonDefaults.buttonColorsPrimary()) {
                    Text("主按钮")
                }
                TextButton(text = "文本按钮", onClick = {})
            }
            EmptyState(emoji = "📦", title = "还没有快递消息", description = "坐等包裹到来～")
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    private fun board(name: String, controller: ThemeController) =
        captureRoboImage("build/outputs/review/$name.png") {
            MiuixTheme(controller = controller, textStyles = AppTypography) { Board() }
        }

    // ---- 组件板 × 主题矩阵 ----

    @Test
    fun `board fixed light`() = board("board_fixed_light", fixed(ColorSchemeMode.Light))

    @Test
    fun `board fixed dark`() = board("board_fixed_dark", fixed(ColorSchemeMode.Dark))

    @Test
    fun `board monet blue light`() =
        board("board_monet_blue_light", monet(ColorSchemeMode.MonetLight, Color(0xFF3482FF)))

    @Test
    fun `board monet pink vibrant dark`() =
        board(
            "board_monet_pink_vibrant_dark",
            monet(ColorSchemeMode.MonetDark, Color(0xFFE91E63), ThemePaletteStyle.Vibrant)
        )

    @Test
    fun `board monet green expressive light`() =
        board(
            "board_monet_green_expressive_light",
            monet(ColorSchemeMode.MonetLight, Color(0xFF43A047), ThemePaletteStyle.Expressive)
        )

    // ---- 页面级 ----

    private fun settingsVm(engine: String? = null): SettingsViewModel {
        val app = ApplicationProvider.getApplicationContext<QuickPickApp>()
        if (engine != null) {
            runBlocking {
                app.settingsStore.setThemeEngine(engine)
                app.settingsStore.setDynamicColor(false)   // 让种子色网格也入镜
            }
        }
        return SettingsViewModel(app)
    }

    @Test
    fun `settings page fixed light`() =
        captureRoboImage("build/outputs/review/page_settings.png") {
            MiuixTheme(controller = fixed(ColorSchemeMode.Light), textStyles = AppTypography) {
                SettingsScreen(settingsViewModel = settingsVm(), onOpenTheme = {})
            }
        }

    @Test
    fun `theme page fixed light`() =
        captureRoboImage("build/outputs/review/page_theme_fixed.png") {
            MiuixTheme(controller = fixed(ColorSchemeMode.Light), textStyles = AppTypography) {
                ThemeSettingsScreen(viewModel = settingsVm(), onBack = {})
            }
        }

    @Test
    fun `theme page monet expanded`() =
        captureRoboImage("build/outputs/review/page_theme_monet.png") {
            MiuixTheme(controller = fixed(ColorSchemeMode.Light), textStyles = AppTypography) {
                ThemeSettingsScreen(viewModel = settingsVm("MONET"), onBack = {})
            }
        }

    /** AI 生成规则页（无短信沙盒 → 空样本态；流程说明卡 + 提示词 + 粘贴区应完整） */
    @Test
    fun `ai generate page empty state`() =
        captureRoboImage("build/outputs/review/page_ai_generate.png") {
            MiuixTheme(controller = fixed(ColorSchemeMode.Light), textStyles = AppTypography) {
                com.quickpick.ui.screen.AiPromptScreen(
                    viewModel = com.quickpick.ui.viewmodel.AiPromptViewModel(
                        ApplicationProvider.getApplicationContext()
                    ),
                    onBack = {}
                )
            }
        }

    /** 地址簿页（v1.1.7 §6） */
    @Test
    fun `address alias page`() =
        captureRoboImage("build/outputs/review/page_address_alias.png") {
            MiuixTheme(controller = fixed(ColorSchemeMode.Light), textStyles = AppTypography) {
                com.quickpick.ui.screen.AddressAliasScreen(
                    viewModel = com.quickpick.ui.viewmodel.AddressAliasViewModel(
                        ApplicationProvider.getApplicationContext()
                    ),
                    onBack = {}
                )
            }
        }

    /** 规则页（带新的三按钮行） */
    @Test
    fun `rules page with workshop buttons`() =
        captureRoboImage("build/outputs/review/page_rules.png") {
            val app = ApplicationProvider.getApplicationContext<QuickPickApp>()
            MiuixTheme(controller = fixed(ColorSchemeMode.Light), textStyles = AppTypography) {
                RuleListScreen(
                    viewModel = com.quickpick.ui.viewmodel.RuleListViewModel(app),
                    onAddRule = {}, onEditRule = {}, onExportRule = {}, onOpenAi = {}
                )
            }
        }

    /** 词块三态专项（v1.1.1 暗色修复的目视证据）：MATCHED 命中态在暗色下必须可读 */
    @Test
    fun `chips tristate dark`() =
        captureRoboImage("build/outputs/review/chips_dark.png") {
            MiuixTheme(controller = fixed(ColorSchemeMode.Dark), textStyles = AppTypography) {
                Row(
                    modifier = Modifier.background(Background).padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TokenChip(text = "普通", state = TokenState.NORMAL, onClick = {})
                    TokenChip(text = "已命中 44651", state = TokenState.MATCHED, onClick = {})
                    TokenChip(text = "选中", state = TokenState.SELECTED, onClick = {})
                }
            }
        }

    @Test
    fun `chips tristate light`() =
        captureRoboImage("build/outputs/review/chips_light.png") {
            MiuixTheme(controller = fixed(ColorSchemeMode.Light), textStyles = AppTypography) {
                Row(
                    modifier = Modifier.background(Background).padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TokenChip(text = "普通", state = TokenState.NORMAL, onClick = {})
                    TokenChip(text = "已命中 44651", state = TokenState.MATCHED, onClick = {})
                    TokenChip(text = "选中", state = TokenState.SELECTED, onClick = {})
                }
            }
        }
}
