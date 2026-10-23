package com.quickpick.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.SurfaceDone
import com.quickpick.ui.theme.TextPrimary
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * 分组标题行（LazyColumn stickyHeader 专用，卡片正文见 [DayGroupContent]）。
 * 背景必须实体色：吸顶时遮住下方滚过的卡片——展开态 Background（与页底同色无缝），
 * 折叠态 SurfaceDone 底色 + 左侧 3dp Primary 竖线做视觉区分（线槽位常驻透明，
 * 切换时内容不产生 3dp 横移；区分只在折叠态可见）。
 */
@Composable
fun DayGroupHeader(
    dateLabel: String,
    dateMs: Long,
    collapsed: Boolean,
    hasUnpicked: Boolean,
    onToggleCollapse: (Long) -> Unit,
    onMarkDayPickedUp: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 折叠箭头轻微旋转（animateFloatAsState 默认 spring，NoBouncy 不弹跳）
    val arrowRotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        label = "arrow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (collapsed) SurfaceDone else Background)
            .clickable { onToggleCollapse(dateMs) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 折叠态左侧 3dp Primary 竖线（样式同 PackageCard 详情强调线）
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (collapsed) Primary else Color.Transparent)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
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
    }
}

/**
 * 分组卡片正文（stickyHeader 下的独立 item）。
 * 折叠动画 = AnimatedVisibility 高度收合；折叠时本 item 高度归零但保持组合，
 * 展开动画可原路播回（不做条件 item，否则折叠瞬间消失无动画）。
 */
@Composable
fun DayGroupContent(
    packages: List<PackageRecord>,
    collapsed: Boolean,
    onTogglePickedUp: (PackageRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = !collapsed) {
        Column(modifier = modifier) {
            packages.forEach { pkg ->
                PackageCard(
                    record = pkg,
                    onTogglePickedUp = { onTogglePickedUp(pkg) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
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

/** 卡片行内时间（v1.1.7：卡片不显示日期，日期靠吸顶分组头） */
fun formatTimeHm(timestampMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMs))

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
