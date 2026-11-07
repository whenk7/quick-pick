package com.quickpick.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.component.ConfirmDialog
import com.quickpick.ui.component.InlineTextButton
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.InputBackground
import com.quickpick.ui.theme.InputFieldColors
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.Surface
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary
import com.quickpick.ui.theme.TextTertiary
import com.quickpick.ui.viewmodel.AiPromptViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * AI 生成规则（v1.1.4 重写）：选样本 → 复制提示词 → 贴回 JSON 入库。
 * 样本只展示正文供隐私审查；移除后手动「补齐」（不自动塞，用户裁决）。
 */
@Composable
fun AiPromptScreen(
    viewModel: AiPromptViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val samples by viewModel.samples.collectAsStateWithLifecycle()
    val canRefill by viewModel.canRefill.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    val conflict by viewModel.conflict.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadSamplesOnce()
        viewModel.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MiuixTheme.textStyles.title4, color = TextPrimary)
                }
                Text(
                    text = "AI 生成规则",
                    style = MiuixTheme.textStyles.headline1,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ============ 第 1 步：审查短信样本 ============
            item {
                AiStepHeader(
                    title = "审查短信样本",
                    hint = "上下滚动查看，含隐私的短信点 × 移除；删多了点「补齐」",
                    trailing = if (canRefill) {
                        {
                            InlineTextButton(
                                text = "补齐",
                                onClick = { viewModel.refill() }
                            )
                        }
                    } else null
                )
            }
            item {
                // 定高容器 + 内部滚动：样本可能上百条，铺开会把第 2、3 步顶到屏幕外
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 360.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                ) {
                    when {
                        loadFailed -> SampleHint("没读到短信——请先在系统设置里允许读取短信权限")
                        samples.isEmpty() -> SampleHint("近 90 天没有可用的短信样本")
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(samples, key = { it.id }) { sample ->
                                SampleRow(
                                    sample = sample,
                                    onRemove = { viewModel.removeSample(sample.id) }
                                )
                            }
                        }
                    }
                }
            }

            // ============ 第 2 步：复制提示词 ============
            item {
                AiStepHeader(
                    title = "复制提示词",
                    hint = "已包含上面确认的样本。发给 AI 前请再核对一遍，确认没有敏感信息"
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(InputBackground)
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = viewModel.buildPrompt(),
                        style = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        copyToClipboard(context, viewModel.buildPrompt())
                        Toast.makeText(context, "已复制，去 AI 那边粘贴吧～", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("一键复制提示词")
                }
            }

            // ============ 第 3 步：粘贴结果 ============
            item {
                AiStepHeader(
                    title = "粘贴 AI 返回的 JSON",
                    hint = "把 AI 的回复整段贴进来，单个对象或数组都行"
                )
            }
            item {
                // 单源：按钮点击时直接读 TextFieldState，不回流 ViewModel
                val fieldState = rememberTextFieldState()
                TextField(
                    state = fieldState,
                    modifier = Modifier.fillMaxWidth(),
                    label = "粘贴 AI 的回复",
                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 7),
                    colors = InputFieldColors
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.importPasted(fieldState.text.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("检查并导入")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 同名冲突：逐条裁决（常驻组合 + visible 参数，退场动画见 ConfirmDialog 注释；
    // lastConflictName 必须 remember：conflict 置 null 后的退场动画期间文案不闪空）
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

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ai-rule-prompt", text))
}

/**
 * 单条样本（v1.1.11 重做）：原来是「背景胶囊标签 + 两行正文 + 40dp 按钮」的三段式，
 * 每行 ~72dp，17 条就把整页顶满。改成单行紧凑布局，行高 ~52dp。
 *
 * 正文保留 2 行：用户要判断「这有没有隐私」，截到 1 行看不出内容就没法审。
 */
@Composable
private fun SampleRow(
    sample: AiPromptViewModel.SmsSample,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 类型标记压成一个小色点 + 2 字，去掉胶囊底
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (sample.isExpress) Primary else TextTertiary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = sample.body,
            style = MiuixTheme.textStyles.body2,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(36.dp)
                .semantics(mergeDescendants = true) { contentDescription = "移除这条样本" }
        ) {
            Text("×", style = MiuixTheme.textStyles.body1, color = TextTertiary)
        }
    }
}

/** 定高容器内的占位提示（无权限 / 没样本） */
@Composable
private fun SampleHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
