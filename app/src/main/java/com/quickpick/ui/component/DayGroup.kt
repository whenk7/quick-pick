package com.quickpick.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextPrimary
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    // 该天无未取快递时不显示「全部已取」（edge-cases 五）
    val hasUnpicked = packages.any { !it.isPickedUp }
    // 折叠箭头轻微旋转（animateFloatAsState 默认 spring，NoBouncy 不弹跳）
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
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.rotate(arrowRotation)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = dateLabel,
                style = MiuixTheme.textStyles.title4,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (hasUnpicked) {
                // 当日已取：直接执行，不弹二次确认（列表页清理，设计文档 7.4）
                InlineTextButton(
                    text = "全部已取",
                    onClick = { onMarkDayPickedUp(dateMs) }
                )
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
