package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.domain.model.MatchRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RuleListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )

    val rules: StateFlow<List<MatchRule>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRule(rule: MatchRule) {
        viewModelScope.launch { repo.deleteById(rule.id) }
    }
}
