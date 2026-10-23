package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.domain.model.AddressAlias
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 全局地址簿（v1.1.7 §6）：行式增删，改动即写 SettingsStore */
class AddressAliasViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = (application as QuickPickApp).settingsStore

    val aliases: StateFlow<List<AddressAlias>> = settingsStore.addressAliases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(match: String, alias: String) {
        viewModelScope.launch {
            // 同 match 覆盖（防重复 match 撞 LazyColumn key 崩溃循环，终审 Important #1）
            settingsStore.setAddressAliases(
                com.quickpick.domain.model.upsertAddressAlias(
                    aliases.value, AddressAlias(match.trim(), alias.trim())
                )
            )
        }
    }

    fun remove(item: AddressAlias) {
        viewModelScope.launch {
            settingsStore.setAddressAliases(aliases.value - item)
        }
    }
}
