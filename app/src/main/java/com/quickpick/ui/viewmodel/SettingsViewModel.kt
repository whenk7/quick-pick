package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = (application as QuickPickApp).settingsStore

    // 偏好读写（初值与 SettingsStore 默认值一致）
    val scanWindowDays: StateFlow<Int> = settingsStore.scanWindowDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

    // ---- 主题（2026-10-22 重做，老 colorMode/useMonet 弃用） ----
    val themeMode: StateFlow<String> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val themeEngine: StateFlow<String> = settingsStore.themeEngine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FIXED")

    val dynamicColor: StateFlow<Boolean> = settingsStore.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val seedColor: StateFlow<Int> = settingsStore.seedColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF3482FF.toInt())

    val paletteStyle: StateFlow<String> = settingsStore.paletteStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "TonalSpot")

    val colorSpec: StateFlow<String> = settingsStore.colorSpec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Spec2025")

    // ---- 玻璃底栏强度 ----
    val glassBlur: StateFlow<Float> = settingsStore.glassBlur
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.6f)

    val glassLens: StateFlow<Float> = settingsStore.glassLens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.55f)

    val glassHighlight: StateFlow<Float> = settingsStore.glassHighlight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.85f)

    val glassAlpha: StateFlow<Float> = settingsStore.glassAlpha
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.32f)

    fun setScanWindowDays(days: Int) {
        viewModelScope.launch { settingsStore.setScanWindowDays(days) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setThemeEngine(engine: String) {
        viewModelScope.launch { settingsStore.setThemeEngine(engine) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    fun setSeedColor(argb: Int) {
        viewModelScope.launch { settingsStore.setSeedColor(argb) }
    }

    fun setPaletteStyle(style: String) {
        viewModelScope.launch { settingsStore.setPaletteStyle(style) }
    }

    fun setColorSpec(spec: String) {
        viewModelScope.launch { settingsStore.setColorSpec(spec) }
    }

    fun setGlassBlur(v: Float) {
        viewModelScope.launch { settingsStore.setGlassBlur(v) }
    }

    fun setGlassLens(v: Float) {
        viewModelScope.launch { settingsStore.setGlassLens(v) }
    }

    fun setGlassHighlight(v: Float) {
        viewModelScope.launch { settingsStore.setGlassHighlight(v) }
    }

    fun setGlassAlpha(v: Float) {
        viewModelScope.launch { settingsStore.setGlassAlpha(v) }
    }
}
