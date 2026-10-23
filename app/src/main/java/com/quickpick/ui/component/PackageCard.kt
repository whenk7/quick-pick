package com.quickpick.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.theme.*
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 快递卡片（v1.1.7 §4 重做）：
 * - 未取件：三行无展开——品牌点+标题+时间(HH:mm)+圆圈 / 取件码大字 / 地址
 * - 已取件：紧凑单行（存在感最低）
 * 手势（用户拍板）：单击=无；长按=短信原文弹窗；左滑=已取件（SwipeToReveal 包装，Task 6）；
 * 右滑=无；圆圈=切换已取件。卡片上无任何复制功能（复制统一在原文弹窗的「复制全文」）。
 */
@Composable
fun PackageCard(
    record: PackageRecord,
    onTogglePickedUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSmsDetail by remember { mutableStateOf(false) }
    val brandColor = BrandColors.forName(record.companyName)

    // 已取件：紧凑单行卡。单击无功能；长按看原文；圆圈=取消已取
    if (record.isPickedUp) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showSmsDetail = true }
                ),
            cornerRadius = 12.dp,
            colors = CardDefaults.defaultColors(color = SurfaceDone)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(brandColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = record.companyName,
                    style = MiuixTheme.textStyles.body1,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("✓", style = MiuixTheme.textStyles.body2, color = Done)
                Spacer(modifier = Modifier.weight(1f))
                record.pickupCode?.let { code ->
                    Text(
                        text = code,
                        style = MiuixTheme.textStyles.body1.copy(
                            fontFamily = FontFamily.Monospace,
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = TextTertiary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onTogglePickedUp,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "取消已取件" }
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, style = MiuixTheme.textStyles.body2)
                    }
                }
            }
        }
        SmsDetailDialog(show = showSmsDetail, record = record, onDismiss = { showSmsDetail = false })
        return
    }

    // 未取件：三行紧凑卡 + 左滑标记已取件（露出动画，阈值 35%）
    SwipeToReveal(
        actionLabel = "✓ 已取件",
        actionColor = Primary,
        modifier = modifier,
        onTrigger = onTogglePickedUp
    ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},                              // 单击无功能（用户裁决）
                onLongClick = { showSmsDetail = true }     // 长按 = 短信原文
            ),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(color = Surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // 行1：品牌点 + 标题 + 时间 + 圆圈（日期不显示——吸顶分组头已有）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(brandColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = record.companyName,
                    style = MiuixTheme.textStyles.headline1,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimeHm(record.smsTimestamp),
                    style = MiuixTheme.textStyles.body2,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.weight(1f))
                // 圆圈：48dp 点击区；未取=主色描边空心圆，点按标已取
                IconButton(
                    onClick = onTogglePickedUp,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "标记已取件" }
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Primary, CircleShape)
                    )
                }
            }

            // 行2：取件码大字（行距收紧：6dp）
            record.pickupCode?.let { code ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (code.length > 4) "取件码" else "柜号",
                        style = MiuixTheme.textStyles.body2,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = code,
                        style = MiuixTheme.textStyles.title1.copy(fontFamily = FontFamily.Monospace),
                        color = Primary
                    )
                }
            }

            // 行3：地址（行距 4dp）
            record.address?.let { address ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 $address",
                    style = MiuixTheme.textStyles.body1,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
    }

    // 短信原文弹窗：常驻组合 + show 参数（退出动画约定同 ConfirmDialog）
    SmsDetailDialog(show = showSmsDetail, record = record, onDismiss = { showSmsDetail = false })
}
