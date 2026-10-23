package com.quickpick.ui.navigation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.quickpick.QuickPickApp
import com.quickpick.ui.component.BottomTab
import com.quickpick.ui.component.glass.GlassTuning
import com.quickpick.ui.component.glass.LiquidGlassNavigationBar
import com.quickpick.ui.component.glass.rememberGlassBackdrop
import com.quickpick.ui.screen.*
import com.quickpick.ui.theme.Background
import com.quickpick.ui.viewmodel.*
import com.quickpick.util.JsonExporter
import com.quickpick.util.UiNotifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object RuleEditor : Screen("rule_editor/{ruleId}") {
        fun createRoute(ruleId: Long = 0L) = "rule_editor/$ruleId"
    }
    data object AiPrompt : Screen("ai_prompt")
    data object ThemeSettings : Screen("theme_settings")
    data object AddressAlias : Screen("address_alias")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as QuickPickApp
    val scope = rememberCoroutineScope()

    // 首次启动判定用 DataStore 标记，不用权限状态——
    // 否则权限被系统回收后用户会被弹回引导页（edge-cases 一）
    val onboardingDone by produceState<Boolean?>(initialValue = null) {
        value = app.settingsStore.isOnboardingDone()
    }

    when (val done = onboardingDone) {
        null -> {
            // 读取中的一帧：暖米白底色，不闪白屏
            Box(modifier = Modifier.fillMaxSize().background(Background))
        }
        else -> NavHost(
            navController = navController,
            startDestination = if (done) Screen.Main.route else Screen.Onboarding.route
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onPermissionGranted = {
                    scope.launch { app.settingsStore.setOnboardingDone() }
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Main.route) {
                MainScreen(
                    onNavigateToEditor = { ruleId ->
                        navController.navigate(Screen.RuleEditor.createRoute(ruleId))
                    },
                    onNavigateToAi = {
                        navController.navigate(Screen.AiPrompt.route)
                    },
                    onNavigateToTheme = {
                        navController.navigate(Screen.ThemeSettings.route)
                    },
                    onNavigateToAliases = {
                        navController.navigate(Screen.AddressAlias.route)
                    }
                )
            }

            composable(
                route = Screen.RuleEditor.route,
                arguments = listOf(navArgument("ruleId") { type = NavType.LongType; defaultValue = 0L })
            ) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getLong("ruleId") ?: 0L
                val viewModel: RuleEditorViewModel = viewModel()
                LaunchedEffect(ruleId) {
                    if (ruleId > 0) {
                        viewModel.loadRule(ruleId)
                    } else {
                        // AI 导入链路：读取 AI 页写入的 JSON（字符串传递，避开 Parcelable）
                        val json = backStackEntry.savedStateHandle.get<String>("importedJson")
                        if (json != null) {
                            val sms = backStackEntry.savedStateHandle.get<String>("importedSms") ?: ""
                            backStackEntry.savedStateHandle.remove<String>("importedJson")
                            backStackEntry.savedStateHandle.remove<String>("importedSms")
                            viewModel.loadFromJson(json, sms)
                        }
                    }
                }
                RuleEditorScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AiPrompt.route) {
                val viewModel: AiPromptViewModel = viewModel()
                // v1.1.4：AI 页内直接粘贴入库，不再借道编辑器
                AiPromptScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ThemeSettings.route) {
                val viewModel: SettingsViewModel = viewModel()
                ThemeSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddressAlias.route) {
                val viewModel: com.quickpick.ui.viewmodel.AddressAliasViewModel = viewModel()
                AddressAliasScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToAliases: () -> Unit = {}
) {
    // 构造函数含 SettingsStore，默认 Factory 反射注入不了，走自建 Factory
    val packageViewModel: PackageListViewModel = viewModel(factory = PackageListViewModel.Factory)
    val ruleListViewModel: RuleListViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val context = LocalContext.current
    val app = context.applicationContext as QuickPickApp

    // rememberSaveable：NavHost 前进到子页时主页组合销毁，remember 会丢 tab 回到快递
    // （v1.1.8 真机反馈「返回直接回快递 tab」根因）；saveable 依赖 NavBackStackEntry 存活
    var currentTab by rememberSaveable { mutableStateOf(BottomTab.PACKAGES) }
    val scope = rememberCoroutineScope()

    // 沉浸式取件模式（v1.1.7 §11）：独立状态，切 tab 不退出；
    // 退出仅三条：长按快递 / 返回键（沉浸前台时）/ 全部取完
    var immersionMode by rememberSaveable { mutableStateOf(false) }
    var showImmersionGuide by remember { mutableStateOf(false) }
    val packages by packageViewModel.packages.collectAsStateWithLifecycle()

    // 玻璃底栏四旋钮（主题与外观页调节，全局即时生效）
    val glassBlur by settingsViewModel.glassBlur.collectAsStateWithLifecycle()
    val glassLens by settingsViewModel.glassLens.collectAsStateWithLifecycle()
    val glassHighlight by settingsViewModel.glassHighlight.collectAsStateWithLifecycle()
    val glassAlpha by settingsViewModel.glassAlpha.collectAsStateWithLifecycle()

    // 全局轻提示：CoalescingSnackbarHost 自管理单条状态（同一时间只有一条，新顶旧，v1.1.7 §3）

    // 主页 tab 双击返回退出（2 秒窗口）；子页返回键由 NavHost 自己处理；
    // 弹窗开着时弹窗内的 BackHandler 优先拦截（v1.1.7 §2），这里的双击逻辑自动不触发
    val activity = context as? Activity
    var lastBackAt by remember { mutableLongStateOf(0L) }
    BackHandler {
        // 沉浸视图在快递 tab 前台时：返回 = 退出沉浸（优先于双击退出）
        if (immersionMode && currentTab == BottomTab.PACKAGES) {
            immersionMode = false
            return@BackHandler
        }
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 2000) {
            activity?.finish()
        } else {
            lastBackAt = now
            UiNotifier.notify("再按一次退出", actionLabel = null)
        }
    }

    // 玻璃底栏采样源：先铺页面底色再录内容；内容全出血延伸到栏后（底部留白由各列表 contentPadding 承担）
    val backdrop = rememberGlassBackdrop(scrimColor = Background)

    // miuix Scaffold 默认容器色是 surface（白），显式传 Background 保持页面底色
    Scaffold(
        snackbarHost = { com.quickpick.ui.component.CoalescingSnackbarHost() },
        bottomBar = {
            LiquidGlassNavigationBar(
                currentTab = currentTab,
                onTabClick = { currentTab = it },
                onTabLongClick = { tab ->
                    if (tab == BottomTab.PACKAGES) {
                        if (!immersionMode) {
                            // 进入：首次先弹引导（一次性），引导浮在沉浸视图上
                            scope.launch {
                                if (!app.settingsStore.immersionGuideShown.first()) {
                                    showImmersionGuide = true
                                }
                            }
                            currentTab = BottomTab.PACKAGES
                            immersionMode = true
                        } else {
                            immersionMode = false
                        }
                    }
                },
                backdrop = backdrop,
                tuning = GlassTuning(
                    blur = glassBlur,
                    lens = glassLens,
                    highlight = glassHighlight,
                    clarity = glassAlpha
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .padding(
                    top = padding.calculateTopPadding(),
                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end = padding.calculateEndPadding(LocalLayoutDirection.current)
                )
        ) {
            when {
                // 沉浸模式：快递 tab + 沉浸态 → 沉浸视图（tab 切换不退出沉浸）
                currentTab == BottomTab.PACKAGES && immersionMode -> {
                    ImmersionScreen(
                        packages = packages.filter { !it.isPickedUp },
                        onMarkAllPicked = {
                            packageViewModel.markAllPickedUp()
                            immersionMode = false
                        }
                    )
                }
                currentTab == BottomTab.PACKAGES -> {
                    PackageListScreen(
                        viewModel = packageViewModel,
                        onGoToRules = { currentTab = BottomTab.RULES }
                    )
                }
                currentTab == BottomTab.RULES -> {
                    RuleListScreen(
                        viewModel = ruleListViewModel,
                        onAddRule = { onNavigateToEditor(0L) },
                        onEditRule = { ruleId -> onNavigateToEditor(ruleId) },
                        onExportRule = { rule ->
                            // 单条导出走系统分享面板（本地 ACTION_SEND，不涉及网络）
                            val json = JsonExporter.exportSingle(rule)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(intent, "分享规则"))
                        },
                        onOpenAi = onNavigateToAi
                    )
                }
                else -> {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onOpenTheme = onNavigateToTheme,
                        onOpenAliases = onNavigateToAliases
                    )
                }
            }
        }
    }

    // 沉浸模式首次引导（v1.1.7 §11 用户裁决）：不自动关闭、返回键吞掉、只能点按钮
    BackHandler(enabled = showImmersionGuide) { /* 吞掉：引导必须手动关闭 */ }
    top.yukonga.miuix.kmp.overlay.OverlayDialog(
        show = showImmersionGuide,
        title = "沉浸式取件模式",
        summary = "站在货架前用：大字显示全部待取件码，按位置分组。\n\n进入：长按底栏「快递」\n退出：再长按「快递」，或按返回键",
        onDismissRequest = {}  // 点外部也不关
    ) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = {
                showImmersionGuide = false
                scope.launch { app.settingsStore.setImmersionGuideShown() }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColorsPrimary()
        ) {
            top.yukonga.miuix.kmp.basic.Text("我知道了", color = androidx.compose.ui.graphics.Color.White)
        }
    }
}
