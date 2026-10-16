# Phase 5c：AI 提示词页 + 设置页 + 权限引导页

> 所属轮次：第 4 轮 ｜ 前置：Phase 2-4
> 本文档为可执行清单：按 Task 顺序照做即可，每个 Step 含完整代码。

---

### Task 5.6: AiPromptViewModel + AiPromptScreen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/viewmodel/AiPromptViewModel.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/AiPromptScreen.kt`

- [ ] **Step 1: 创建 AiPromptViewModel.kt**

```kotlin
package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.quickpick.util.JsonExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AiPromptViewModel(application: Application) : AndroidViewModel(application) {

    private val _smsText = MutableStateFlow("")
    val smsText: StateFlow<String> = _smsText

    private val _generatedPrompt = MutableStateFlow("")
    val generatedPrompt: StateFlow<String> = _generatedPrompt

    private val _importedJson = MutableStateFlow("")
    val importedJson: StateFlow<String> = _importedJson

    fun updateSmsText(text: String) {
        _smsText.value = text
    }

    fun generatePrompt() {
        val sms = _smsText.value
        if (sms.isBlank()) return

        _generatedPrompt.value = PROMPT_TEMPLATE.replace("{{SMS_CONTENT}}", sms)
    }

    fun updateImportedJson(json: String) {
        _importedJson.value = json
    }

    /**
     * 校验用户粘贴的 AI 返回内容。
     * 先剥离 markdown 围栏等杂质，合法则返回清理后的 JSON 字符串，否则 null。
     * （字符串传递到编辑器，避开 Parcelable 和进程重建问题）
     */
    fun validatedJson(): String? {
        val json = _importedJson.value
        if (json.isBlank()) return null
        val clean = JsonExporter.sanitizeAiJson(json)
        val result = JsonExporter.import(clean)
        return if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) clean else null
    }

    companion object {
        private val PROMPT_TEMPLATE = """
你是一个快递短信解析专家。我会给你一条快递取件通知短信，请你分析短信内容，
为每个字段提取"前缀"和"后缀"锚点，输出 JSON 格式的提取规则。

字段说明：
- trackingNumber: 快递单号（运单号，如 SF1234567890）
- pickupCode: 取件码 / 快递柜号 / 货架号（取件凭证）
- address: 取件地址 / 驿站名 / 快递柜位置
- phone: 快递员或客服电话（短信中有则提取，无则留空）

规则格式：
每个字段用 prefix（前缀固定文本）和 suffix（后缀固定文本）定位。
提取逻辑：在短信中找到 prefix，取其后内容，直到 suffix 截止。
prefix 和 suffix 尽量取最短、最能唯一定位的文本。
如果短信中没有该字段，prefix 和 suffix 都留空字符串 ""。

输出严格 JSON，不要任何解释：
{
  "name": "快递公司名",
  "fields": {
    "trackingNumber": { "prefix": "单号", "suffix": "，" },
    "pickupCode":    { "prefix": "取件码：", "suffix": "，" },
    "address":       { "prefix": "到达", "suffix": "，" },
    "phone":         { "prefix": "电话：", "suffix": "" }
  }
}

短信内容：
---
{{SMS_CONTENT}}
---

请输出 JSON：
        """.trimIndent()
    }
}
```

- [ ] **Step 2: 创建 AiPromptScreen.kt**

```kotlin
package com.quickpick.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.AiPromptViewModel

@Composable
fun AiPromptScreen(
    viewModel: AiPromptViewModel,
    onImportRule: (json: String, smsText: String) -> Unit,
    onBack: () -> Unit
) {
    val smsText by viewModel.smsText.collectAsStateWithLifecycle()
    val prompt by viewModel.generatedPrompt.collectAsStateWithLifecycle()
    val importedJson by viewModel.importedJson.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            // 自定义顶栏要自己处理状态栏 inset（targetSdk 36 强制 edge-to-edge）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "AI 生成规则",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Step 1: 粘贴短信
            Text(
                text = "第 1 步：粘贴短信",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = smsText,
                onValueChange = { viewModel.updateSmsText(it) },
                placeholder = { Text("从短信 App 复制一条快递短信粘贴到这里...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 生成按钮
            Button(
                onClick = { viewModel.generatePrompt() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = MaterialTheme.shapes.large,
                enabled = smsText.isNotBlank()
            ) {
                Text("生成提示词", color = Color.White)
            }

            if (prompt.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                // Step 2: 复制提示词
                Text(
                    text = "第 2 步：复制提示词并去 AI 网站提问",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = InputBackground)
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        copyToClipboard(context, prompt)
                        Toast.makeText(context, "提示词已复制，去 AI 网站粘贴提问吧", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("📋 一键复制提示词", color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step 3: 导入 JSON
                Text(
                    text = "第 3 步：粘贴 AI 返回的 JSON",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = importedJson,
                    onValueChange = { viewModel.updateImportedJson(it) },
                    placeholder = { Text("将从 AI 网站获取的 JSON 结果粘贴到这里...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    shape = MaterialTheme.shapes.large
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // 温和文案：不把 Gson 英文异常原文漏给用户
                        val clean = viewModel.validatedJson()
                        if (clean != null) {
                            onImportRule(clean, smsText)
                        } else {
                            Toast.makeText(context, "没看懂这段内容，检查一下再粘贴试试～", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.large,
                    enabled = importedJson.isNotBlank()
                ) {
                    Text("导入并打开规则编辑器 →", color = Color.White)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("", text))
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/viewmodel/AiPromptViewModel.kt app/src/main/java/com/quickpick/ui/screen/AiPromptScreen.kt && git commit -m "feat: add AiPromptViewModel and AiPromptScreen"
```

