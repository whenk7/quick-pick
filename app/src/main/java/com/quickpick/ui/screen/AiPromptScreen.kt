package com.quickpick.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.component.ConfirmDialog
import com.quickpick.ui.theme.Background
import com.quickpick.ui.theme.InputBackground
import com.quickpick.ui.theme.InputFieldColors
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.PrimarySoft
import com.quickpick.ui.theme.Surface
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary
import com.quickpick.ui.theme.TextTertiary
import com.quickpick.ui.viewmodel.AiPromptViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 流程说明
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = CardDefaults.defaultColors(color = PrimarySoft)
                ) {
                    Text(
                        text = "① 审查短信样本（只有正文会被复制）→ ② 复制提示词去问任意 AI → ③ 把 AI 返回的 JSON 贴回来导入",
                        style = MiuixTheme.textStyles.body2,
                        color = TextPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // ① 短信样本
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallTitle(text = "① 短信样本（${samples.size} 条）", modifier = Modifier.weight(1f))
                    if (canRefill) {
                        TextButton(
                            text = "补齐",
                            onClick = { viewModel.refill() },
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
            when {
                loadFailed -> item {
                    Text(
                        text = "没读到短信——请先在系统设置里允许读取短信权限",
                        style = MiuixTheme.textStyles.body2,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                samples.isEmpty() -> item {
                    Text(
                        text = "近 90 天没有可用的短信样本",
                        style = MiuixTheme.textStyles.body2,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                else -> items(samples, key = { it.id }) { sample ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface)
                            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (sample.isExpress) "快递" else "通知",
                            style = MiuixTheme.textStyles.body2,
                            color = if (sample.isExpress) Primary else TextTertiary,
                            modifier = Modifier
                                .background(
                                    if (sample.isExpress) PrimarySoft else InputBackground,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = sample.body,
                            style = MiuixTheme.textStyles.body2,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.removeSample(sample.id) },
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .semantics(mergeDescendants = true) { contentDescription = "移除样本" }
                        ) {
                            Text("×", style = MiuixTheme.textStyles.body1, color = TextTertiary)
                        }
                    }
                }
            }

            // ② 提示词
            item {
                SmallTitle(text = "② 提示词（已包含上面确认的样本）")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(InputBackground)
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = viewModel.buildPrompt(),
                        style = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        copyToClipboard(context, viewModel.buildPrompt())
                        Toast.makeText(context, "已复制，去 AI 那边粘贴吧～", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("📋 一键复制提示词")
                }
            }

            // ③ 粘贴结果
            item {
                SmallTitle(text = "③ 粘贴 AI 返回的 JSON")
                // 单源：按钮点击时直接读 TextFieldState，不回流 ViewModel
                val fieldState = rememberTextFieldState()
                TextField(
                    state = fieldState,
                    modifier = Modifier.fillMaxWidth(),
                    label = "粘贴 AI 的回复（单个 JSON 或数组都行）",
                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 7),
                    colors = InputFieldColors
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.importPasted(fieldState.text.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("检查并导入")
                }
                Spacer(modifier = Modifier.height(24.dp))
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
