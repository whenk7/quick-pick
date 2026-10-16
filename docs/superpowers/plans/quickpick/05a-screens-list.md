# Phase 5a：快递列表页 + 规则列表页（ViewModel + Screen）

> 所属轮次：第 4 轮 ｜ 前置：Phase 2-4
> 本文档为可执行清单：按 Task 顺序照做即可，每个 Step 含完整代码。

---

## Phase 5: ViewModel + Screen

### Task 5.1: PackageListViewModel + Screen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/viewmodel/PackageListViewModel.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/PackageListScreen.kt`

- [ ] **Step 1: 创建 PackageListViewModel.kt**

```kotlin
package com.quickpick.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.PackageRepository
import com.quickpick.data.repository.RuleRepository
import com.quickpick.data.repository.SmsRepository
import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.model.PackageRecord
import com.quickpick.util.SmsReader
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PackageListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as QuickPickApp).database
    private val packageRepo = PackageRepository(db.packageRecordDao())
    private val ruleRepo = RuleRepository(db.matchRuleDao())
    private val smsRepo = SmsRepository(db.scannedSmsDao())

    val packages: StateFlow<List<PackageRecord>> = packageRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpickedCount: StateFlow<Int> = packageRepo.observeUnpickedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val ruleCount: StateFlow<Int> = ruleRepo.observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    /** 是否有短信权限。由 UI 在进入页面/回到前台时调用 refreshPermissionState() 刷新 */
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    /** 折叠的日期分组（当天 00:00 时间戳），会话内有效 */
    private val _collapsedDays = MutableStateFlow<Set<Long>>(emptySet())
    val collapsedDays: StateFlow<Set<Long>> = _collapsedDays

    /** 一次性提示（Toast） */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events

    /** UI 在进入页面与 ON_RESUME 时调用：刷新权限状态，有权限则增量扫描 */
    fun refreshPermissionState() {
        val granted = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        _hasPermission.value = granted
        if (granted) scanSms()
    }

    fun scanSms() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                // 权限判定前置：不再靠「读到空表」猜权限状态
                val granted = ContextCompat.checkSelfPermission(
                    getApplication(), Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                _hasPermission.value = granted
                if (!granted) return@launch

                val messages = SmsReader.readAll(getApplication())
                _scannedCount.value = smsRepo.scannedCount()

                val rules = ruleRepo.getAll()
                if (rules.isEmpty()) return@launch

                messages.forEach { sms ->
                    if (smsRepo.isScanned(sms.id)) return@forEach
                    val result = MatchEngine.findBestMatch(sms.body, rules)
                    if (result != null) {
                        val (rule, fields) = result
                        packageRepo.insertFromSms(
                            PackageRecord(
                                companyName = rule.name,
                                trackingNumber = fields.trackingNumber,
                                pickupCode = fields.pickupCode,
                                address = fields.address,
                                phone = fields.phone,
                                smsBody = sms.body,
                                smsTimestamp = sms.timestamp,
                                // 配置了但没提取到才算不完整；电话可选，不硬编码字段数
                                isIncomplete = fields.matchedCount < rule.configuredFieldCount,
                                ruleId = rule.id,
                                smsId = sms.id
                            )
                        )
                    }
                    smsRepo.markScanned(sms.id)
                }
                _scannedCount.value = smsRepo.scannedCount()
            } catch (e: SecurityException) {
                // 扫描过程中权限被系统回收
                _hasPermission.value = false
            } catch (e: Exception) {
                // 存储空间不足、数据库异常等：温和提示，不崩溃（edge-cases 六）
                _events.tryEmit("没能完成扫描，请稍后再试～")
            } finally {
                _isScanning.value = false
            }
        }
    }

    /** 手动全量重扫（下拉刷新）：清空已扫标记后重扫 */
    fun refresh() {
        viewModelScope.launch {
            smsRepo.clearScanned()
            scanSms()
        }
    }

    fun togglePickedUp(record: PackageRecord) {
        viewModelScope.launch {
            packageRepo.setPickedUp(record.id, !record.isPickedUp)
        }
    }

    fun markAllPickedUp() {
        viewModelScope.launch { packageRepo.markAllPickedUp() }
    }

    fun markDayPickedUp(dateMs: Long) {
        viewModelScope.launch { packageRepo.markDayPickedUp(dateMs) }
    }

    fun toggleDayCollapse(dateMs: Long) {
        _collapsedDays.value = _collapsedDays.value.let {
            if (dateMs in it) it - dateMs else it + dateMs
        }
    }
}
```

- [ ] **Step 2: 创建 PackageListScreen.kt**（分组、折叠、下拉重扫、权限引导、5 种空状态）

