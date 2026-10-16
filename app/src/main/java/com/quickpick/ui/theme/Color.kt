package com.quickpick.ui.theme

import androidx.compose.ui.graphics.Color

// 主色调：豆绿
val Primary = Color(0xFF6AB37B)
val PrimaryLight = Color(0xFFA8D5B0)
val PrimarySoft = Color(0xFFE9F3EB) // 10% 豆绿叠白的等效实体色（容器色必须实体，避免透出底层背景）

// 背景
val Background = Color(0xFFF9F9F6)
val Surface = Color(0xFFFFFFFF)
val SurfaceDone = Color(0xFFF5F5F5)
val InputBackground = Color(0xFFF5F5F5)

// 文字（从不用纯黑）
val TextPrimary = Color(0xFF424242)
val TextSecondary = Color(0xFF757575)
val TextTertiary = Color(0xFF9E9E9E)

// 语义色
val Pending = Color(0xFFFFB76B)
val Done = Color(0xFFBDBDBD)
val Danger = Color(0xFFEF5350)

// 分割线
val Divider = Color(0xFFEEEEEE)

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
