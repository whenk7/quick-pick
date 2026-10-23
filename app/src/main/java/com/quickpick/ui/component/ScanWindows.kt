package com.quickpick.ui.component

/** 扫描窗口档位（天 → 显示名），设置页与首页副标题共用（v1.1.7 §8） */
val SCAN_WINDOW_LABELS = listOf(7 to "一周", 30 to "一月", 90 to "三月", 180 to "半年")

/** 按天数取显示名，未知值兜底最近档 */
fun scanWindowLabel(days: Int): String =
    SCAN_WINDOW_LABELS.firstOrNull { it.first == days }?.second
        ?: SCAN_WINDOW_LABELS.minBy { kotlin.math.abs(it.first - days) }.second
