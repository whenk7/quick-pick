package com.quickpick.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.quickpick.ui.theme.InputBackground
import com.quickpick.ui.theme.TextPrimary
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 规则 JSON 预览弹窗（miuix OverlayDialog 实现）。
 * 调用方式与 [ConfirmDialog] 相同：**常驻组合**（不要 if (show) 条件包裹），
 * 显隐交给 [show]——退出动画由 OverlayDialog 内部状态驱动，条件组合会跳过它。
 * 内容：浅灰圆角容器内等宽字体 JSON（LazyColumn 可滚动，maxHeight 封顶），底部「复制」按钮。
 */
@Composable
fun JsonPreviewDialog(
    show: Boolean,
    json: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // 按行拆给 LazyColumn：超长 JSON 不压单个大 Text；空行占位一个空格防止行高塌陷
    val lines = remember(json) { json.lines().map { it.ifEmpty { " " } } }

    // v1.1.7 §2：OverlayDialog 不消费返回键，自己拦——返回 = 关弹窗
    BackHandler(enabled = show) { onDismiss() }

    OverlayDialog(
        show = show,
        title = "规则 JSON",
        summary = null,
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InputBackground),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                items(lines) { line ->
                    Text(
                        text = line,
                        style = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = "复制",
                onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("rule-json", json))
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}
