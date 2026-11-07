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

class RuleListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )
    private val smsRepo = com.quickpick.data.repository.SmsRepository(
        (application as QuickPickApp).database.scannedSmsDao()
    )

    val rules: StateFlow<List<MatchRule>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 规则变化后必须让全量重扫可再次发生（v1.1.10 修复）。
     *
     * 原实现只靠「首页下拉」触发 refresh()，而空态首页没有下拉入口（已同批修复）；
     * 更关键的是：导入/删除规则时若不重置 scanned 标记，增量扫描会把所有旧短信当成
     * 「已处理」直接跳过——用户导入规则后看到的一直是空列表，只有卸载重装才能恢复。
     */
    private fun invalidateScanCache() {
        viewModelScope.launch { smsRepo.clearScanned() }
    }

    fun deleteRule(rule: MatchRule) {
        viewModelScope.launch {
            repo.deleteById(rule.id)
            // 删规则 = 少了一种匹配可能，清标记让下次扫描重新判定（已取件记录不受影响）
            smsRepo.clearScanned()
        }
    }

    // ============ 导入导出（v1.1.4 从设置页挪入，规则归规则页管） ============

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events

    /** 待裁决的同名冲突（逐条 覆盖/跳过） */
    data class ImportConflict(val incoming: MatchRule, val existingId: Long)

    private val _conflict = MutableStateFlow<ImportConflict?>(null)
    val conflict: StateFlow<ImportConflict?> = _conflict

    private var pendingImport = mutableListOf<MatchRule>()
    private var importedCount = 0
    private var skippedCount = 0

    /**
     * 导出全部规则 JSON；为空时提示并返回 null（edge-cases 四）。
     * suspend 直读仓库：rules 是 WhileSubscribed，无订阅者时 .value 恒为 emptyList，
     * 导出是动作不是状态，不能依赖它
     */
    suspend fun exportAllJson(): String? {
        val list = repo.getAll()
        if (list.isEmpty()) {
            _events.tryEmit("暂无规则可导出")
            return null
        }
        return JsonExporter.exportAll(list)
    }

    /**
     * 解析导入文本（粘贴或文件），成功返回规则列表供预览，失败 null + toast。
     * 智能识别单个 JSON 对象或数组，自动剥 AI 爱加的 markdown 围栏。
     */
    fun parseImport(raw: String): List<MatchRule>? {
        if (raw.isBlank()) return null
        return JsonExporter.import(JsonExporter.sanitizeAiJson(raw)).getOrNull()
    }

    /** 确认导入：无同名直接新增；有同名进入冲突队列逐条裁决（覆盖/跳过） */
    fun importRules(list: List<MatchRule>) {
        importedCount = 0
        skippedCount = 0
        pendingImport = list.toMutableList()
        viewModelScope.launch { processNext() }
    }

    private suspend fun processNext() {
        val next = pendingImport.removeFirstOrNull()
        if (next == null) {
            // 导入结束：清扫描标记，否则新规则对存量短信永远不生效（v1.1.10 修复）
            if (importedCount > 0) smsRepo.clearScanned()
            _events.tryEmit(
                "导入完成：新增 $importedCount 条" +
                    if (skippedCount > 0) "，跳过 $skippedCount 条" else ""
            )
            return
        }
        val existing = repo.getByName(next.name)
        if (existing.isEmpty()) {
            repo.insert(next)
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
                repo.update(c.incoming.copy(id = c.existingId))
                importedCount++
            } else {
                skippedCount++
            }
            processNext()
        }
    }
}
