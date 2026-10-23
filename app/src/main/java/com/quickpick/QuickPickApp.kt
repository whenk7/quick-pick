package com.quickpick

import android.app.Application
import android.util.Log
import com.quickpick.data.db.AppDatabase
import com.quickpick.data.local.SettingsStore
import com.quickpick.data.preset.PresetRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickPickApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    // SupervisorJob：种子出错不影响其他协程；带日志，不静默
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching {
                PresetRules.seedIfNeeded(settingsStore, database.matchRuleDao())
                // v1.0.0 老用户：旧预置规则补 senderPattern/品牌锚点（一次性，seeded 标记后不再跑）
                PresetRules.upgradeIfNeeded(settingsStore, database.matchRuleDao())
                // v1.1.4：预置极简重置（去代取点锚点，只留取件码+品牌）
                PresetRules.upgradeToV3IfNeeded(settingsStore, database.matchRuleDao())
                // v1.1.5：发件方过滤从预置移除（端口常变不可靠）
                PresetRules.upgradeToV4IfNeeded(settingsStore, database.matchRuleDao())
            }.onFailure { Log.w("QuickPickApp", "预置规则没能灌入/升级", it) }
        }
    }
}
