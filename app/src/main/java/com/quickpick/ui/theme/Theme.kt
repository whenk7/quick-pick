package com.quickpick.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 覆盖完整槽位，杜绝 M3 默认紫色（弹窗容器 surfaceContainerHigh 等）泄漏
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = Primary,
    secondary = Primary,
    onSecondary = Color.White,
    secondaryContainer = PrimarySoft,   // 底部导航选中胶囊
    onSecondaryContainer = Primary,
    tertiary = PrimaryLight,
    onTertiary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = InputBackground,   // 输入框/浅灰底
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface,     // AlertDialog 默认容器
    surfaceContainerHighest = SurfaceDone,
    error = Danger,                     // 删除操作专用
    onError = Color.White,
    outline = Divider,
    outlineVariant = Divider
)

@Composable
fun QuickPickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
