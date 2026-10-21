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
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onOpenAiPrompt: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conflict by settingsViewModel.conflict.collectAsStateWithLifecycle()
    val scanWindowDays by settingsViewModel.scanWindowDays.collectAsStateWithLifecycle()
    val colorMode by settingsViewModel.colorMode.collectAsStateWithLifecycle()
    val useMonet by settingsViewModel.useMonet.collectAsStateWithLifecycle()
    val paletteStyle by settingsViewModel.paletteStyle.collectAsStateWithLifecycle()

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
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MiuixTheme.textStyles.title2,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 短信扫描窗口：scanSms 按此值算 cutoff（默认 7 天）
        SectionTitle("短信扫描")
        listOf(7 to "一周", 30 to "一月", 90 to "三月", 180 to "半年").forEach { (days, label) ->
            OptionRow(
                label = label,
                selected = scanWindowDays == days,
                onClick = { settingsViewModel.setScanWindowDays(days) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 主题：明暗三选一 + Monet 开关 + 取色风格（Monet 开启时显示）
        SectionTitle("主题")
        listOf("SYSTEM" to "跟随系统", "LIGHT" to "浅色", "DARK" to "深色").forEach { (mode, label) ->
            OptionRow(
                label = label,
                selected = colorMode == mode,
                onClick = { settingsViewModel.setColorMode(mode) }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "动态取色",
                    style = MiuixTheme.textStyles.main,
                    color = TextPrimary
                )
                Text(
                    text = "以豆绿为种子，生成随系统深浅的配色",
                    style = MiuixTheme.textStyles.body2,
                    color = TextTertiary
                )
            }
            Switch(
                checked = useMonet,
                onCheckedChange = { settingsViewModel.setUseMonet(it) }
            )
        }
        HorizontalDivider(color = Divider)
        if (useMonet) {
            listOf(
                "TonalSpot" to "标准（TonalSpot）",
                "Vibrant" to "鲜艳（Vibrant）",
                "Expressive" to "表现力（Expressive）"
            ).forEach { (style, label) ->
                OptionRow(
                    label = label,
                    selected = paletteStyle == style,
                    onClick = { settingsViewModel.setPaletteStyle(style) }
                )
            }
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
    conflict?.let { c ->
        ConfirmDialog(
            title = "发现同名规则",
            message = "已存在「${c.incoming.name}」，要覆盖它吗？",
            confirmText = "覆盖",
            dismissText = "跳过",
            onConfirm = { settingsViewModel.resolveConflict(true) },
            onDismiss = { settingsViewModel.resolveConflict(false) }
        )
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

/** 通用单选选项行（扫描窗口/明暗/取色风格）：选中态用 Primary 色 + ✓ 标记 */
@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
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
            text = label,
            style = MiuixTheme.textStyles.main,
            color = if (selected) Primary else TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Text("✓", style = MiuixTheme.textStyles.main, color = Primary)
        }
    }
    HorizontalDivider(color = Divider)
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
