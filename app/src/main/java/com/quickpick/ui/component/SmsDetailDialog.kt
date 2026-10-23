package com.quickpick.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.quickpick.domain.model.PackageRecord
import com.quickpick.ui.theme.InputBackground
import com.quickpick.ui.theme.Primary
import com.quickpick.ui.theme.TextPrimary
import com.quickpick.ui.theme.TextSecondary
import com.quickpick.ui.theme.TextTertiary
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 短信原文弹窗（v1.1.6 替代跳系统短信应用——OEM 会话 URI 兼容性差，用户裁决不跳了）。
 * 发件人 + 时间 + 正文，一键复制全部。限高 + 内容滚动（弹窗规范）。
 * 常驻组合 + show 参数（退出动画约定同 ConfirmDialog）。
 */
@Composable
fun SmsDetailDialog(
    show: Boolean,
    record: PackageRecord?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    OverlayDialog(
        show = show,
        title = "短信原文",
        summary = null,
        onDismissRequest = onDismiss
    ) {
        if (record == null) return@OverlayDialog
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 发件人 + 时间
            Text(
                text = "发件人：${record.smsSender ?: "未知（老记录，重扫后回填）"}",
                style = MiuixTheme.textStyles.body2,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "时间：" + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                    .format(Date(record.smsTimestamp)),
                style = MiuixTheme.textStyles.body2,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            // 正文（等宽，可滚动）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InputBackground)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = record.smsBody,
                    style = MiuixTheme.textStyles.body2,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = "📋 复制全文",
                onClick = {
                    val full = buildString {
                        append("发件人：").append(record.smsSender ?: "未知").append('\n')
                        append("时间：").append(
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                                .format(Date(record.smsTimestamp))
                        ).append('\n')
                        append(record.smsBody)
                    }
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("sms-detail", full))
                    Toast.makeText(context, "已复制～", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}