```kotlin
package com.quickpick.ui.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.component.*
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.PackageListViewModel

@Composable
fun PackageListScreen(
    viewModel: PackageListViewModel,
    onGoToRules: () -> Unit
) {
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val unpickedCount by viewModel.unpickedCount.collectAsStateWithLifecycle()
    val ruleCount by viewModel.ruleCount.collectAsStateWithLifecycle()
    val scannedCount by viewModel.scannedCount.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val collapsedDays by viewModel.collapsedDays.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmAllVisible by remember { mutableStateOf(false) }

    // 一次性事件 → Toast
    LaunchedEffect(Unit) {
        viewModel.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 进入页面 & 回到前台：刷新权限状态并增量扫描（新短信到达不靠后台推送，靠回前台时增量比对）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissionState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 权限请求：普通拒绝 → 留在空态可再点；永久拒绝（不再询问）→ 跳系统设置页
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.refreshPermissionState()
        } else {
            val activity = context as? android.app.Activity
            val permanentlyDenied = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.READ_SMS
            ) == false
            if (permanentlyDenied) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
        }
    }

    val groupedPackages = remember(packages) {
        packages.groupBy { normalizeToDayStart(it.smsTimestamp) }
            .toSortedMap(reverseOrder())
    }

    // 外层 MainScreen 的 Scaffold 已吃 systemBars inset，内层必须置零，否则上下双份留白
    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 标题栏
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("取件宝", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                if (hasPermission && packages.isNotEmpty()) {
                    Text(
                        text = "$unpickedCount 件待取",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // 权限被回收的横幅：已有数据仍可查看，只是不再更新（edge-cases 一）
            if (!hasPermission && packages.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = InputBackground)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "短信权限已关闭，新快递不会更新",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) {
                            Text("去开启", color = Primary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // 快捷操作：全部已取（无未取时置灰，edge-cases 五）
            if (hasPermission && packages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Button(
                        onClick = { confirmAllVisible = true },
                        enabled = unpickedCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✓ 全部已取", color = Color.White)
                    }
                }
            }

            when {
                // 权限被回收但已有数据 → 落到 else 列表分支，banner 压顶提示，数据可看
                !hasPermission && packages.isEmpty() -> {
                    EmptyState(
                        emoji = "📬",
                        title = "需要短信权限才能提取快递信息",
                        description = "所有数据仅存储在本地，不上传、不联网",
                        modifier = Modifier.weight(1f),
                        action = {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text("前往授权", color = Color.White)
                            }
                        }
                    )
                }
                isScanning && packages.isEmpty() -> {
                    EmptyState(
                        emoji = "🔍",
                        title = "扫描中…",
                        description = "正在读取快递短信",
                        modifier = Modifier.weight(1f)
                    )
                }
                packages.isEmpty() && ruleCount == 0 -> {
                    // 无规则（edge-cases 三）：引导去规则页
                    EmptyState(
                        emoji = "🧩",
                        title = "还没有匹配规则",
                        description = "添加规则后才能提取快递信息",
                        modifier = Modifier.weight(1f),
                        action = {
                            Button(
                                onClick = onGoToRules,
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text("去看看规则", color = Color.White)
                            }
                        }
                    )
                }
                packages.isEmpty() && scannedCount == 0 -> {
                    // 收件箱空 / 还没扫过
                    EmptyState(
                        emoji = "📦",
                        title = "还没有快递消息",
                        description = "坐等包裹到来～\n也可以下拉手动刷新",
                        modifier = Modifier.weight(1f)
                    )
                }
                packages.isEmpty() -> {
                    // 扫过但 0 匹配：引导加规则
                    EmptyState(
                        emoji = "🔎",
                        title = "暂未发现快递短信",
                        description = "可以试试添加匹配规则，或下拉刷新",
                        modifier = Modifier.weight(1f),
                        action = {
                            TextButton(onClick = onGoToRules) {
                                Text("添加规则", color = Primary)
                            }
                        }
                    )
                }
                else -> {
                    // 下拉刷新 = 全量重扫。LazyColumn 必须在 weight(1f) 容器内，
                    // 否则拿到无限高度约束直接崩溃（初版缺陷）
                    PullToRefreshBox(
                        isRefreshing = isScanning,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            groupedPackages.forEach { (dateMs, pkgs) ->
                                item(key = "day-$dateMs") {
                                    DayGroup(
                                        dateLabel = formatDayLabel(dateMs),
                                        dateMs = dateMs,
                                        packages = pkgs,
                                        collapsed = dateMs in collapsedDays,
                                        onToggleCollapse = { viewModel.toggleDayCollapse(it) },
                                        onTogglePickedUp = { viewModel.togglePickedUp(it) },
                                        onCopyField = { copyToClipboard(context, it) },
                                        onMarkDayPickedUp = { viewModel.markDayPickedUp(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmAllVisible) {
        ConfirmDialog(
            title = "全部标记为已取？",
            message = "把 $unpickedCount 件待取快递都标记为已取件。",
            onConfirm = {
                viewModel.markAllPickedUp()
                confirmAllVisible = false
            },
            onDismiss = { confirmAllVisible = false }
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("", text))
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}

/** 时间戳归一到本地当天 00:00:00 用于分组（与 DAO 的 'localtime' 日界一致） */
private fun normalizeToDayStart(timestampMs: Long): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/viewmodel/PackageListViewModel.kt app/src/main/java/com/quickpick/ui/screen/PackageListScreen.kt && git commit -m "feat: add PackageListViewModel and PackageListScreen"
```

