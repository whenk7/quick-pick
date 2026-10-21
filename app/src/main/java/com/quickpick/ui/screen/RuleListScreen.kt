package com.quickpick.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.domain.model.MatchRule
import com.quickpick.ui.component.ConfirmDialog
import com.quickpick.ui.component.EmptyState
import com.quickpick.ui.component.InlineTextButton
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.RuleListViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun RuleListScreen(
    viewModel: RuleListViewModel,
    onAddRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    onExportRule: (MatchRule) -> Unit
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val presets = rules.filter { it.isPreset }
    val customs = rules.filter { !it.isPreset }
    // 删除目标：非 null 时展示确认弹窗
    var deleteTarget by remember { mutableStateOf<MatchRule?>(null) }

    // 内层 Scaffold 置零 inset（外层 MainScreen 已处理），否则上下双份留白
    Scaffold(contentWindowInsets = WindowInsets(0), containerColor = Background) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题（一个 item 里多个组件要包一层 Column，否则会叠在一起渲染）
            item {
                Column {
                    Text(
                        text = "匹配规则",
                        style = MiuixTheme.textStyles.title2,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "${rules.size} 条规则，${presets.size} 个预置",
                        style = MiuixTheme.textStyles.body2,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // 预置规则
            if (presets.isNotEmpty()) {
                item {
                    Text(
                        text = "预置规则",
                        style = MiuixTheme.textStyles.body2,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(presets, key = { "preset-${it.id}" }) { rule ->
                    RuleCard(
                        rule = rule,
                        isPreset = true,
                        onClick = { onEditRule(rule.id) },
                        onExport = { onExportRule(rule) },
                        onDelete = { deleteTarget = rule }
                    )
                }
            }

            // 自定义规则
            if (customs.isNotEmpty()) {
                item {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "自定义规则",
                            style = MiuixTheme.textStyles.body2,
                            color = TextTertiary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                items(customs, key = { "custom-${it.id}" }) { rule ->
                    RuleCard(
                        rule = rule,
                        isPreset = false,
                        onClick = { onEditRule(rule.id) },
                        onExport = { onExportRule(rule) },
                        onDelete = { deleteTarget = rule }
                    )
                }
            }

            // 预置正常但无自定义：自定义区给个温和提示（edge-cases 七）
            if (presets.isNotEmpty() && customs.isEmpty()) {
                item {
                    Text(
                        text = "暂无自定义规则",
                        style = MiuixTheme.textStyles.body2,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (rules.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📋",
                        title = "还没有匹配规则",
                        description = "添加规则后才能提取快递信息"
                    )
                }
            }

            // 新增按钮（miuix 无 OutlinedButton：透明 TextButton + 主色描边替代）
            item {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    InlineTextButton(
                        text = "+ 新增匹配规则",
                        onClick = onAddRule,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Primary, RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // 删除规则：二次确认（预置也可删，文案遵循设计系统温和规范）
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除「${target.name}」？",
            message = "删除后将无法恢复，确定要删除吗？",
            confirmText = "删除",
            dangerConfirm = true,
            onConfirm = {
                viewModel.deleteRule(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun RuleCard(
    rule: MatchRule,
    isPreset: Boolean,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val brandColor = BrandColors.forName(rule.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(color = Surface),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 品牌色圆点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(brandColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    style = MiuixTheme.textStyles.headline1,
                    color = TextPrimary
                )
                Text(
                    text = "${rule.configuredFieldCount} 个字段",
                    style = MiuixTheme.textStyles.body2,
                    color = TextSecondary
                )
            }
            // 预置标签：浅灰（落地方案），不用主色
            if (isPreset) {
                Text(
                    text = "预置",
                    style = MiuixTheme.textStyles.body2,
                    color = TextSecondary,
                    modifier = Modifier
                        .background(InputBackground, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            // 操作按钮 48dp 点击区域
            IconButton(onClick = onExport, modifier = Modifier.size(48.dp)) {
                Text("↗", style = MiuixTheme.textStyles.body1, color = TextSecondary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Text("🗑", style = MiuixTheme.textStyles.body2)
            }
        }
    }
}
