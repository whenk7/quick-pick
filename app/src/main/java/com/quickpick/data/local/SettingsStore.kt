package com.quickpick.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quickpick_prefs")

/** 轻量偏好存储：权限引导完成标记、预置规则灌入标记 */
class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_RULES_SEEDED = booleanPreferencesKey("rules_seeded")
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
}
