package com.quickpick.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.component.*
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.ActiveField
import com.quickpick.ui.viewmodel.EditorState
import com.quickpick.ui.viewmodel.RuleEditorViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

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
                    // 返回箭头字形：原依赖 titleLarge 内嵌色，T6 起显式传色
                    Text("←", style = MiuixTheme.textStyles.title4, color = TextPrimary)
                }
                Text(
                    text = if (state.ruleId == 0L) "新增匹配规则" else "编辑匹配规则",
                    style = MiuixTheme.textStyles.headline1,
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
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("保存规则", color = Color.White)
                }
            }
        },
        containerColor = Background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 分词区：固定在顶部不滚动（真·吸顶，两区布局实现，不依赖 stickyHeader）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                cornerRadius = 16.dp,
                colors = CardDefaults.defaultColors(color = Surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "点击词块填入前后缀 · 当前选中：${activeFieldLabel(state.activeField)}",
                            style = MiuixTheme.textStyles.body2,
                            color = TextTertiary,
                            modifier = Modifier.weight(1f)
                        )
                        // 已粘贴短信后提供「重新粘贴」出口，否则文本永久锁死
                        if (state.smsText.isNotEmpty()) {
                            InlineTextButton(
                                text = "重新粘贴",
                                onClick = {
                                    selectedToken = null
                                    viewModel.updateSmsText("")
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.smsText.isEmpty()) {
                        TextField(
                            value = state.smsText,
                            onValueChange = { viewModel.updateSmsText(it) },
                            label = "从短信 App 复制一条快递短信，粘贴到这里...",
                            useLabelAsPlaceholder = true,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            colors = InputFieldColors
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
                    TextField(
                        value = state.ruleName,
                        onValueChange = { viewModel.updateRuleName(it) },
                        label = "快递公司名",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = InputFieldColors
                    )
                }

                item {
                    Text(
                        text = "提取字段",
                        style = MiuixTheme.textStyles.body2,
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
                item {
                    // senderPattern 是单字符串匹配（对发件方号码做包含过滤），非前后缀锚点对 → 简化版单输入卡片
                    SingleFieldCard(
                        fieldName = "📮 发件方过滤",
                        dotColor = BrandColors.YTO,
                        state = fieldState(state, ActiveField.SENDER),
                        value = state.senderPattern,
                        placeholder = "发件方号码包含（如 1069）",
                        onValueChange = { viewModel.updateActiveFieldPrefix(it) },
                        onClick = { viewModel.setActiveField(ActiveField.SENDER) }
                    )
                }
                item {
                    FieldCard(
                        fieldName = "🏷 品牌提取",
                        dotColor = BrandColors.CAINIAO,
                        state = fieldState(state, ActiveField.BRAND),
                        prefix = state.brandPrefix,
                        suffix = state.brandSuffix,
                        onPrefixChange = { viewModel.updateActiveFieldPrefix(it) },
                        onSuffixChange = { viewModel.updateActiveFieldSuffix(it) },
                        onClick = { viewModel.setActiveField(ActiveField.BRAND) }
                    )
                }

                // 实时预览：直接复用列表页的 PackageCard，样式完全一致
                val preview = state.preview
                if (preview != null) {
                    item {
                        Text(
                            text = "实时预览",
                            style = MiuixTheme.textStyles.body2,
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
                                smsTimestamp = System.currentTimeMillis()
                            ),
                            onTogglePickedUp = {},
                            onCopyField = {}
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // OverlayDialog 必须组合在 miuix Scaffold 子树内（弹窗由 Scaffold 的 popupHost 渲染），
            // 放在 content lambda 里而不是与 Scaffold 平级，否则注册不到 popupHost、弹窗不显示
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
    ActiveField.SENDER -> state.senderPattern.isNotEmpty()
    ActiveField.BRAND -> state.brandPrefix.isNotEmpty() || state.brandSuffix.isNotEmpty()
}

private fun activeFieldLabel(field: ActiveField): String = when (field) {
    ActiveField.TRACKING -> "单号"
    ActiveField.PICKUP_CODE -> "取件码"
    ActiveField.ADDRESS -> "地址"
    ActiveField.PHONE -> "电话"
    ActiveField.SENDER -> "发件方"
    ActiveField.BRAND -> "品牌"
}

/**
 * 单输入字段卡片（senderPattern 专用）：视觉与 [FieldCard] 一致（选中抬高、左侧强调线、状态徽标），
 * 但只有一个输入框——发件方过滤是单字符串包含匹配，不分前后缀锚点对。
 */
@Composable
private fun SingleFieldCard(
    fieldName: String,
    dotColor: Color,
    state: FieldCardState,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit
) {
    val elevation = if (state == FieldCardState.ACTIVE) 6.dp else 2.dp
    val offsetX = if (state == FieldCardState.ACTIVE) 2.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = offsetX)
            .shadow(elevation, RoundedCornerShape(16.dp)),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = if (state == FieldCardState.EMPTY) Surface.copy(alpha = 0.7f) else Surface
        ),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // 左侧强调色线：与 FieldCard 同款
            if (state != FieldCardState.EMPTY) {
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp, top = 14.dp, bottom = 14.dp)
                        .fillMaxHeight()
                        .width(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Primary)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fieldName,
                        style = MiuixTheme.textStyles.body1,
                        color = if (state == FieldCardState.ACTIVE) Primary else TextPrimary,
                        fontWeight = if (state == FieldCardState.ACTIVE)
                            FontWeight.SemiBold
                        else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = when (state) {
                            FieldCardState.CONFIGURED -> "已配置"
                            FieldCardState.ACTIVE -> "编辑中"
                            FieldCardState.EMPTY -> "可选"
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = when (state) {
                            FieldCardState.CONFIGURED -> Done
                            FieldCardState.ACTIVE -> Primary
                            FieldCardState.EMPTY -> TextTertiary
                        }
                    )
                }

                if (state != FieldCardState.EMPTY || value.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = placeholder,
                        useLabelAsPlaceholder = true,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MiuixTheme.textStyles.body1,
                        colors = InputFieldColors
                    )
                }
            }
        }
    }
}
