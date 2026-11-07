package com.quickpick.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quickpick_prefs")

/** 轻量偏好存储：权限引导完成标记、扫描窗口、主题与玻璃底栏偏好 */
class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        // 预置规则已移除（v1.1.11），旧键 rules_seeded / preset_v2~v4_seeded 保留在 DataStore 里不清理：
        // 删键要动存储结构，而残留几个布尔值没有任何影响。
        private val KEY_SCAN_WINDOW_DAYS = intPreferencesKey("scan_window_days")

        /**
         * 规则变更标记（v1.1.11）：规则一旦变化就置 true，首页回到前台时发现它
         * 就自动全量重扫并复位。这样所有变更路径（编辑器保存 / 规则页导入 / AI 导入 /
         * 删除规则）都能闭环，用户配完规则直接看到数据，不用自己下拉。
         */
        private val KEY_RULES_DIRTY = booleanPreferencesKey("rules_dirty")

        // 主题（v1.1.7 砍引擎：theme_engine 键废弃不迁移；seed_color 缺失 = miuix 默认板）
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_SEED_COLOR = intPreferencesKey("seed_color")
        private val KEY_PALETTE_STYLE = stringPreferencesKey("palette_style")
        private val KEY_COLOR_SPEC = stringPreferencesKey("color_spec")

        // 玻璃底栏强度（0..1，UI 层映射到 dp/alpha）
        private val KEY_GLASS_BLUR = floatPreferencesKey("glass_blur")
        private val KEY_GLASS_LENS = floatPreferencesKey("glass_lens")
        private val KEY_GLASS_HIGHLIGHT = floatPreferencesKey("glass_highlight")
        private val KEY_GLASS_ALPHA = floatPreferencesKey("glass_alpha")

        // 全局地址簿（v1.1.7 §6）：JSON 列表 [{match, alias}]，显示层替换不动库存数据
        private val KEY_ADDRESS_ALIASES = stringPreferencesKey("address_aliases")
        // 沉浸式取件模式首次引导（v1.1.7 §11）
        private val KEY_IMMERSION_GUIDE_SHOWN = booleanPreferencesKey("immersion_guide_shown")
        // 用户手动删除的记录来源短信（v1.1.8）：重扫跳过，否则全量重扫会把删除的记录扫回来
        private val KEY_IGNORED_SMS_IDS = stringSetPreferencesKey("ignored_sms_ids")
    }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun isOnboardingDone(): Boolean = onboardingDone.first()

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    /** 规则变更标记：任何规则写操作后调用；首页消费后调 clearRulesDirty() */
    val rulesDirty: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_RULES_DIRTY] ?: false }

    suspend fun isRulesDirty(): Boolean = rulesDirty.first()

    suspend fun markRulesDirty() {
        context.dataStore.edit { it[KEY_RULES_DIRTY] = true }
    }

    suspend fun clearRulesDirty() {
        context.dataStore.edit { it[KEY_RULES_DIRTY] = false }
    }

    /** 扫描时间窗口（天） */
    val scanWindowDays: Flow<Int> =
        context.dataStore.data.map { it[KEY_SCAN_WINDOW_DAYS] ?: 7 }

    suspend fun setScanWindowDays(days: Int) {
        context.dataStore.edit { it[KEY_SCAN_WINDOW_DAYS] = days }
    }

    // ============ 主题 ============

    /** 明暗模式："SYSTEM" / "LIGHT" / "DARK" */
    val themeMode: Flow<String> =
        context.dataStore.data.map { it[KEY_THEME_MODE] ?: "SYSTEM" }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    /**
     * 动态取色（壁纸/系统调色板，Android 12+ 才有意义）。
     * v1.1.7 起默认 false：原 FIXED 用户键缺失，若默认 true 升级后会从固定板跳到壁纸 Monet。
     */
    val dynamicColor: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: false }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    /** 种子色；null（键缺失）= miuix 默认板（v1.1.7 §7 网格首项「默认」） */
    val seedColor: Flow<Int?> =
        context.dataStore.data.map { it[KEY_SEED_COLOR] }

    suspend fun setSeedColor(argb: Int?) {
        context.dataStore.edit {
            if (argb == null) it.remove(KEY_SEED_COLOR) else it[KEY_SEED_COLOR] = argb
        }
    }

    /** 调色板风格（ThemePaletteStyle 枚举名），默认 TonalSpot */
    val paletteStyle: Flow<String> =
        context.dataStore.data.map { it[KEY_PALETTE_STYLE] ?: "TonalSpot" }

    suspend fun setPaletteStyle(style: String) {
        context.dataStore.edit { it[KEY_PALETTE_STYLE] = style }
    }

    /** 色彩规格："Spec2021" / "Spec2025"（风格不支持时生效值回退，不改写此偏好） */
    val colorSpec: Flow<String> =
        context.dataStore.data.map { it[KEY_COLOR_SPEC] ?: "Spec2025" }

    suspend fun setColorSpec(spec: String) {
        context.dataStore.edit { it[KEY_COLOR_SPEC] = spec }
    }

    // ============ 玻璃底栏 ============

    val glassBlur: Flow<Float> =
        context.dataStore.data.map { it[KEY_GLASS_BLUR] ?: 0.6f }

    suspend fun setGlassBlur(v: Float) {
        context.dataStore.edit { it[KEY_GLASS_BLUR] = v }
    }

    val glassLens: Flow<Float> =
        context.dataStore.data.map { it[KEY_GLASS_LENS] ?: 0.55f }

    suspend fun setGlassLens(v: Float) {
        context.dataStore.edit { it[KEY_GLASS_LENS] = v }
    }

    val glassHighlight: Flow<Float> =
        context.dataStore.data.map { it[KEY_GLASS_HIGHLIGHT] ?: 0.85f }

    suspend fun setGlassHighlight(v: Float) {
        context.dataStore.edit { it[KEY_GLASS_HIGHLIGHT] = v }
    }

    /** 玻璃通透度 0..1（UI 映射为容器不透明度 0.6→0.25，越高越透） */
    val glassAlpha: Flow<Float> =
        context.dataStore.data.map { it[KEY_GLASS_ALPHA] ?: 0.8f }

    suspend fun setGlassAlpha(v: Float) {
        context.dataStore.edit { it[KEY_GLASS_ALPHA] = v }
    }

    // ============ 全局地址簿（v1.1.7 §6） ============

    private val aliasListType = object : com.google.gson.reflect.TypeToken<List<com.quickpick.domain.model.AddressAlias>>() {}.type
    private val gson = com.google.gson.Gson()

    val addressAliases: Flow<List<com.quickpick.domain.model.AddressAlias>> =
        context.dataStore.data.map { prefs ->
            val json = prefs[KEY_ADDRESS_ALIASES] ?: return@map emptyList()
            runCatching {
                gson.fromJson<List<com.quickpick.domain.model.AddressAlias?>>(json, aliasListType)
                    .mapNotNull { it }
                    .filter { it.match.isNotBlank() }
            }.getOrDefault(emptyList())
        }

    /** 写入前过滤空白 match（contains("") 恒 true 的坑，见 Review Focus #5） */
    suspend fun setAddressAliases(list: List<com.quickpick.domain.model.AddressAlias>) {
        val cleaned = list.filter { it.match.isNotBlank() && it.alias.isNotBlank() }
        context.dataStore.edit { it[KEY_ADDRESS_ALIASES] = gson.toJson(cleaned) }
    }

    // ============ 沉浸模式引导（v1.1.7 §11） ============

    val immersionGuideShown: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_IMMERSION_GUIDE_SHOWN] ?: false }

    suspend fun setImmersionGuideShown() {
        context.dataStore.edit { it[KEY_IMMERSION_GUIDE_SHOWN] = true }
    }

    // ============ 忽略名单（v1.1.8） ============

    val ignoredSmsIds: Flow<Set<String>> =
        context.dataStore.data.map { it[KEY_IGNORED_SMS_IDS] ?: emptySet() }

    suspend fun ignoreSms(smsId: String) {
        context.dataStore.edit { it[KEY_IGNORED_SMS_IDS] = (it[KEY_IGNORED_SMS_IDS] ?: emptySet()) + smsId }
    }
}
