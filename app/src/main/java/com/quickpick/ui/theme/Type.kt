package com.quickpick.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.TextStyles
import top.yukonga.miuix.kmp.theme.defaultTextStyles

/**
 * 设计系统字号（28/18/16/15/14/12/15sp），覆盖 miuix 默认 token。
 * M3→miuix token 映射（T7 组件迁移时按此全局替换）：
 * headlineLarge→title1、headlineMedium→title2、titleLarge→title4、titleMedium→headline1、
 * bodyLarge→main、bodyMedium→body1、bodySmall→body2、labelLarge→subtitle。
 * 不带颜色：文字色由调用处显式传入，或由组件 LocalContentColor 提供（随明暗切换）。
 */
val AppTypography: TextStyles = defaultTextStyles(
    // 取件码 — 页面最大最醒目的数字
    title1 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    ),
    // 页面标题
    title2 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    // 分组标题
    title4 = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    ),
    // 公司名
    headline1 = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    // 正文
    main = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    ),
    // 辅助字段
    body1 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    // 小字 / 标签 / 时间
    body2 = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),
    // 按钮文字
    subtitle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
    )
)
