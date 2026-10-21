package com.quickpick.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.QuickPickApp
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// ============ miuix 豆绿色板（固定配色模式；Monet 开启时被动态色板整体接管） ============

private val QuickPickLightColors = miuixLightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryVariant = Primary,
    onPrimaryVariant = Color.White,
    error = Danger,
    onError = Color.White,
    disabledPrimary = PrimaryLight,
    disabledOnPrimary = Color.White,
    disabledPrimaryButton = PrimaryLight,
    disabledOnPrimaryButton = Color.White,
    primaryContainer = LightPrimarySoft,
    onPrimaryContainer = Primary,
    secondary = Primary,
    onSecondary = Color.White,
    secondaryContainer = LightPrimarySoft,     // 底部导航选中胶囊
    onSecondaryContainer = Primary,
    tertiaryContainer = LightPrimarySoft,
    onTertiaryContainer = Primary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    onBackgroundVariant = LightTextSecondary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightInputBackground,     // 输入框/浅灰底
    onSurfaceSecondary = LightTextSecondary,
    onSurfaceVariantSummary = LightTextTertiary,
    onSurfaceVariantActions = LightTextTertiary,
    disabledOnSurface = LightTextTertiary,
    surfaceContainer = LightSurface,
    onSurfaceContainer = LightTextPrimary,
    onSurfaceContainerVariant = LightTextSecondary,
    surfaceContainerHigh = LightSurfaceDone,   // 已取件置灰底
    onSurfaceContainerHigh = LightTextSecondary,
    surfaceContainerHighest = LightSurfaceDone,
    onSurfaceContainerHighest = LightTextPrimary,
    outline = LightDivider,
    dividerLine = LightDivider
)

private val QuickPickDarkColors = miuixDarkColorScheme(
    primary = Primary,                         // 暗色下豆绿保持原色
    onPrimary = Color.White,
    primaryVariant = Primary,
    onPrimaryVariant = Color.White,
    error = Danger,
    onError = Color.White,
    disabledPrimary = DarkPrimarySoft,
    disabledOnPrimary = DarkTextTertiary,
    disabledPrimaryButton = DarkPrimarySoft,
    disabledOnPrimaryButton = DarkTextTertiary,
    primaryContainer = DarkPrimarySoft,
    onPrimaryContainer = PrimaryLight,         // 暗色容器上用浅豆绿保证对比度
    secondary = Primary,
    onSecondary = Color.White,
    secondaryContainer = DarkPrimarySoft,
    onSecondaryContainer = PrimaryLight,
    tertiaryContainer = DarkPrimarySoft,
    onTertiaryContainer = PrimaryLight,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    onBackgroundVariant = DarkTextSecondary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkInputBackground,
    onSurfaceSecondary = DarkTextSecondary,
    onSurfaceVariantSummary = DarkTextTertiary,
    onSurfaceVariantActions = DarkTextTertiary,
    disabledOnSurface = DarkTextTertiary,
    surfaceContainer = DarkSurface,
    onSurfaceContainer = DarkTextPrimary,
    onSurfaceContainerVariant = DarkTextSecondary,
    surfaceContainerHigh = DarkSurfaceDone,
    onSurfaceContainerHigh = DarkTextSecondary,
    surfaceContainerHighest = DarkSurfaceDone,
    onSurfaceContainerHighest = DarkTextPrimary,
    outline = DarkDivider,
    dividerLine = DarkDivider
)

// ============ M3 桥接色板（T7 迁移完现有 M3 组件前防默认紫色泄漏，之后删除） ============

// 覆盖完整槽位，杜绝 M3 默认紫色（弹窗容器 surfaceContainerHigh 等）泄漏
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = LightPrimarySoft,
    onPrimaryContainer = Primary,
    secondary = Primary,
    onSecondary = Color.White,
    secondaryContainer = LightPrimarySoft,   // 底部导航选中胶囊
    onSecondaryContainer = Primary,
    tertiary = PrimaryLight,
    onTertiary = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightInputBackground,   // 输入框/浅灰底
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface,     // AlertDialog 默认容器
    surfaceContainerHighest = LightSurfaceDone,
    error = Danger,                          // 删除操作专用
    onError = Color.White,
    outline = LightDivider,
    outlineVariant = LightDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = DarkPrimarySoft,
    onPrimaryContainer = PrimaryLight,
    secondary = Primary,
    onSecondary = Color.White,
    secondaryContainer = DarkPrimarySoft,
    onSecondaryContainer = PrimaryLight,
    tertiary = PrimaryLight,
    onTertiary = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkInputBackground,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface,      // AlertDialog 默认容器
    surfaceContainerHighest = DarkSurfaceDone,
    error = Danger,
    onError = Color.White,
    outline = DarkDivider,
    outlineVariant = DarkDivider
)

@Composable
fun QuickPickTheme(content: @Composable () -> Unit) {
    // 主题偏好从 DataStore 读；异步首帧用默认值（SYSTEM/关/TonalSpot），接受首帧闪烁
    val settingsStore = (LocalContext.current.applicationContext as QuickPickApp).settingsStore
    val colorMode by settingsStore.colorMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
    val useMonet by settingsStore.useMonet.collectAsStateWithLifecycle(initialValue = false)
    val paletteStyle by settingsStore.paletteStyle.collectAsStateWithLifecycle(initialValue = "TonalSpot")

    val miuixColorMode = when {
        useMonet && colorMode == "DARK" -> ColorSchemeMode.MonetDark
        useMonet && colorMode == "LIGHT" -> ColorSchemeMode.MonetLight
        useMonet -> ColorSchemeMode.MonetSystem
        colorMode == "DARK" -> ColorSchemeMode.Dark
        colorMode == "LIGHT" -> ColorSchemeMode.Light
        else -> ColorSchemeMode.System
    }

    // ThemeController 属性无公开 setter，偏好变化时重建 controller 生效
    val controller = remember(colorMode, useMonet, paletteStyle) {
        ThemeController(
            colorSchemeMode = miuixColorMode,
            lightColors = QuickPickLightColors,
            darkColors = QuickPickDarkColors,
            // keyColor 固定豆绿：Monet 色板由品牌色生成；Color.Primary 本身不随 Monet 变
            keyColor = if (useMonet) Primary else null,
            paletteStyle = ThemePaletteStyle.entries.firstOrNull { it.name == paletteStyle }
                ?: ThemePaletteStyle.TonalSpot
        )
    }

    // M3 桥接的明暗判定（Monet 只影响 miuix 侧，M3 桥接板固定豆绿）
    val dark = when (colorMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    MiuixTheme(controller = controller, textStyles = AppTypography) {
        MaterialTheme(
            colorScheme = if (dark) DarkColorScheme else LightColorScheme,
            content = content
        )
    }
}
