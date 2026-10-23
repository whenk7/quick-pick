package com.quickpick.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.domain.model.groupForImmersion
import com.quickpick.ui.component.EmptyState
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.BrandColors
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.Surface
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary
import com.quickpick.ui.theme.TextTertiary
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 字号三档（标准/大/特大）缩放系数 */
private val SIZE_SCALES = listOf(1.0f, 1.25f, 1.5f)

/**
 * 沉浸式取件模式（v1.1.7 §11）：货架前大字看码。
 * 行只读（用户裁决）；分组按别名后地址；组件数倒序、组内时间倒序。
 * 底部：A－/A＋ 三档循环 + 全部取完。
 */
@Composable
fun ImmersionScreen(
    packages: List<PackageRecord>,
    onMarkAllPicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 智能默认档：件少（≤8）默认大档，件多默认标准档；会话内记忆（不落盘）
    var sizeLevel by rememberSaveable { mutableIntStateOf(if (packages.size <= 8) 1 else 0) }
    val scale = SIZE_SCALES[sizeLevel]
    val groups = remember(packages) { groupForImmersion(packages) }

    Column(modifier = modifier.fillMaxSize().background(Background)) {
        if (groups.isEmpty()) {
            EmptyState(
                emoji = "🎉",
                title = "没有待取的快递",
                description = "取完了，回家吧～",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                groups.forEach { group ->
                    stickyHeader(key = "loc-${group.location}") {
                        // 吸顶分组头：实体色遮住下方滚动内容
                        Text(
                            text = "${group.location} · ${group.items.size}",
                            style = MiuixTheme.textStyles.title4.copy(fontSize = 15.sp * scale),
                            color = TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Background)
                                .padding(vertical = 8.dp)
                        )
                    }
                    items(group.items, key = { "pkg-${it.id}" }) { pkg ->
                        ImmersionRow(pkg, scale)
                    }
                }
            }
        }

        // 底部操作栏：A－ A＋ 全部取完（底部留白给悬浮玻璃底栏）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = com.quickpick.ui.component.glass.glassNavBarClearance()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                text = "A－",
                onClick = { sizeLevel = (sizeLevel + SIZE_SCALES.size - 1) % SIZE_SCALES.size }
            )
            TextButton(
                text = "A＋",
                onClick = { sizeLevel = (sizeLevel + 1) % SIZE_SCALES.size }
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onMarkAllPicked,
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text("全部取完", color = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

/** 单行：月日（等宽小灰）+ 色点标题（定宽省略）+ 取件码（等宽粗体右对齐，不截断） */
@Composable
private fun ImmersionRow(pkg: PackageRecord, scale: Float) {
    // 取件码不许截断（少一位就取不了件）：按长度降档，越长越小
    val codeLen = pkg.pickupCode?.length ?: 0
    val codeScale = scale * when {
        codeLen <= 6 -> 1.0f
        codeLen <= 10 -> 0.85f
        else -> 0.66f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (10f * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(pkg.smsTimestamp)),
            style = MiuixTheme.textStyles.body2.copy(
                fontSize = 12.sp * scale,
                fontFamily = FontFamily.Monospace
            ),
            color = TextTertiary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width((8f * scale).dp)
                .height((8f * scale).dp)
                .clip(CircleShape)
                .background(BrandColors.forName(pkg.companyName))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = pkg.companyName,
            style = MiuixTheme.textStyles.body1.copy(fontSize = 14.sp * scale),
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width((88f * scale).dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = pkg.pickupCode ?: "—",
            style = MiuixTheme.textStyles.title1.copy(
                fontSize = 22.sp * codeScale,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = Primary,
            maxLines = 1
        )
    }
}
