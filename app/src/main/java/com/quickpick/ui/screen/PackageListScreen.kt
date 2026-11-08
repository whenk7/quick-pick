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
fun PackageListScreen(viewModel: PackageListViewModel) {
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val unpickedCount by viewModel.unpickedCount.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
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

            // 下拉刷新包在最外层：空态也要能下拉（v1.1.10 修复——原先只在「有数据」分支里
            // 渲染 PullToRefresh，一旦列表为空就彻底没有重扫入口，规则导入后无法触发全量重扫，
            // 只能卸载重装。空态内容用可滚动容器撑满，否则没有可拖拽的内容）。
            PullToRefresh(
                isRefreshing = isRefreshing,
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
            when {
                // 权限被回收但已有数据 → 落到 else 列表分支，banner 压顶提示，数据可看
                !hasPermission && packages.isEmpty() -> {
                    EmptyState(
                        emoji = "📬",
                        title = "需要短信权限才能提取快递信息",
                        description = "所有数据仅存储在本地，不上传、不联网",
                        modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
                packages.isEmpty() -> {
                    // 无数据（v1.1.11）：合并原先「无规则」「扫过但 0 匹配」两个分支——
                    // 对用户是同一件事（没数据），且定位都在规则页，没必要分两套文案。
                    // 不放按钮：全站只有三个 tab，规则入口在底栏，加按钮反而冗余。
                    // v1.1.15：补第二、三句排查引导——「范围太小」和「范围内本来没有」是
                    // 除规则外的另两个常见原因，不给线索用户只会反复翻规则页。
                    EmptyState(
                        emoji = "🔎",
                        title = "还没有识别到快递",
                        description = "先看规则配没配\n" +
                                      "配了还是空，就把「扫描范围」调大试试\n" +
                                      "（也可能这个范围里本来就没有取件短信）",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    // 空态也要能下拉刷新（见下方 EmptyListPane）；此处是「有数据」分支
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                    onTogglePickedUp = { viewModel.togglePickedUp(it) },
                                    onDelete = { viewModel.deleteRecord(it) }
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