---

### Task 5.2: RuleListViewModel + RuleListScreen

**Files:**
- Create: `app/src/main/java/com/quickpick/ui/viewmodel/RuleListViewModel.kt`
- Create: `app/src/main/java/com/quickpick/ui/screen/RuleListScreen.kt`

- [ ] **Step 1: 创建 RuleListViewModel.kt**

```kotlin
package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.domain.model.MatchRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RuleListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )

    val rules: StateFlow<List<MatchRule>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRule(rule: MatchRule) {
        viewModelScope.launch { repo.deleteById(rule.id) }
    }
}
```

- [ ] **Step 2: 创建 RuleListScreen.kt**

```kotlin
package com.quickpick.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.domain.model.MatchRule
import com.quickpick.ui.component.ConfirmDialog
import com.quickpick.ui.component.EmptyState
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.RuleListViewModel

@Composable
fun RuleListScreen(
    viewModel: RuleListViewModel,
    onAddRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    onExportRule: (MatchRule) -> Unit
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val presets = rules.filter { it.isPreset }
    val customs = rules.filter { !it.isPreset }
    // 删除目标：非 null 时展示确认弹窗
    var deleteTarget by remember { mutableStateOf<MatchRule?>(null) }

    // 内层 Scaffold 置零 inset（外层 MainScreen 已处理），否则上下双份留白
    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题（一个 item 里多个组件要包一层 Column，否则会叠在一起渲染）
            item {
                Column {
                    Text(
                        text = "匹配规则",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "${rules.size} 条规则，${presets.size} 个预置",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // 预置规则
            if (presets.isNotEmpty()) {
                item {
                    Text(
                        text = "预置规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(presets, key = { "preset-${it.id}" }) { rule ->
                    RuleCard(
                        rule = rule,
                        isPreset = true,
                        onClick = { onEditRule(rule.id) },
                        onExport = { onExportRule(rule) },
                        onDelete = { deleteTarget = rule }
                    )
                }
            }

            // 自定义规则
            if (customs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "自定义规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(customs, key = { "custom-${it.id}" }) { rule ->
                    RuleCard(
                        rule = rule,
                        isPreset = false,
                        onClick = { onEditRule(rule.id) },
                        onExport = { onExportRule(rule) },
                        onDelete = { deleteTarget = rule }
                    )
                }
            }

            // 预置正常但无自定义：自定义区给个温和提示（edge-cases 七）
            if (presets.isNotEmpty() && customs.isEmpty()) {
                item {
                    Text(
                        text = "暂无自定义规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (rules.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📋",
                        title = "还没有匹配规则",
                        description = "添加规则后才能提取快递信息"
                    )
                }
            }

            // 新增按钮
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAddRule,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Primary)
                    )
                ) {
                    Text("+ 新增匹配规则", color = Primary)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // 删除规则：二次确认（预置也可删，文案遵循设计系统温和规范）
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除「${target.name}」？",
            message = "删除后将无法恢复，确定要删除吗？",
            confirmText = "删除",
            dangerConfirm = true,
            onConfirm = {
                viewModel.deleteRule(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun RuleCard(
    rule: MatchRule,
    isPreset: Boolean,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val brandColor = BrandColors.forName(rule.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 品牌色圆点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(brandColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${rule.configuredFieldCount} 个字段",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            // 预置标签：浅灰（落地方案），不用主色
            if (isPreset) {
                Text(
                    text = "预置",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .background(InputBackground, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            // 操作按钮 48dp 点击区域
            IconButton(onClick = onExport, modifier = Modifier.size(48.dp)) {
                Text("↗", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Text("🗑", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quickpick/ui/viewmodel/RuleListViewModel.kt app/src/main/java/com/quickpick/ui/screen/RuleListScreen.kt && git commit -m "feat: add RuleListViewModel and RuleListScreen"
```

---

