package com.quickpick.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.TextFieldColors
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ============ 固定色（明暗通用，不受 Monet 影响） ============

// 主色调：豆绿（品牌色，动态取色开启时也保持原色）
val Primary = Color(0xFF6AB37B)
val PrimaryLight = Color(0xFFA8D5B0)

// 语义色（暗色下豆绿和橙色保持原色）
val Pending = Color(0xFFFFB76B)
val Done = Color(0xFFBDBDBD)
val Danger = Color(0xFFEF5350)

// ============ 浅色原始色板（供浅色 scheme 构建，勿在 UI 直接引用） ============

val LightBackground = Color(0xFFF9F9F6)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceDone = Color(0xFFF5F5F5)
val LightInputBackground = Color(0xFFF5F5F5)
val LightPrimarySoft = Color(0xFFE9F3EB) // 10% 豆绿叠白的等效实体色（容器色必须实体，避免透出底层背景）

// 文字（从不用纯黑）
val LightTextPrimary = Color(0xFF424242)
val LightTextSecondary = Color(0xFF757575)
val LightTextTertiary = Color(0xFF9E9E9E)

val LightDivider = Color(0xFFEEEEEE)

// ============ 深色原始色板（供深色 scheme 构建，勿在 UI 直接引用） ============

val DarkBackground = Color(0xFF1A1A1A)
val DarkSurface = Color(0xFF2C2C2C)
val DarkSurfaceDone = Color(0xFF242424)
val DarkTextPrimary = Color(0xFFE0E0E0)
val DarkTextSecondary = Color(0xFF9E9E9E)
val DarkTextTertiary = Color(0xFF6E6E6E)
val DarkInputBackground = Color(0xFF333333)
val DarkDivider = Color(0xFF3A3A3A)
val DarkPrimarySoft = Color(0xFF2E3B31) // 深色容器色：低饱和深绿，与 LightPrimarySoft 对应

// ============ 语义常量：从 MiuixTheme.colorScheme 取值，明暗/Monet 自动切换 ============

// 背景
val Background: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.background
val Surface: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.surface
val SurfaceDone: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.surfaceContainerHigh
val InputBackground: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.surfaceVariant
val PrimarySoft: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.primaryContainer

// 文字
val TextPrimary: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.onBackground
val TextSecondary: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.onSurfaceSecondary
val TextTertiary: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.onSurfaceVariantSummary

// 分割线
val Divider: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.dividerLine

// 输入框配色：浅灰底 + 灰标签 + 聚焦豆绿描边
// （miuix TextField 默认 secondaryContainer 底=淡绿，会让每个输入框都泛绿，违反单强调色原则）
val InputFieldColors: TextFieldColors
    @Composable get() = TextFieldDefaults.textFieldColors(
        backgroundColor = InputBackground,
        labelColor = TextTertiary,
        borderColor = Primary
    )

// 快递公司品牌色（小面积点缀）
object BrandColors {
    val SF = Color(0xFF000000)        // 顺丰
    val JD = Color(0xFFE2231A)        // 京东
    val JT = Color(0xFFE50012)        // 极兔
    val ZTO = Color(0xFF006CB7)       // 中通
    val YTO = Color(0xFF662D91)       // 圆通
    val YUNDA = Color(0xFFFFCC00)     // 韵达
    val STO = Color(0xFFFF6600)       // 申通
    val EMS = Color(0xFF006633)       // 邮政
    val DEPPON = Color(0xFF0056D2)    // 德邦
    val CAINIAO = Color(0xFF0066FF)   // 菜鸟
    val DEFAULT = Primary

    fun forName(name: String): Color = when {
        name.contains("顺丰") -> SF
        name.contains("京东") -> JD
        name.contains("极兔") -> JT
        name.contains("中通") -> ZTO
        name.contains("圆通") -> YTO
        name.contains("韵达") -> YUNDA
        name.contains("申通") -> STO
        name.contains("邮政") || name.contains("EMS") -> EMS
        name.contains("德邦") -> DEPPON
        name.contains("菜鸟") -> CAINIAO
        else -> DEFAULT
    }
}
