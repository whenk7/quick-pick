# Phase 4：UI 组件（EmptyState / ConfirmDialog / BottomNavBar / PackageCard / DayGroup）

> 所属轮次：第 3 轮 ｜ 前置：Phase 1 主题、Phase 2 领域模型
> 本文档为可执行清单：按 Task 顺序照做即可，每个 Step 含完整代码。

---

## Phase 4: UI 组件

### Task 4.1: 通用组件 — EmptyState, ConfirmDialog, BottomNavBar

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/component/EmptyState.kt`
- Create: `app/src/main/java/com/quickpick/ui/component/ConfirmDialog.kt`
- Create: `app/src/main/java/com/quickpick/ui/component/BottomNavBar.kt`

- [ ] **Step 1: 创建 EmptyState.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(20.dp))
            action()
        }
    }
}
```

- [ ] **Step 2: 创建 ConfirmDialog.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.Danger
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.Surface

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    dangerConfirm: Boolean = false,   // 删除类操作传 true，确认键用危险色
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // 显式白色容器 + 20dp 圆角：不依赖 colorScheme 槽位，杜绝默认淡紫泄漏
        shape = RoundedCornerShape(20.dp),
        containerColor = Surface,
        tonalElevation = 2.dp,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = if (dangerConfirm) Danger else Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}
```

- [ ] **Step 3: 创建 BottomNavBar.kt**

```kotlin
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
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/component/ && git commit -m "feat: add shared components — EmptyState, ConfirmDialog, BottomNavBar"
```

---

### Task 4.2: 快递卡片组件

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/component/PackageCard.kt`
- Create: `app/src/main/java/com/quickpick/ui/component/DayGroup.kt`

- [ ] **Step 1: 创建 PackageCard.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.theme.*

@Composable
fun PackageCard(
    record: PackageRecord,
    onTogglePickedUp: () -> Unit,
    onCopyField: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val brandColor = BrandColors.forName(record.companyName)
    // 已取件：内容整体 50% 透明（置灰保留，不删除）
    val contentAlpha = if (record.isPickedUp) 0.5f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isPickedUp) SurfaceDone else Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) 4.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部：品牌色点 + 公司名 + 状态标签 + 时间 + 勾选圆圈
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 品牌色 8dp 圆点（全卡唯一的品牌色露出）
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(brandColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f).alpha(contentAlpha)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        if (record.isPickedUp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✓ 已取件", style = MaterialTheme.typography.bodySmall, color = Done)
                        }
                        // 「不完整」浅灰标签放右上角区域，不用红/橙色制造焦虑
                        if (record.isIncomplete && !record.isPickedUp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "不完整",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                modifier = Modifier
                                    .background(InputBackground, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = formatRelativeTime(record.smsTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                // 勾选圆圈：48dp 点击区域；未取=主色描边空心圆（可见！），已取=主色实心+白勾
                IconButton(
                    onClick = onTogglePickedUp,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (record.isPickedUp) Primary else Color.Transparent)
                            .border(1.5.dp, Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (record.isPickedUp) {
                            Text("✓", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 取件码 — 28sp 大字突出；已取件加删除线
            record.pickupCode?.let { code ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(contentAlpha)
                ) {
                    Text(
                        text = if (code.length > 4) "取件码" else "柜号",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = code,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            textDecoration = if (record.isPickedUp) TextDecoration.LineThrough else null
                        ),
                        color = if (record.isPickedUp) TextSecondary else Primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onCopyField(code) }) {
                        Text("复制", color = Primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 地址（点击复制）
            record.address?.let { address ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📍 $address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .clickable { onCopyField(address) }
                )
            }

            // 展开详情：左侧强调色细线 + 全部字段
            AnimatedVisibility(visible = expanded) {
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Primary)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.alpha(contentAlpha)) {
                        record.trackingNumber?.let { tracking ->
                            DetailRow("单号", tracking) { onCopyField(tracking) }
                        }
                        record.address?.let { address ->
                            DetailRow("地址", address) { onCopyField(address) }
                        }
                        record.phone?.let { phone ->
                            DetailRow("电话", phone) { onCopyField(phone) }
                        }
                        if (record.isIncomplete) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "信息不完整，可以在规则页优化匹配规则",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCopy) {
            Text("复制", color = Primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

注：规则编辑器的「实时预览」直接复用本组件（Task 5.5），保证与列表页样式完全一致。

- [ ] **Step 2: 创建 DayGroup.kt**

```kotlin
package com.quickpick.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DayGroup(
    dateLabel: String,
    dateMs: Long,
    packages: List<PackageRecord>,
    collapsed: Boolean,
    onToggleCollapse: (Long) -> Unit,
    onTogglePickedUp: (PackageRecord) -> Unit,
    onCopyField: (String) -> Unit,
    onMarkDayPickedUp: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmVisible by remember { mutableStateOf(false) }
    // 该天无未取快递时不显示「全部已取」（edge-cases 五）
    val hasUnpicked = packages.any { !it.isPickedUp }
    // 折叠箭头轻微旋转（200ms ease-out，不弹跳）
    val arrowRotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        label = "arrow"
    )

    Column(modifier = modifier) {
        // 分组标题行：整行点击折叠/展开
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleCollapse(dateMs) }
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▾",
                color = Primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.rotate(arrowRotation)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (hasUnpicked) {
                TextButton(onClick = { confirmVisible = true }) {
                    Text("全部已取", style = MaterialTheme.typography.bodySmall, color = Primary)
                }
            }
        }

        // 卡片列表（可折叠）
        AnimatedVisibility(visible = !collapsed) {
            Column {
                packages.forEach { pkg ->
                    PackageCard(
                        record = pkg,
                        onTogglePickedUp = { onTogglePickedUp(pkg) },
                        onCopyField = onCopyField,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
        }
    }

    if (confirmVisible) {
        ConfirmDialog(
            title = "全部标记为已取？",
            message = "把「$dateLabel」的 ${packages.count { !it.isPickedUp }} 件待取快递标记为已取件。",
            onConfirm = {
                onMarkDayPickedUp(dateMs)
                confirmVisible = false
            },
            onDismiss = { confirmVisible = false }
        )
    }
}

/** 格式化相对时间 */
fun formatRelativeTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMs

    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestampMs))
    }
}

/** 分组标题格式：今天 · 5月30日 / 昨天 · 5月29日 / 5月27日 · 周二 */
fun formatDayLabel(timestampMs: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = timestampMs }
    val today = Calendar.getInstance()
    val datePart = SimpleDateFormat("M月d日", Locale.CHINESE).format(Date(timestampMs))

    fun Calendar.isSameDay(other: Calendar) =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

    return when {
        target.isSameDay(today) -> "今天 · $datePart"
        // 注意顺序：先判今天，再把 today 往前拨一天判昨天
        target.isSameDay(today.apply { add(Calendar.DAY_OF_YEAR, -1) }) -> "昨天 · $datePart"
        else -> {
            val week = SimpleDateFormat("E", Locale.CHINESE).format(Date(timestampMs))
            "$datePart · $week"
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/component/ && git commit -m "feat: add PackageCard and DayGroup components"
```

---

