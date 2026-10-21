package com.quickpick.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quickpick_prefs")

/** 轻量偏好存储：权限引导完成标记、预置规则灌入标记、扫描窗口与主题偏好 */
class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_RULES_SEEDED = booleanPreferencesKey("rules_seeded")
        private val KEY_PRESET_V2_SEEDED = booleanPreferencesKey("preset_v2_seeded")
        private val KEY_SCAN_WINDOW_DAYS = intPreferencesKey("scan_window_days")
        private val KEY_COLOR_MODE = stringPreferencesKey("color_mode")
        private val KEY_USE_MONET = booleanPreferencesKey("use_monet")
        private val KEY_PALETTE_STYLE = stringPreferencesKey("palette_style")
    }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun isOnboardingDone(): Boolean = onboardingDone.first()

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun isRulesSeeded(): Boolean =
        context.dataStore.data.map { it[KEY_RULES_SEEDED] ?: false }.first()

    suspend fun setRulesSeeded() {
        context.dataStore.edit { it[KEY_RULES_SEEDED] = true }
    }

    suspend fun isPresetV2Seeded(): Boolean =
        context.dataStore.data.map { it[KEY_PRESET_V2_SEEDED] ?: false }.first()

    suspend fun setPresetV2Seeded() {
        context.dataStore.edit { it[KEY_PRESET_V2_SEEDED] = true }
    }

    /** 扫描时间窗口（天） */
    val scanWindowDays: Flow<Int> =
        context.dataStore.data.map { it[KEY_SCAN_WINDOW_DAYS] ?: 7 }

    suspend fun setScanWindowDays(days: Int) {
        context.dataStore.edit { it[KEY_SCAN_WINDOW_DAYS] = days }
    }

    /** 颜色模式："SYSTEM" / "LIGHT" / "DARK" */
    val colorMode: Flow<String> =
        context.dataStore.data.map { it[KEY_COLOR_MODE] ?: "SYSTEM" }

    suspend fun setColorMode(mode: String) {
        context.dataStore.edit { it[KEY_COLOR_MODE] = mode }
    }

    /** 是否启用 Monet 动态取色（Android 12+） */
    val useMonet: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_USE_MONET] ?: false }

    suspend fun setUseMonet(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_MONET] = enabled }
    }

    /** 动态调色板风格（miuix/Hct），默认 TonalSpot */
    val paletteStyle: Flow<String> =
        context.dataStore.data.map { it[KEY_PALETTE_STYLE] ?: "TonalSpot" }

    suspend fun setPaletteStyle(style: String) {
        context.dataStore.edit { it[KEY_PALETTE_STYLE] = style }
    }
}
