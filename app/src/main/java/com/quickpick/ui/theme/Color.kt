package com.quickpick.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import top.yukonga.miuix.kmp.basic.TextFieldColors
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ============ 语义常量：全部从 MiuixTheme.colorScheme 取值 ============
// 主题引擎（固定板/Monet）切换时全局自动跟随，UI 层不得引用任何硬编码色值。

/** 主题色（选中态/强调/聚焦描边） */
val Primary: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.primary

// 背景
val Background: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.background

/**
 * 卡片底：**保证与页面底始终有一档色差**（v1.1.15）。
 *
 * 原实现直取 `colorScheme.surface`，在内置板上侥幸成立（浅色 #F7F7F7 落在
 * #FFFFFF 页面上），但 Monet 板按 M3 规范让 background 与 surface 取同一 tone，
 * 两者同色 → 卡片只剩下 2dp 阴影，在浅紫底上几乎看不见，整页融成一坨。
 * 内置板里 surfaceContainer / surfaceVariant 同样与 background 同色（浅深两套都是），
 * 所以不能靠换令牌解决，必须在**取值时**兜底。
 *
 * 判据用感知亮度差：够用就用原色（保留内置板既有观感，深色 #000000 卡片不变），
 * 不够就朝 onSurface 方向混 5% 造出台阶。
 */
val Surface: Color
    @Composable @ReadOnlyComposable get() = ensureVisibleOverBackground(
        MiuixTheme.colorScheme.surface,
        MiuixTheme.colorScheme.background,
        MiuixTheme.colorScheme.onBackground
    )

/** 已取件卡（低存在感底）：同 Surface 的兜底逻辑——内置板深色 surfaceContainerHigh 与页面同色 */
val SurfaceDone: Color
    @Composable @ReadOnlyComposable get() = ensureVisibleOverBackground(
        MiuixTheme.colorScheme.surfaceContainerHigh,
        MiuixTheme.colorScheme.background,
        MiuixTheme.colorScheme.onBackground
    )

val InputBackground: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.surfaceVariant

/** 感知亮度（sRGB 加权，够用于「肉眼能不能看出台阶」的判断） */
private fun Color.perceivedLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

/**
 * 最小可见亮度差。
 *
 * 内置板浅色的 surface(#F7F7F7) 与 background(#FFFFFF) 实测差 0.0314，
 * 阈值必须低于它（否则内置板也会被改色，动到既有观感）；Monet 板下两者差 ~0
 * 必然触发兜底。取 0.025 留出余量，不吃浮点误差。
 */
private const val MIN_LUMINANCE_STEP = 0.025f

/**
 * 与页面底色差不足时，朝 [topColor] 方向混入 5% 造出台阶。
 * 混入比固定色值安全：浅色底朝黑走、深色底朝白走，方向自动正确，不写死任何色值。
 */
private fun ensureVisibleOverBackground(
    color: Color,
    background: Color,
    topColor: Color
): Color {
    if (kotlin.math.abs(color.perceivedLuminance() - background.perceivedLuminance()) >= MIN_LUMINANCE_STEP) {
        return color
    }
    return topColor.copy(alpha = 0.05f).compositeOver(background)
}

/**
 * 主题色弱容器（小面积强调底：命中词块、提示卡）。
 * 不能用 primaryContainer——miuix 内置板它是高饱和强色，会成一坨实色块；
 * 以 primary 12% 混入 surface 合成实体色（不透底），Monet/固定板下都柔和。
 */
val PrimarySoft: Color
    @Composable @ReadOnlyComposable get() = run {
        val scheme = MiuixTheme.colorScheme
        scheme.primary.copy(alpha = 0.12f).compositeOver(scheme.surface)
    }

// 文字
val TextPrimary: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.onBackground
val TextSecondary: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.onSurfaceSecondary
val TextTertiary: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.onSurfaceVariantSummary

// 分割线
val Divider: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.dividerLine

/** 危险操作色（删除等） */
val Danger: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.error

/** 已完成/置灰（中性灰，明暗通用） */
val Done = Color(0xFFBDBDBD)

// 输入框配色：浅灰底 + 灰标签 + 聚焦主题色描边
// （miuix TextField 默认 secondaryContainer 底会让每个输入框泛主题色，违反单强调色原则）
val InputFieldColors: TextFieldColors
    @Composable get() = TextFieldDefaults.textFieldColors(
        backgroundColor = InputBackground,
        labelColor = TextTertiary,
        borderColor = Primary
    )

/** 快递公司品牌色（小面积点缀，不随主题引擎变化） */
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
    val DEFAULT = Color(0xFF9E9E9E)   // 未知公司：中性灰点

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
