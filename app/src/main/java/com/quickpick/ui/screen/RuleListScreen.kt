package com.quickpick.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.domain.model.MatchRule
import androidx.compose.foundation.clickable
import com.quickpick.ui.component.ConfirmDialog
import com.quickpick.ui.component.EmptyState
import com.quickpick.ui.component.ExportRulesDialog
import com.quickpick.ui.component.ImportRulesDialog
import com.quickpick.ui.component.InlineTextButton
import com.quickpick.ui.component.JsonPreviewDialog
import com.quickpick.ui.component.QpIcons
import com.quickpick.ui.component.glass.glassNavBarClearance
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.RuleListViewModel
import com.quickpick.util.JsonExporter
import com.quickpick.util.UiNotifier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
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
    onExportRule: (MatchRule) -> Unit,
    onOpenAi: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val conflict by viewModel.conflict.collectAsStateWithLifecycle()
    val presets = rules.filter { it.isPreset }
    val customs = rules.filter { !it.isPreset }
    // 删除规则：直接删 + 轻提示（v1.1.5 用户裁决：不再二次确认弹窗；v1.1.7 §3 文案精简）
    val onDeleteRule: (MatchRule) -> Unit = { rule ->
        viewModel.deleteRule(rule)
        UiNotifier.notify("已删除「${rule.name}」，请首页下拉刷新")
    }
    // JSON 预览目标：非 null 时展示 JsonPreviewDialog（分享改为长按卡片，见 RuleCard）
    var jsonRule by remember { mutableStateOf<MatchRule?>(null) }
    // 导入/导出弹窗显隐
    var showImport by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 导入文件选择器（MIME */*：部分国产 ROM 不认 application/json 会选不到）
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()
            if (text != null) {
                val list = viewModel.parseImport(text)
                if (list != null) {
                    viewModel.importRules(list)
                    showImport = false
                } else {
                    Toast.makeText(context, "没解析出来，换个文件试试～", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 导出存文件（SAF）
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportAllJson()
                if (json != null) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(json.toByteArray())
                        }
                    }.onSuccess {
                        Toast.makeText(context, "已存好～", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, "没能写入这个文件，换个位置试试～", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 内层 Scaffold 置零 inset（外层 MainScreen 已处理），否则上下双份留白
    Scaffold(contentWindowInsets = WindowInsets(0), containerColor = Background) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            // 底部留白给悬浮玻璃底栏（inset 动态）
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = glassNavBarClearance()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题 + 三操作按钮（一个 item 里多个组件要包一层 Column，否则会叠在一起渲染）
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
                    // 新建 / AI 生成 / 导入 / 导出（v1.1.7 §9：图标+文字四按钮，底部新增挪入此处）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            WorkshopAction("新建", QpIcons.Plus, "新建规则") { onAddRule() },
                            WorkshopAction("AI 生成", QpIcons.Sparkle, "AI 生成规则") { onOpenAi() },
                            WorkshopAction("导入", QpIcons.Download, "导入规则") { showImport = true },
                            WorkshopAction("导出", QpIcons.Upload, "导出规则") { showExport = true }
                        ).forEach { (label, icon, desc, action) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable(onClick = action)
                                    .padding(vertical = 10.dp)
                                    .semantics(mergeDescendants = true) { contentDescription = desc },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(label, style = MiuixTheme.textStyles.body2, color = Primary)
                            }
                        }
                    }
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
                        onShowJson = { jsonRule = rule },
                        onShare = { onExportRule(rule) },
                        onDelete = { onDeleteRule(rule) }
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
                        onShowJson = { jsonRule = rule },
                        onShare = { onExportRule(rule) },
                        onDelete = { onDeleteRule(rule) }
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

            // v1.1.7 §9：新增挪到顶部四按钮，底部不再重复
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // JSON 预览弹窗：常驻组合 + show 参数（退出动画同 ConfirmDialog 注释）。
    // 记住最近的非 null 规则：jsonRule 置 null 后的退场动画期间内容不闪空
    var lastJsonRule by remember { mutableStateOf<MatchRule?>(null) }
    jsonRule?.let { lastJsonRule = it }
    JsonPreviewDialog(
        show = jsonRule != null,
        json = lastJsonRule?.let { JsonExporter.exportSingle(it) } ?: "",
        onDismiss = { jsonRule = null }
    )

    // 导入/导出弹窗（常驻组合 + show 参数，退出动画约定同 ConfirmDialog）
    ImportRulesDialog(
        show = showImport,
        onPickFile = { importLauncher.launch("*/*") },
        onParse = { viewModel.parseImport(it) },
        onConfirmImport = { list ->
            viewModel.importRules(list)
            showImport = false
        },
        onDismiss = { showImport = false }
    )
    ExportRulesDialog(
        show = showExport,
        onCopy = {
            scope.launch {
                val json = viewModel.exportAllJson()
                if (json != null) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("rules-json", json))
                    Toast.makeText(context, "已复制全部规则 JSON～", Toast.LENGTH_SHORT).show()
                }
            }
            showExport = false
        },
        onSaveFile = {
            exportLauncher.launch("quickpick-rules.json")
            showExport = false
        },
        onDismiss = { showExport = false }
    )

    // 导入同名冲突：逐条裁决（记住最近冲突名，退场动画期间文案不闪空）
    var lastConflictName by remember { mutableStateOf("") }
    conflict?.let { lastConflictName = it.incoming.name }
    ConfirmDialog(
        visible = conflict != null,
        title = "发现同名规则",
        message = "已存在「$lastConflictName」，要覆盖它吗？",
        confirmText = "覆盖",
        dismissText = "跳过",
        // 裁决弹窗（v1.1.7 §2）：返回/点外部无效，必须明确选覆盖或跳过
        swallowBack = true,
        onConfirm = { viewModel.resolveConflict(true) },
        onDismiss = { viewModel.resolveConflict(false) }
    )
}

/** 工坊按钮（v1.1.7 §9）：图标+文字四等分横排 */
private data class WorkshopAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val desc: String,
    val onClick: () -> Unit
)

/**
 * 规则卡片：点击进编辑，**长按分享**（原 ↗ 直触分享改为长按，图标位让给 `</>` JSON 预览，
 * 避免卡片挤三个 48dp 按钮），`</>` 弹 JSON 预览，🗑 直接删除+Snackbar 轻提示（v1.1.5 起不弹确认）。
 */
@Composable
private fun RuleCard(
    rule: MatchRule,
    isPreset: Boolean,
    onClick: () -> Unit,
    onShowJson: () -> Unit,
    onShare: () -> Unit,
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
        onClick = onClick,
        onLongPress = onShare
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
            // 操作按钮 48dp 点击区域（contentDescription 供无障碍与 UI 测试定位；
            // mergeDescendants 把内部 clickable 的 OnClick 动作并到本节点，否则语义点击打不到）
            IconButton(
                onClick = onShowJson,
                modifier = Modifier
                    .size(48.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "查看规则 JSON" }
            ) {
                Text("</>", style = MiuixTheme.textStyles.body1, color = TextSecondary)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "删除规则" }
            ) {
                Text("🗑", style = MiuixTheme.textStyles.body2)
            }
        }
    }
}
