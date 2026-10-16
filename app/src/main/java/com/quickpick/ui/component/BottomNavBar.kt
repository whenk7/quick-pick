package com.quickpick.ui.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextSecondary

// 图标定案：emoji（零版权风险、零资源文件），品牌色只出现在 8dp 圆点
enum class BottomTab(val label: String, val icon: String) {
    PACKAGES("快递", "📦"),
    RULES("规则", "🧩"),
    SETTINGS("设置", "⚙️")
}

@Composable
fun BottomNavBar(
    currentTab: BottomTab,
    onTabClick: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabClick(tab) },
                icon = { Text(text = tab.icon, style = MaterialTheme.typography.titleLarge) },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentTab == tab) Primary else TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
