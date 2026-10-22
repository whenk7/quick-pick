package com.quickpick.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.quickpick.ui.component.ConfirmDialog
import com.quickpick.ui.component.glass.glassNavBarClearance
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
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
    onOpenAiPrompt: () -> Unit,
    onOpenTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conflict by settingsViewModel.conflict.collectAsStateWithLifecycle()
    val scanWindowDays by settingsViewModel.scanWindowDays.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 导入规则：系统文件选择器（spec 七）
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()
            if (text != null) {
                settingsViewModel.importRules(text)
            } else {
                Toast.makeText(context, "没能读取这个文件，换一个试试～", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        // 快捷操作（「全部已取」入口在列表页，带二次确认；按天已取在各分组标题右侧）
        SectionTitle("快捷操作")
        SettingsRow("🤖 AI 生成规则", onClick = onOpenAiPrompt)

        Spacer(modifier = Modifier.height(16.dp))

        // 规则管理
        SectionTitle("规则管理")
        SettingsRow("📤 导出全部规则", onClick = {
            // 导出走系统分享面板（本地 ACTION_SEND，不涉及任何网络）
            scope.launch {
                val json = settingsViewModel.exportAllJson()
                if (json != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, json)
                    }
                    context.startActivity(Intent.createChooser(intent, "导出规则"))
                }
            }
        })
        // MIME 用 */*：部分国产 ROM 文件管理器不认 application/json 会选不到文件
        SettingsRow("📥 导入规则", onClick = { importLauncher.launch("*/*") })

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

    // 导入同名冲突：逐条裁决 覆盖/跳过（edge-cases 四）
    // 常驻组合 + visible 参数：退出动画由 OverlayDialog 的 show 状态驱动，条件组合会跳过它。
    // 记住最近的冲突名：conflict 置 null 后的退场动画期间文案不闪空
    var lastConflictName by remember { mutableStateOf("") }
    conflict?.let { lastConflictName = it.incoming.name }
    ConfirmDialog(
        visible = conflict != null,
        title = "发现同名规则",
        message = "已存在「$lastConflictName」，要覆盖它吗？",
        confirmText = "覆盖",
        dismissText = "跳过",
        onConfirm = { settingsViewModel.resolveConflict(true) },
        onDismiss = { settingsViewModel.resolveConflict(false) }
    )
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
