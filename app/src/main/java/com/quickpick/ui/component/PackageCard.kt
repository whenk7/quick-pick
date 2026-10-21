package com.quickpick.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import top.yukonga.miuix.kmp.utils.PressFeedbackType

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
            .shadow(if (expanded) 4.dp else 2.dp, RoundedCornerShape(16.dp)),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = if (record.isPickedUp) SurfaceDone else Surface
        ),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = { expanded = !expanded }
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
                            style = MiuixTheme.textStyles.headline1,
                            color = TextPrimary
                        )
                        if (record.isPickedUp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✓ 已取件", style = MiuixTheme.textStyles.body2, color = Done)
                        }
                    }
                    Text(
                        text = formatRelativeTime(record.smsTimestamp),
                        style = MiuixTheme.textStyles.body2,
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
                            Text("✓", color = Color.White, style = MiuixTheme.textStyles.body2)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 取件码 — 28sp 大字突出；已取件加删除线
            // 颜色必须显式传（T6 起 AppTypography.title1 不再内嵌 Primary 色）
            record.pickupCode?.let { code ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(contentAlpha)
                ) {
                    Text(
                        text = if (code.length > 4) "取件码" else "柜号",
                        style = MiuixTheme.textStyles.body2,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = code,
                        style = MiuixTheme.textStyles.title1.copy(
                            fontFamily = FontFamily.Monospace,
                            textDecoration = if (record.isPickedUp) TextDecoration.LineThrough else null
                        ),
                        color = if (record.isPickedUp) TextSecondary else Primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    InlineTextButton(text = "复制", onClick = { onCopyField(code) })
                }
            }

            // 地址（点击复制）
            record.address?.let { address ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📍 $address",
                    style = MiuixTheme.textStyles.body1,
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
            style = MiuixTheme.textStyles.body2,
            color = TextTertiary,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        InlineTextButton(text = "复制", onClick = onCopy)
    }
}
