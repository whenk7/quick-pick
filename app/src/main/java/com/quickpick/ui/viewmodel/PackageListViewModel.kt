package com.quickpick.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.quickpick.QuickPickApp
import com.quickpick.data.local.SettingsStore
import com.quickpick.data.repository.PackageRepository
import com.quickpick.data.repository.RuleRepository
import com.quickpick.data.repository.SmsRepository
import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.model.PackageRecord
import com.quickpick.util.SmsReader
import com.quickpick.util.UiNotifier
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PackageListViewModel(
    application: Application,
    private val settingsStore: SettingsStore
) : AndroidViewModel(application) {

    companion object {
        /** 默认 Factory 只会注入 Application，SettingsStore 需自建 Factory 传入 */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as QuickPickApp
                PackageListViewModel(app, app.settingsStore)
            }
        }
    }

    private val db = (application as QuickPickApp).database
    private val packageRepo = PackageRepository(db.packageRecordDao())
    private val ruleRepo = RuleRepository(db.matchRuleDao())
    private val smsRepo = SmsRepository(db.scannedSmsDao())

    // v1.1.7 §6：显示层应用地址别名（不动库存数据；改别名即时生效无需重扫）
    val packages: StateFlow<List<PackageRecord>> = packageRepo.observeAll()
        .combine(settingsStore.addressAliases) { list, aliases ->
            list.map { it.copy(address = com.quickpick.domain.model.applyAddressAlias(it.address, aliases)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpickedCount: StateFlow<Int> = packageRepo.observeUnpickedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val ruleCount: StateFlow<Int> = ruleRepo.observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 扫描窗口天数（首页副标题「· 近三月」用，v1.1.7 §8） */
    val scanWindowDays: StateFlow<Int> = settingsStore.scanWindowDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    /** 是否有短信权限。由 UI 在进入页面/回到前台时调用 refreshPermissionState() 刷新 */
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    /** 折叠的日期分组（当天 00:00 时间戳），会话内有效 */
    private val _collapsedDays = MutableStateFlow<Set<Long>>(emptySet())
    val collapsedDays: StateFlow<Set<Long>> = _collapsedDays

    /** 一次性提示（Toast） */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events

    /** UI 在进入页面与 ON_RESUME 时调用：刷新权限状态，有权限则增量扫描 */
    fun refreshPermissionState() {
        val granted = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        _hasPermission.value = granted
        if (granted) scanSms()
    }

    /**
     * @param removeUnmatched 全量重扫语义（v1.1.5 用户裁决）：短信当前规则匹配不上时，
     *   移除其对应的未取件失效记录。仅手动下拉（clearScanned 全量重扫）传 true；
     *   增量扫描（新短信到达/权限恢复）不动存量记录。
     */
    fun scanSms(removeUnmatched: Boolean = false) {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                // 权限判定前置：不再靠「读到空表」猜权限状态
                val granted = ContextCompat.checkSelfPermission(
                    getApplication(), Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                _hasPermission.value = granted
                if (!granted) return@launch

                // 扫描时间窗口：读用户偏好（默认 7 天，设置页「短信扫描」四档可调）
                val days = settingsStore.scanWindowDays.first()
                val cutoffMs = System.currentTimeMillis() - days * 86400000L
                val messages = SmsReader.readSince(getApplication(), cutoffMs)
                _scannedCount.value = smsRepo.scannedCount()

                val rules = ruleRepo.getAll()
                if (rules.isEmpty()) {
                    // 无规则 = 一切不匹配（v1.1.7）：手动重扫清空未取件；增量扫描不动存量
                    if (removeUnmatched) packageRepo.deleteAllUnpicked()
                    return@launch
                }

                // 用户手动删除过的短信：标已扫但跳过入库（v1.1.8 忽略名单，重扫不复活）
                val ignored = settingsStore.ignoredSmsIds.first()

                messages.forEach { sms ->
                    if (sms.id in ignored) {
                        smsRepo.markScanned(sms.id)
                        return@forEach
                    }
                    if (smsRepo.isScanned(sms.id)) return@forEach
                    val result = MatchEngine.findBestMatch(sms.body, rules, sms.address)
                    if (result != null) {
                        val (rule, fields) = result
                        packageRepo.insertFromSms(
                            PackageRecord(
                                // v1.1.7 §5：规则名纯记忆用不进卡片；品牌提取失败显示「快递」
                                companyName = fields.brandName ?: "快递",
                                trackingNumber = fields.trackingNumber,
                                pickupCode = fields.pickupCode,
                                address = fields.address,
                                phone = fields.phone,
                                smsBody = sms.body,
                                smsTimestamp = sms.timestamp,
                                ruleId = rule.id,
                                smsId = sms.id,
                                smsThreadId = sms.threadId,
                                smsSender = sms.address
                            )
                        )
                    }
                    if (result == null && removeUnmatched) {
                        packageRepo.deleteUnpickedBySmsId(sms.id)
                    }
                    smsRepo.markScanned(sms.id)
                }
                _scannedCount.value = smsRepo.scannedCount()
            } catch (e: SecurityException) {
                // 扫描过程中权限被系统回收
                _hasPermission.value = false
            } catch (e: Exception) {
                // 存储空间不足、数据库异常等：温和提示，不崩溃（edge-cases 六）
                _events.tryEmit("没能完成扫描，请稍后再试～")
            } finally {
                _isScanning.value = false
            }
        }
    }

    /** 手动全量重扫（下拉刷新）：清空已扫标记后重扫 */
    fun refresh() {
        viewModelScope.launch {
            // 扫描在途时不清标记：否则 clearScanned 后 scanSms 直接 return，
            // 下次扫描会把在途那轮已处理的短信再处理一遍
            if (_isScanning.value) return@launch
            smsRepo.clearScanned()
            scanSms(removeUnmatched = true)
        }
    }

    fun togglePickedUp(record: PackageRecord) {
        viewModelScope.launch {
            packageRepo.setPickedUp(record.id, !record.isPickedUp)
        }
    }

    /** 手动删除记录（v1.1.8 原文弹窗）：无论是否已取件；短信加忽略名单防重扫复活 */
    fun deleteRecord(record: PackageRecord) {
        viewModelScope.launch {
            packageRepo.deleteById(record.id)
            record.smsId?.let { settingsStore.ignoreSms(it) }
            UiNotifier.notify("已删除该快递信息")
        }
    }

    fun markAllPickedUp() {
        viewModelScope.launch { packageRepo.markAllPickedUp() }
    }

    fun markDayPickedUp(dateMs: Long) {
        viewModelScope.launch { packageRepo.markDayPickedUp(dateMs) }
    }

    fun toggleDayCollapse(dateMs: Long) {
        _collapsedDays.value = _collapsedDays.value.let {
            if (dateMs in it) it - dateMs else it + dateMs
        }
    }
}
