package com.quickpick.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.quickpick.QuickPickApp
import com.quickpick.ui.component.BottomNavBar
import com.quickpick.ui.component.BottomTab
import com.quickpick.ui.screen.*
import com.quickpick.ui.theme.Background
import com.quickpick.ui.viewmodel.*
import com.quickpick.util.JsonExporter
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object RuleEditor : Screen("rule_editor/{ruleId}") {
        fun createRoute(ruleId: Long = 0L) = "rule_editor/$ruleId"
    }
    data object AiPrompt : Screen("ai_prompt")
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
            Surface(modifier = Modifier.fillMaxSize(), color = Background) {}
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
                AiPromptScreen(
                    viewModel = viewModel,
                    onImportRule = { json, smsText ->
                        navController.navigate(Screen.RuleEditor.createRoute(0L))
                        // navigate 之后 currentBackStackEntry 即编辑器页，写入它的 savedStateHandle
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("importedJson", json)
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("importedSms", smsText)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToAi: () -> Unit
) {
    // 构造函数含 SettingsStore，默认 Factory 反射注入不了，走自建 Factory
    val packageViewModel: PackageListViewModel = viewModel(factory = PackageListViewModel.Factory)
    val ruleListViewModel: RuleListViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf(BottomTab.PACKAGES) }

    Scaffold(
        bottomBar = {
            BottomNavBar(currentTab = currentTab, onTabClick = { currentTab = it })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                BottomTab.PACKAGES -> {
                    PackageListScreen(
                        viewModel = packageViewModel,
                        onGoToRules = { currentTab = BottomTab.RULES }
                    )
                }
                BottomTab.RULES -> {
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
                        }
                    )
                }
                BottomTab.SETTINGS -> {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onOpenAiPrompt = onNavigateToAi
                    )
                }
            }
        }
    }
}
