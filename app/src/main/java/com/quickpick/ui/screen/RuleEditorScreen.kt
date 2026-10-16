package com.quickpick.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.component.*
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.ActiveField
import com.quickpick.ui.viewmodel.EditorState
import com.quickpick.ui.viewmodel.RuleEditorViewModel

@Composable
fun RuleEditorScreen(
    viewModel: RuleEditorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedToken by remember { mutableStateOf<String?>(null) }
    var rescanDialogVisible by remember { mutableStateOf(false) }

    // 一次性提示 → Toast
    LaunchedEffect(Unit) {
        viewModel.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    // 保存完成：编辑已有规则时提示是否重扫未匹配短信（edge-cases 三）；新增直接返回
    LaunchedEffect(Unit) {
        viewModel.savedEvents.collect { wasEdit ->
            if (wasEdit) rescanDialogVisible = true else onBack()
        }
    }

    Scaffold(
        topBar = {
            // 自定义顶栏要自己处理状态栏 inset（targetSdk 36 强制 edge-to-edge）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = if (state.ruleId == 0L) "新增匹配规则" else "编辑匹配规则",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        bottomBar = {
            // 底部固定保存大按钮（落地方案：大圆角、主色填充、白字）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.saveRule() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("保存规则", color = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 分词区：固定在顶部不滚动（真·吸顶，两区布局实现，不依赖 stickyHeader）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "点击词块填入前后缀 · 当前选中：${activeFieldLabel(state.activeField)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            modifier = Modifier.weight(1f)
                        )
                        // 已粘贴短信后提供「重新粘贴」出口，否则文本永久锁死
                        if (state.smsText.isNotEmpty()) {
                            TextButton(onClick = {
                                selectedToken = null
                                viewModel.updateSmsText("")
                            }) {
                                Text("重新粘贴", style = MaterialTheme.typography.bodySmall, color = Primary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.smsText.isEmpty()) {
                        OutlinedTextField(
                            value = state.smsText,
                            onValueChange = { viewModel.updateSmsText(it) },
                            placeholder = { Text("从短信 App 复制一条快递短信，粘贴到这里...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = MaterialTheme.shapes.large
                        )
                    } else {
                        TokenGrid(
                            smsText = state.smsText,
                            preview = state.preview,
                            selectedToken = selectedToken,
                            onTokenClick = { token ->
                                selectedToken = token
                                viewModel.setTokenAsAnchor(token)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 下方滚动区：规则名 + 字段卡片 + 实时预览
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = state.ruleName,
                        onValueChange = { viewModel.updateRuleName(it) },
                        label = { Text("快递公司名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }

                item {
                    Text(
                        text = "提取字段",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }

                item {
                    FieldCard(
                        fieldName = "📦 快递单号",
                        dotColor = BrandColors.SF,
                        state = fieldState(state, ActiveField.TRACKING),
                        prefix = state.trackingPrefix,
                        suffix = state.trackingSuffix,
                        onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                        onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                        onClick = { viewModel.setActiveField(ActiveField.TRACKING) }
                    )
                }
                item {
                    FieldCard(
                        fieldName = "🔑 取件码",
                        dotColor = Primary,
                        state = fieldState(state, ActiveField.PICKUP_CODE),
                        prefix = state.codePrefix,
                        suffix = state.codeSuffix,
                        onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                        onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                        onClick = { viewModel.setActiveField(ActiveField.PICKUP_CODE) }
                    )
                }
                item {
                    FieldCard(
                        fieldName = "📍 取件地址",
                        dotColor = BrandColors.ZTO,
                        state = fieldState(state, ActiveField.ADDRESS),
                        prefix = state.addressPrefix,
                        suffix = state.addressSuffix,
                        onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                        onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                        onClick = { viewModel.setActiveField(ActiveField.ADDRESS) }
                    )
                }
                item {
                    FieldCard(
                        fieldName = "📱 快递员电话（可选）",
                        dotColor = BrandColors.EMS,
                        state = fieldState(state, ActiveField.PHONE),
                        prefix = state.phonePrefix,
                        suffix = state.phoneSuffix,
                        onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                        onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                        onClick = { viewModel.setActiveField(ActiveField.PHONE) }
                    )
                }

                // 实时预览：直接复用列表页的 PackageCard，样式完全一致
                val preview = state.preview
                if (preview != null) {
                    item {
                        Text(
                            text = "实时预览",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                    item {
                        PackageCard(
                            record = PackageRecord(
                                companyName = state.ruleName.ifEmpty { "预览" },
                                trackingNumber = preview.trackingNumber,
                                pickupCode = preview.pickupCode,
                                address = preview.address,
                                phone = preview.phone,
                                smsBody = state.smsText,
                                smsTimestamp = System.currentTimeMillis(),
                                isIncomplete = false
                            ),
                            onTogglePickedUp = {},
                            onCopyField = {}
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    if (rescanDialogVisible) {
        ConfirmDialog(
            title = "规则已保存",
            message = "要用新规则重新扫描之前没匹配到的短信吗？",
            confirmText = "重新扫描",
            onConfirm = {
                viewModel.rescanUnmatched()
                rescanDialogVisible = false
                onBack()
            },
            onDismiss = {
                rescanDialogVisible = false
                onBack()
            }
        )
    }
}

@Composable
private fun TokenGrid(
    smsText: String,
    preview: com.quickpick.domain.model.ExtractedFields?,
    selectedToken: String?,
    onTokenClick: (String) -> Unit
) {
    // 简单分词：按标点和空格拆分，保留标点作为独立 token
    val tokens = remember(smsText) {
        val result = mutableListOf<String>()
        var current = ""
        for (ch in smsText) {
            if (ch in "【】《》（）()，。；、：！？,.!? \n\t") {
                if (current.isNotEmpty()) { result.add(current); current = "" }
                if (!ch.isWhitespace()) result.add(ch.toString())
            } else {
                current += ch
            }
        }
        if (current.isNotEmpty()) result.add(current)
        result
    }

    val matchedValues = remember(preview) {
        listOfNotNull(preview?.trackingNumber, preview?.pickupCode, preview?.address, preview?.phone)
    }

    // FlowRow 在 Compose foundation 1.7+ 已是稳定 API（BOM 2026.06 无需 @OptIn）
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tokens.forEach { token ->
            // 命中判定：token 被任一已提取字段值包含（多 token 的长字段也能高亮）
            val isMatched = matchedValues.any { value -> value.contains(token) }
            TokenChip(
                text = token,
                state = when {
                    token == selectedToken -> TokenState.SELECTED
                    isMatched -> TokenState.MATCHED
                    else -> TokenState.NORMAL
                },
                onClick = { onTokenClick(token) }
            )
        }
    }
}

private fun fieldState(state: EditorState, field: ActiveField): FieldCardState = when {
    state.activeField == field -> FieldCardState.ACTIVE
    isFieldConfigured(state, field) -> FieldCardState.CONFIGURED
    else -> FieldCardState.EMPTY
}

private fun isFieldConfigured(state: EditorState, field: ActiveField): Boolean = when (field) {
    ActiveField.TRACKING -> state.trackingPrefix.isNotEmpty() || state.trackingSuffix.isNotEmpty()
    ActiveField.PICKUP_CODE -> state.codePrefix.isNotEmpty() || state.codeSuffix.isNotEmpty()
    ActiveField.ADDRESS -> state.addressPrefix.isNotEmpty() || state.addressSuffix.isNotEmpty()
    ActiveField.PHONE -> state.phonePrefix.isNotEmpty() || state.phoneSuffix.isNotEmpty()
}

private fun activeFieldLabel(field: ActiveField): String = when (field) {
    ActiveField.TRACKING -> "单号"
    ActiveField.PICKUP_CODE -> "取件码"
    ActiveField.ADDRESS -> "地址"
    ActiveField.PHONE -> "电话"
}
