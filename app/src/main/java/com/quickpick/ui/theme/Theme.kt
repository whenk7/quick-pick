package com.quickpick.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.quickpick.QuickPickApp
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * 主题引擎（双路径，模式参考 InstallerX-Revived）：
 * - FIXED：miuix 内置默认明暗板（lightColorScheme/darkColorScheme 不传参）
 * - MONET：MonetSystem/Light/Dark；keyColor = null 走壁纸/系统调色板（动态取色开且 API≥31），
 *   否则用用户选的种子色从 materialkolor 生成
 * 偏好与生效值分离：风格不支持所选色彩规格时生效值回退 2021，不改写存储的偏好。
 */

/** 色彩规格对调色板风格的支持表（miuix colorsFromSeed 内部规则，提前在 UI 层暴露用于置灰） */
fun ThemePaletteStyle.supportedSpecs(): List<ThemeColorSpec> = when (this) {
    ThemePaletteStyle.TonalSpot, ThemePaletteStyle.Neutral,
    ThemePaletteStyle.Vibrant, ThemePaletteStyle.Expressive ->
        listOf(ThemeColorSpec.Spec2021, ThemeColorSpec.Spec2025)

    else -> listOf(ThemeColorSpec.Spec2021)
}

/** 解析生效规格：不支持时回退，但不改写用户存的选择 */
fun ThemePaletteStyle.effectiveSpec(preferred: ThemeColorSpec): ThemeColorSpec =
    preferred.takeIf { it in supportedSpecs() } ?: ThemeColorSpec.Spec2021

@Composable
fun QuickPickTheme(content: @Composable () -> Unit) {
    // 主题偏好从 DataStore 读；异步首帧用默认值，接受首帧闪烁（裁决，见迁移计划 Review Focus #5）
    val settingsStore = (LocalContext.current.applicationContext as QuickPickApp).settingsStore
    val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
    val themeEngine by settingsStore.themeEngine.collectAsStateWithLifecycle(initialValue = "FIXED")
    val dynamicColor by settingsStore.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
    val seedColor by settingsStore.seedColor.collectAsStateWithLifecycle(initialValue = 0xFF3482FF.toInt())
    val paletteStyle by settingsStore.paletteStyle.collectAsStateWithLifecycle(initialValue = "TonalSpot")
    val colorSpec by settingsStore.colorSpec.collectAsStateWithLifecycle(initialValue = "Spec2025")

    val style = ThemePaletteStyle.entries.firstOrNull { it.name == paletteStyle }
        ?: ThemePaletteStyle.TonalSpot
    val preferredSpec = ThemeColorSpec.entries.firstOrNull { it.name == colorSpec }
        ?: ThemeColorSpec.Spec2025

    // ThemeController 属性无公开 setter，偏好变化时重建 controller 生效
    val controller = remember(themeMode, themeEngine, dynamicColor, seedColor, paletteStyle, colorSpec) {
        if (themeEngine == "MONET") {
            ThemeController(
                colorSchemeMode = when (themeMode) {
                    "LIGHT" -> ColorSchemeMode.MonetLight
                    "DARK" -> ColorSchemeMode.MonetDark
                    else -> ColorSchemeMode.MonetSystem
                },
                // keyColor=null → platformDynamicColors（壁纸/系统调色板）；否则种子色生成
                keyColor = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    null
                } else {
                    Color(seedColor)
                },
                paletteStyle = style,
                colorSpec = style.effectiveSpec(preferredSpec)
            )
        } else {
            ThemeController(
                colorSchemeMode = when (themeMode) {
                    "LIGHT" -> ColorSchemeMode.Light
                    "DARK" -> ColorSchemeMode.Dark
                    else -> ColorSchemeMode.System
                }
            )
        }
    }

    // miuix 窗口弹层（WindowSpinner/ListPopup 等）内的 NavigationBackHandler 要求
    // LocalNavigationEventDispatcherOwner；activity-compose 1.11 不提供，缺了点击即崩
    // （v1.1.2 设置页闪退根因，点击复现测试钉在 SettingsClickTest）
    val navEventOwner = rememberNavigationEventDispatcherOwner(parent = null)
    MiuixTheme(controller = controller, textStyles = AppTypography) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navEventOwner) {
            content()
        }
    }
}
