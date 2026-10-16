package com.quickpick.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.BuildConfig
import com.quickpick.ui.component.ConfirmDialog
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.PackageListViewModel
import com.quickpick.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    packageViewModel: PackageListViewModel,
    onOpenAiPrompt: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conflict by settingsViewModel.conflict.collectAsStateWithLifecycle()
    var confirmAllVisible by remember { mutableStateOf(false) }

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
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 快捷操作（按天已取在列表页各分组标题右侧，这里不重复）
        SectionTitle("快捷操作")
        SettingsRow("✓ 一键全部已取", onClick = { confirmAllVisible = true })
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
        SettingsRow("项目地址", subtitle = "github.com/whenk7/quick-pick", onClick = {})

        Spacer(modifier = Modifier.height(24.dp))

        // 合规声明（规格要求的两处之一；另一处在权限引导页）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = PrimarySoft)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("🛡", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "所有数据仅存储在本地，不上传、不联网、不收集任何隐私信息。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (confirmAllVisible) {
        ConfirmDialog(
            title = "全部标记为已取？",
            message = "把所有待取快递标记为已取件。",
            onConfirm = {
                packageViewModel.markAllPickedUp()
                confirmAllVisible = false
            },
            onDismiss = { confirmAllVisible = false }
        )
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
        style = MaterialTheme.typography.bodySmall,
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
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("→", color = TextTertiary)
    }
    HorizontalDivider(color = Divider)
}
