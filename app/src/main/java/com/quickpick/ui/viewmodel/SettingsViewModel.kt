package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.PackageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = (application as QuickPickApp).settingsStore
    private val packageRepo = PackageRepository(
        (application as QuickPickApp).database.packageRecordDao()
    )
    private val smsRepo = com.quickpick.data.repository.SmsRepository(
        (application as QuickPickApp).database.scannedSmsDao()
    )

    // 偏好读写（初值与 SettingsStore 默认值一致）
    val scanWindowDays: StateFlow<Int> = settingsStore.scanWindowDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

    // ---- 主题（v1.1.7 砍引擎；老 colorMode/useMonet/themeEngine 弃用） ----
    val themeMode: StateFlow<String> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val dynamicColor: StateFlow<Boolean> = settingsStore.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** null = miuix 默认板（网格首项「默认」选中态） */
    val seedColor: StateFlow<Int?> = settingsStore.seedColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    /**
     * 改扫描窗口。
     *
     * v1.1.12 修复：原实现只删窗口外记录、不重新扫描。改完范围回到首页时，
     * ON_RESUME 触发的增量扫描会把这些短信当「已处理」全跳过（它们在 scanned_sms 里），
     * 用户看到的就是「下拉动画转了但列表没变」。
     * 现在同时清扫描标记，下次扫描会按新窗口重新判定全部短信。
     */
    fun setScanWindowDays(days: Int) {
        viewModelScope.launch {
            settingsStore.setScanWindowDays(days)
            // 窗口调小：先移除窗口外的未取件记录（已取件历史保留）；调大时无记录可删，幂等
            packageRepo.deleteUnpickedOlderThan(System.currentTimeMillis() - days * 86400000L)
            // 清标记让新窗口的短信被重新判定，否则增量扫描会全部跳过
            smsRepo.clearScanned()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    fun setSeedColor(argb: Int?) {
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
