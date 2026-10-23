package com.quickpick.ui.screen

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextTertiary
import com.quickpick.ui.theme.effectiveSpec
import com.quickpick.ui.theme.supportedSpecs
import com.quickpick.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

private val THEME_MODES = listOf("SYSTEM" to "跟随系统", "LIGHT" to "浅色", "DARK" to "深色")
private val STYLE_LABELS = listOf(
    "TonalSpot" to "标准",
    "Neutral" to "中性",
    "Vibrant" to "鲜艳",
    "Expressive" to "表现力",
    "Rainbow" to "彩虹",
    "FruitSalad" to "果缤纷",
    "Monochrome" to "单色",
    "Fidelity" to "保真",
    "Content" to "内容还原",
)
private val SPEC_LABELS = listOf("Spec2021" to "Material 2021", "Spec2025" to "Expressive 2025")

/** 主题与外观页：改任何选项全局即时生效（即改即预览），见 spec 2026-10-22 */
@Composable
fun ThemeSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val seedColor by viewModel.seedColor.collectAsStateWithLifecycle()
    val paletteStyle by viewModel.paletteStyle.collectAsStateWithLifecycle()
    val colorSpec by viewModel.colorSpec.collectAsStateWithLifecycle()
    val glassBlur by viewModel.glassBlur.collectAsStateWithLifecycle()
    val glassLens by viewModel.glassLens.collectAsStateWithLifecycle()
    val glassHighlight by viewModel.glassHighlight.collectAsStateWithLifecycle()
    val glassAlpha by viewModel.glassAlpha.collectAsStateWithLifecycle()

    val monetSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val currentStyle = ThemePaletteStyle.entries.firstOrNull { it.name == paletteStyle }
        ?: ThemePaletteStyle.TonalSpot
    val supportedSpecs = currentStyle.supportedSpecs()

    Scaffold(
        topBar = {
            // 自定义顶栏要自己处理状态栏 inset（同规则编辑器先例）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MiuixTheme.textStyles.title4, color = TextPrimary)
                }
                Text(
                    text = "主题与外观",
                    style = MiuixTheme.textStyles.headline1,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SmallTitle(text = "外观")
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                cornerRadius = 16.dp
            ) {
                OverlaySpinnerPreference(
                    title = "主题模式",
                    items = THEME_MODES.map { DropdownItem(title = it.second) },
                    selectedIndex = THEME_MODES.indexOfFirst { it.first == themeMode }.coerceAtLeast(0),
                    onSelectedIndexChange = { viewModel.setThemeMode(THEME_MODES[it].first) }
                )
            }

            SmallTitle(text = "配色")
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                cornerRadius = 16.dp
            ) {
                // v1.1.7 §7 砍引擎：动态取色开关 + 默认/种子色 + 风格 + 规格
                SwitchPreference(
                    title = "动态取色",
                    summary = if (monetSupported) "从系统壁纸取色" else "需要 Android 12 及以上",
                    checked = dynamicColor && monetSupported,
                    enabled = monetSupported,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
                AnimatedVisibility(
                    visible = !dynamicColor || !monetSupported,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        // 「默认」选项行：miuix 内置板（seedColor == null）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSeedColor(null) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (seedColor == null) "●" else "○",
                                style = MiuixTheme.textStyles.body1,
                                color = if (seedColor == null) Primary else TextTertiary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "默认（miuix 内置配色）",
                                style = MiuixTheme.textStyles.main,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "种子色",
                            style = MiuixTheme.textStyles.main,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                        )
                        top.yukonga.miuix.kmp.basic.ColorPalette(
                            color = seedColor?.let { Color(it) } ?: Color(0xFF3482FF),
                            onColorChanged = { viewModel.setSeedColor(it.toArgb()) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                OverlaySpinnerPreference(
                    title = "调色板风格",
                    items = STYLE_LABELS.map { DropdownItem(title = it.second) },
                    selectedIndex = STYLE_LABELS.indexOfFirst { it.first == paletteStyle }.coerceAtLeast(0),
                    onSelectedIndexChange = { viewModel.setPaletteStyle(STYLE_LABELS[it].first) }
                )
                OverlaySpinnerPreference(
                    title = "色彩规格",
                    // 偏好与生效值分离：风格不支持所选规格时生效值回退，不改写用户选择
                    summary = if (supportedSpecs.size > 1) {
                        "配色规格版本"
                    } else {
                        "「${STYLE_LABELS.first { it.first == currentStyle.name }.second}」风格仅支持 Material 2021，已按 2021 生效"
                    },
                    items = SPEC_LABELS.map { DropdownItem(title = it.second) },
                    selectedIndex = SPEC_LABELS.indexOfFirst { it.first == colorSpec }.coerceAtLeast(0),
                    enabled = supportedSpecs.size > 1,
                    onSelectedIndexChange = { viewModel.setColorSpec(SPEC_LABELS[it].first) }
                )
            }

            SmallTitle(text = "玻璃底栏")
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                cornerRadius = 16.dp
            ) {
                SliderPreference(
                    title = "模糊强度",
                    value = glassBlur,
                    onValueChange = { viewModel.setGlassBlur(it) },
                    valueRange = 0f..1f
                )
                SliderPreference(
                    title = "透镜折射",
                    value = glassLens,
                    onValueChange = { viewModel.setGlassLens(it) },
                    valueRange = 0f..1f
                )
                SliderPreference(
                    title = "高光强度",
                    value = glassHighlight,
                    onValueChange = { viewModel.setGlassHighlight(it) },
                    valueRange = 0f..1f
                )
                SliderPreference(
                    title = "通透度",
                    summary = "越高栏越透明",
                    value = glassAlpha,
                    onValueChange = { viewModel.setGlassAlpha(it) },
                    valueRange = 0f..1f
                )
            }
        }
    }
}
