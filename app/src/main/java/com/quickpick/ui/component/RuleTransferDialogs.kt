package com.quickpick.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.MatchRule
import com.quickpick.ui.theme.InputFieldColors
import com.quickpick.ui.theme.TextSecondary
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 导入规则弹窗：粘贴 JSON / 选文件 → 解析预览 → 确认导入。
 * 限高 + 内容滚动（弹窗规范：高度不超过内容需要，长内容内部滚）。
 * 常驻组合 + show 参数（退出动画约定同 ConfirmDialog）。
 */
@Composable
fun ImportRulesDialog(
    show: Boolean,
    onPickFile: () -> Unit,
    onParse: (String) -> List<MatchRule>?,
    onConfirmImport: (List<MatchRule>) -> Unit,
    onPasteClipboard: () -> String?,
    onDismiss: () -> Unit
) {
    val fieldState = rememberTextFieldState()
    // 解析预览：null=未解析/解析失败，非 null=可导入列表
    var parsed by remember(show) { mutableStateOf<List<MatchRule>?>(null) }
    var parseFailed by remember(show) { mutableStateOf(false) }

    OverlayDialog(
        show = show,
        title = "导入规则",
        summary = "支持单个 JSON 或 JSON 数组",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                state = fieldState,
                modifier = Modifier.fillMaxWidth(),
                label = "粘贴规则 JSON",
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6),
                colors = InputFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                TextButton(
                    text = "📋 粘贴",
                    onClick = {
                        onPasteClipboard()?.let { fieldState.edit { replace(0, length, it) } }
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    text = "📁 选文件",
                    onClick = onPickFile,
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (parseFailed) {
                Text(
                    text = "没解析出来——检查下是不是完整的规则 JSON",
                    style = MiuixTheme.textStyles.body2,
                    color = TextSecondary
                )
            }
            parsed?.let { list ->
                Text(
                    text = "识别到 ${list.size} 条：" + list.joinToString("、") { it.name },
                    style = MiuixTheme.textStyles.body2,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        val list = onParse(fieldState.text.toString())
                        if (list == null) {
                            parsed = null
                            parseFailed = true
                        } else {
                            parseFailed = false
                            parsed = list
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("解析")
                }
            }
            parsed?.let { list ->
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onConfirmImport(list) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("确认导入 ${list.size} 条")
                }
            }
        }
    }
}

/**
 * 导出规则弹窗：复制到剪贴板 / 存为文件。限高同样收敛。
 */
@Composable
fun ExportRulesDialog(
    show: Boolean,
    onCopy: () -> Unit,
    onSaveFile: () -> Unit,
    onDismiss: () -> Unit
) {
    OverlayDialog(
        show = show,
        title = "导出规则",
        summary = "JSON 格式，可复制或存成文件",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Button(
                onClick = onCopy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text("📋 复制到剪贴板")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                text = "📁 存为文件",
                onClick = onSaveFile,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
