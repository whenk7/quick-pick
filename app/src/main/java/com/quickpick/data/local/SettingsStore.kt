package com.quickpick.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quickpick.data.preset.PresetFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quickpick_prefs")

/** 轻量偏好存储：权限引导完成标记、预置规则灌入标记、扫描窗口、主题与玻璃底栏偏好 */
class SettingsStore(private val context: Context) : PresetFlags {

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_RULES_SEEDED = booleanPreferencesKey("rules_seeded")
        private val KEY_PRESET_V2_SEEDED = booleanPreferencesKey("preset_v2_seeded")
        private val KEY_PRESET_V3_SEEDED = booleanPreferencesKey("preset_v3_seeded")
        private val KEY_PRESET_V4_SEEDED = booleanPreferencesKey("preset_v4_seeded")
        private val KEY_SCAN_WINDOW_DAYS = intPreferencesKey("scan_window_days")

        // 主题（2026-10-22 重做；老键 color_mode/use_monet 弃用不迁移）
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_THEME_ENGINE = stringPreferencesKey("theme_engine")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_SEED_COLOR = intPreferencesKey("seed_color")
        private val KEY_PALETTE_STYLE = stringPreferencesKey("palette_style")
        private val KEY_COLOR_SPEC = stringPreferencesKey("color_spec")

        // 玻璃底栏强度（0..1，UI 层映射到 dp/alpha）
        private val KEY_GLASS_BLUR = floatPreferencesKey("glass_blur")
        private val KEY_GLASS_LENS = floatPreferencesKey("glass_lens")
        private val KEY_GLASS_HIGHLIGHT = floatPreferencesKey("glass_highlight")
        private val KEY_GLASS_ALPHA = floatPreferencesKey("glass_alpha")
    }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun isOnboardingDone(): Boolean = onboardingDone.first()

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    override suspend fun isRulesSeeded(): Boolean =
        context.dataStore.data.map { it[KEY_RULES_SEEDED] ?: false }.first()

    override suspend fun setRulesSeeded() {
        context.dataStore.edit { it[KEY_RULES_SEEDED] = true }
    }

    override suspend fun isPresetV2Seeded(): Boolean =
        context.dataStore.data.map { it[KEY_PRESET_V2_SEEDED] ?: false }.first()

    override suspend fun setPresetV2Seeded() {
        context.dataStore.edit { it[KEY_PRESET_V2_SEEDED] = true }
    }

    override suspend fun isPresetV3Seeded(): Boolean =
        context.dataStore.data.map { it[KEY_PRESET_V3_SEEDED] ?: false }.first()

    override suspend fun setPresetV3Seeded() {
        context.dataStore.edit { it[KEY_PRESET_V3_SEEDED] = true }
    }

    override suspend fun isPresetV4Seeded(): Boolean =
        context.dataStore.data.map { it[KEY_PRESET_V4_SEEDED] ?: false }.first()

    override suspend fun setPresetV4Seeded() {
        context.dataStore.edit { it[KEY_PRESET_V4_SEEDED] = true }
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

    /** 配色引擎："FIXED"（miuix 内置板）/ "MONET"（动态配色） */
    val themeEngine: Flow<String> =
        context.dataStore.data.map { it[KEY_THEME_ENGINE] ?: "FIXED" }

    suspend fun setThemeEngine(engine: String) {
        context.dataStore.edit { it[KEY_THEME_ENGINE] = engine }
    }

    /** Monet 内的动态取色（壁纸/系统调色板，Android 12+ 才有意义） */
    val dynamicColor: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    /** 种子色（动态取色关闭或 API<31 时作为 Monet 种子），默认 miuix 主题蓝 */
    val seedColor: Flow<Int> =
        context.dataStore.data.map { it[KEY_SEED_COLOR] ?: 0xFF3482FF.toInt() }

    suspend fun setSeedColor(argb: Int) {
        context.dataStore.edit { it[KEY_SEED_COLOR] = argb }
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
}
