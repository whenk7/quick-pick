package com.quickpick.ui.component

// 图标定案：emoji（零版权风险、零资源文件），品牌色只出现在 8dp 圆点
// 底栏实现已迁至 component/glass/LiquidGlassNavigationBar.kt（悬浮玻璃底栏，T8）
enum class BottomTab(val label: String, val icon: String) {
    PACKAGES("快递", "📦"),
    RULES("规则", "🧩"),
    SETTINGS("设置", "⚙️")
}
