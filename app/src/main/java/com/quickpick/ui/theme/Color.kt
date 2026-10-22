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
val Surface: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.surface
val SurfaceDone: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.surfaceContainerHigh
val InputBackground: Color @Composable @ReadOnlyComposable get() = MiuixTheme.colorScheme.surfaceVariant

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
