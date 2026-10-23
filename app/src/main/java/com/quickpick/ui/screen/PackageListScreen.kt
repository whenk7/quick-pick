package com.quickpick.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickpick.ui.component.*
import com.quickpick.ui.component.glass.glassNavBarClearance
import com.quickpick.ui.theme.*
import com.quickpick.ui.viewmodel.PackageListViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    Scaffold(contentWindowInsets = WindowInsets(0), containerColor = Background) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 标题栏（v1.1.7 §8：副标题 = 待取件数 · 扫描时间范围）
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("取件宝", style = MiuixTheme.textStyles.title2, color = TextPrimary)
                if (hasPermission && packages.isNotEmpty()) {
                    val scanDays by viewModel.scanWindowDays.collectAsStateWithLifecycle()
                    Text(
                        text = "$unpickedCount 件待取 · 近${com.quickpick.ui.component.scanWindowLabel(scanDays)}",
                        style = MiuixTheme.textStyles.body2,
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
                    cornerRadius = 12.dp,
                    colors = CardDefaults.defaultColors(color = InputBackground)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "短信权限已关闭，新快递不会更新",
                            style = MiuixTheme.textStyles.body2,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        InlineTextButton(
                            text = "去开启",
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }
                        )
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
                                colors = ButtonDefaults.buttonColorsPrimary()
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
                                colors = ButtonDefaults.buttonColorsPrimary()
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
                            InlineTextButton(text = "添加规则", onClick = onGoToRules)
                        }
                    )
                }
                else -> {
                    // 下拉刷新 = 全量重扫。LazyColumn 必须在 weight(1f) 容器内，
                    // 否则拿到无限高度约束直接崩溃（初版缺陷）
                    PullToRefresh(
                        isRefreshing = isScanning,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.weight(1f),
                        color = Primary,
                        refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新…", "刷新完成"),
                        refreshTextStyle = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextTertiary
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            // 底部留白给悬浮玻璃底栏（内容可滚动到栏后透出模糊；inset 动态，三键导航不遮挡）
                            // top 必须 0：contentPadding 区域不裁剪内容，吸顶 header 停在 padding 之下，
                            // 留缝会露出滚过的卡片（header 自带 10dp 垂直内边距，视觉间距不缺）
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = glassNavBarClearance())
                        ) {
                            groupedPackages.forEach { (dateMs, pkgs) ->
                                // 标题吸顶：背景实体色遮住下方滚动内容（DayGroupHeader 内处理）；
                                // key 用 dateMs（分组键天然唯一），正文 item 加 -content 后缀避免撞 key
                                stickyHeader(key = "day-$dateMs") {
                                    DayGroupHeader(
                                        dateLabel = formatDayLabel(dateMs),
                                        dateMs = dateMs,
                                        collapsed = dateMs in collapsedDays,
                                        hasUnpicked = pkgs.any { !it.isPickedUp },
                                        onToggleCollapse = { viewModel.toggleDayCollapse(it) },
                                        onMarkDayPickedUp = { viewModel.markDayPickedUp(it) }
                                    )
                                }
                                item(key = "day-content-$dateMs") {
                                    DayGroupContent(
                                        packages = pkgs,
                                        collapsed = dateMs in collapsedDays,
                                        onTogglePickedUp = { viewModel.togglePickedUp(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
