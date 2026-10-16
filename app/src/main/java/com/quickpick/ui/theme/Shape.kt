package com.quickpick.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),   // 小组件（分词块单独用 8dp，见 TokenChip）
    medium = RoundedCornerShape(16.dp),  // 卡片
    large = RoundedCornerShape(16.dp),   // 按钮、输入框
    extraLarge = RoundedCornerShape(20.dp) // 弹窗
)