---

### Task 5.7: SettingsScreen + OnboardingScreen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/viewmodel/SettingsViewModel.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/SettingsScreen.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/OnboardingScreen.kt`

- [ ] **Step 1: 创建 SettingsViewModel.kt（导入/导出/同名冲突裁决）**

```kotlin
package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.domain.model.MatchRule
import com.quickpick.util.JsonExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val ruleRepo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )

    val rules: StateFlow<List<MatchRule>> = ruleRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events

    /** 待裁决的同名冲突（逐条 覆盖/跳过） */
    data class ImportConflict(val incoming: MatchRule, val existingId: Long)

    private val _conflict = MutableStateFlow<ImportConflict?>(null)
    val conflict: StateFlow<ImportConflict?> = _conflict

    private var pendingImport = mutableListOf<MatchRule>()
    private var importedCount = 0
    private var skippedCount = 0

    /** 导出全部规则 JSON；为空时提示并返回 null（edge-cases 四） */
    fun exportAllJson(): String? {
        val list = rules.value
        if (list.isEmpty()) {
            _events.tryEmit("暂无规则可导出")
            return null
        }
        return JsonExporter.exportAll(list)
    }

    /** 导入 JSON：无同名直接新增；有同名进入冲突队列逐条裁决（覆盖/跳过） */
    fun importRules(json: String) {
        viewModelScope.launch {
            val result = JsonExporter.import(JsonExporter.sanitizeAiJson(json))
            val list = result.getOrNull()
            if (list == null) {
                _events.tryEmit("文件格式不太对，请选择规则 JSON 文件")
                return@launch
            }
            importedCount = 0
            skippedCount = 0
            pendingImport = list.toMutableList()
            processNext()
        }
    }

    private suspend fun processNext() {
        val next = pendingImport.removeFirstOrNull()
        if (next == null) {
            _events.tryEmit(
                "导入完成：新增 $importedCount 条" +
                    if (skippedCount > 0) "，跳过 $skippedCount 条" else ""
            )
            return
        }
        val existing = ruleRepo.getByName(next.name)
        if (existing.isEmpty()) {
            ruleRepo.insert(next)
            importedCount++
            processNext()
        } else {
            _conflict.value = ImportConflict(next, existing.first().id)
        }
    }

    /** 冲突裁决：true=覆盖已有规则，false=跳过 */
    fun resolveConflict(overwrite: Boolean) {
        val c = _conflict.value ?: return
        _conflict.value = null
        viewModelScope.launch {
            if (overwrite) {
                // update 内部会保留原 isPreset / createdAt（见 RuleRepository）
                ruleRepo.update(c.incoming.copy(id = c.existingId))
                importedCount++
            } else {
                skippedCount++
            }
            processNext()
        }
    }
}
```

- [ ] **Step 2: 创建 SettingsScreen.kt**

```kotlin
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

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    packageViewModel: PackageListViewModel,
    onOpenAiPrompt: () -> Unit
) {
    val context = LocalContext.current
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
            val json = settingsViewModel.exportAllJson()
            if (json != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                context.startActivity(Intent.createChooser(intent, "导出规则"))
            }
        })
        // MIME 用 */*：部分国产 ROM 文件管理器不认 application/json 会选不到文件
        SettingsRow("📥 导入规则", onClick = { importLauncher.launch("*/*") })

        Spacer(modifier = Modifier.height(16.dp))

        // 关于
        SectionTitle("关于")
        SettingsRow("版本号", subtitle = BuildConfig.VERSION_NAME, onClick = {})
        SettingsRow("项目地址", subtitle = "github.com/quick-pick", onClick = {})

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
```

- [ ] **Step 3: 创建 OnboardingScreen.kt**

```kotlin
package com.quickpick.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickpick.ui.theme.*

@Composable
fun OnboardingScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    // 用 var + remember：授权结果和从系统设置返回都要能刷新状态（初版 remember 不刷新是死状态）
    var hasPermission by remember { mutableStateOf(checkSmsPermission(context)) }

    // 从系统设置页返回时重新检查
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkSmsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        // 「授权并开始使用」即授权即进入（spec 验收 1）；被拒则界面保留「去系统设置开启」入口
        if (granted) onPermissionGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📦", style = MaterialTheme.typography.displayLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "欢迎使用取件宝",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "我们需要读取您的短信，来自动提取快递取件信息。\n\n" +
                   "✅ 所有数据仅存储在您的设备本地\n" +
                   "✅ 不上传任何信息到服务器\n" +
                   "✅ 不收集任何隐私数据",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        if (hasPermission) {
            Button(
                onClick = onPermissionGranted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = MaterialTheme.shapes.large
            ) {
                Text("开始使用", color = Color.White)
            }
        } else {
            Button(
                onClick = { launcher.launch(Manifest.permission.READ_SMS) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = MaterialTheme.shapes.large
            ) {
                Text("授权并开始使用", color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 永久拒绝时的出口：跳本应用系统设置页
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }) {
                Text("去系统设置开启", color = TextSecondary)
            }
        }
    }
}

private fun checkSmsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/screen/SettingsScreen.kt app/src/main/java/com/quickpick/ui/screen/OnboardingScreen.kt app/src/main/java/com/quickpick/ui/viewmodel/SettingsViewModel.kt && git commit -m "feat: add Settings (import/export/conflict) + Onboarding with permission flow"
```

---

