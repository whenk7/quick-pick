package com.quickpick.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.BuildConfig
import com.quickpick.ui.component.glass.glassNavBarClearance
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 扫描窗口档位（天 → 显示名） */
private val SCAN_WINDOW_LABELS = listOf(7 to "一周", 30 to "一月", 90 to "三月", 180 to "半年")

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onOpenTheme: () -> Unit
) {
    val context = LocalContext.current
    val scanWindowDays by settingsViewModel.scanWindowDays.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // 底部留白给悬浮玻璃底栏（inset 动态）
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = glassNavBarClearance())
    ) {
        Text(
            text = "设置",
            style = MiuixTheme.textStyles.title2,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 短信扫描窗口：scanSms 按此值算 cutoff（默认 7 天）
        SectionTitle("短信扫描")
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            WindowSpinnerPreference(
                title = "扫描范围",
                summary = "只扫描该时间段内的短信",
                items = SCAN_WINDOW_LABELS.map { DropdownItem(title = it.second) },
                selectedIndex = SCAN_WINDOW_LABELS.indexOfFirst { it.first == scanWindowDays }.coerceAtLeast(0),
                onSelectedIndexChange = { settingsViewModel.setScanWindowDays(SCAN_WINDOW_LABELS[it].first) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 主题与外观：独立页面（模式/引擎/种子色/风格/规格 + 玻璃底栏调参）
        SectionTitle("外观")
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            ArrowPreference(
                title = "主题与外观",
                summary = "主题模式、配色引擎、玻璃底栏",
                onClick = onOpenTheme
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 关于
        SectionTitle("关于")
        SettingsRow("版本号", subtitle = BuildConfig.VERSION_NAME, onClick = {})
        SettingsRow("项目地址", subtitle = "github.com/whenk7/quick-pick", onClick = {
            // 无浏览器等设备差异下温和兜底，不崩溃
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/whenk7/quick-pick"))
                )
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "没找到能打开链接的应用～", Toast.LENGTH_SHORT).show()
            }
        })

        Spacer(modifier = Modifier.height(24.dp))

        // 合规声明（规格要求的两处之一；另一处在权限引导页）
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(color = PrimarySoft)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("🛡", style = MiuixTheme.textStyles.title4)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "所有数据仅存储在本地，不上传、不联网、不收集任何隐私信息。",
                    style = MiuixTheme.textStyles.body2,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.body2,
        color = TextTertiary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.main,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (subtitle != null) {
            Text(text = subtitle, style = MiuixTheme.textStyles.body2, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("→", color = TextTertiary)
    }
    HorizontalDivider(color = Divider)
}
