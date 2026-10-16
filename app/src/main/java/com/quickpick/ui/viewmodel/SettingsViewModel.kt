package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.domain.model.MatchRule
import com.quickpick.util.JsonExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val ruleRepo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )

    val rules: StateFlow<List<MatchRule>> = ruleRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events

    /** 待裁决的同名冲突（逐条 覆盖/跳过） */
    data class ImportConflict(val incoming: MatchRule, val existingId: Long)

    private val _conflict = MutableStateFlow<ImportConflict?>(null)
    val conflict: StateFlow<ImportConflict?> = _conflict

    private var pendingImport = mutableListOf<MatchRule>()
    private var importedCount = 0
    private var skippedCount = 0

    /** 导出全部规则 JSON；为空时提示并返回 null（edge-cases 四） */
    fun exportAllJson(): String? {
        val list = rules.value
        if (list.isEmpty()) {
            _events.tryEmit("暂无规则可导出")
            return null
        }
        return JsonExporter.exportAll(list)
    }

    /** 导入 JSON：无同名直接新增；有同名进入冲突队列逐条裁决（覆盖/跳过） */
    fun importRules(json: String) {
        viewModelScope.launch {
            val result = JsonExporter.import(JsonExporter.sanitizeAiJson(json))
            val list = result.getOrNull()
            if (list == null) {
                _events.tryEmit("文件格式不太对，请选择规则 JSON 文件")
                return@launch
            }
            importedCount = 0
            skippedCount = 0
            pendingImport = list.toMutableList()
            processNext()
        }
    }

    private suspend fun processNext() {
        val next = pendingImport.removeFirstOrNull()
        if (next == null) {
            _events.tryEmit(
                "导入完成：新增 $importedCount 条" +
                    if (skippedCount > 0) "，跳过 $skippedCount 条" else ""
            )
            return
        }
        val existing = ruleRepo.getByName(next.name)
        if (existing.isEmpty()) {
            ruleRepo.insert(next)
            importedCount++
            processNext()
        } else {
            _conflict.value = ImportConflict(next, existing.first().id)
        }
    }

    /** 冲突裁决：true=覆盖已有规则，false=跳过 */
    fun resolveConflict(overwrite: Boolean) {
        val c = _conflict.value ?: return
        _conflict.value = null
        viewModelScope.launch {
            if (overwrite) {
                // update 内部会保留原 isPreset / createdAt（见 RuleRepository）
                ruleRepo.update(c.incoming.copy(id = c.existingId))
                importedCount++
            } else {
                skippedCount++
            }
            processNext()
        }
    }
}
