package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.util.UiNotifier
import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.model.ExtractedFields
import com.quickpick.domain.model.MatchRule
import com.quickpick.util.JsonExporter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EditorState(
    val ruleId: Long = 0,
    val ruleName: String = "",
    val smsText: String = "",
    val activeField: ActiveField = ActiveField.PICKUP_CODE,
    val trackingPrefix: String = "",
    val trackingSuffix: String = "",
    val codePrefix: String = "",
    val codeSuffix: String = "",
    val addressPrefix: String = "",
    val addressSuffix: String = "",
    val phonePrefix: String = "",
    val phoneSuffix: String = "",
    val senderPattern: String = "",
    val brandPrefix: String = "",
    val brandSuffix: String = "",
    val preview: ExtractedFields? = null
)

enum class ActiveField {
    TRACKING, PICKUP_CODE, ADDRESS, PHONE, SENDER, BRAND
}

class RuleEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as QuickPickApp).database
    private val repo = RuleRepository(db.matchRuleDao())

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state

    /** 一次性提示（Toast） */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events

    /** 保存完成事件：true=编辑已有规则（UI 提示是否重扫未匹配短信），false=新增 */
    private val _savedEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val savedEvents: SharedFlow<Boolean> = _savedEvents

    fun loadRule(id: Long) {
        viewModelScope.launch {
            val rule = repo.getById(id) ?: return@launch
            _state.value = _state.value.copy(
                ruleId = rule.id,
                ruleName = rule.name,
                smsText = rule.sampleSms ?: "",
                trackingPrefix = rule.trackingPrefix ?: "",
                trackingSuffix = rule.trackingSuffix ?: "",
                codePrefix = rule.codePrefix ?: "",
                codeSuffix = rule.codeSuffix ?: "",
                addressPrefix = rule.addressPrefix ?: "",
                addressSuffix = rule.addressSuffix ?: "",
                phonePrefix = rule.phonePrefix ?: "",
                phoneSuffix = rule.phoneSuffix ?: "",
                senderPattern = rule.senderPattern ?: "",
                brandPrefix = rule.brandPrefix ?: "",
                brandSuffix = rule.brandSuffix ?: ""
            )
            refreshPreview()
        }
    }

    /** 从 AI 导入的 JSON 字符串加载（字符串传递，避开 Parcelable 和进程重建问题） */
    fun loadFromJson(json: String, smsText: String) {
        val rule = JsonExporter.import(JsonExporter.sanitizeAiJson(json))
            .getOrNull()?.firstOrNull() ?: return
        _state.value = _state.value.copy(
            ruleName = rule.name,
            smsText = smsText,
            trackingPrefix = rule.trackingPrefix ?: "",
            trackingSuffix = rule.trackingSuffix ?: "",
            codePrefix = rule.codePrefix ?: "",
            codeSuffix = rule.codeSuffix ?: "",
            addressPrefix = rule.addressPrefix ?: "",
            addressSuffix = rule.addressSuffix ?: "",
            phonePrefix = rule.phonePrefix ?: "",
            phoneSuffix = rule.phoneSuffix ?: "",
            senderPattern = rule.senderPattern ?: "",
            brandPrefix = rule.brandPrefix ?: "",
            brandSuffix = rule.brandSuffix ?: ""
        )
        refreshPreview()
    }

    fun updateRuleName(name: String) {
        _state.value = _state.value.copy(ruleName = name)
    }

    fun updateSmsText(text: String) {
        // 规则名为空且短信带【公司签名】时，提取为默认规则名（spec 五.7；纯 indexOf 与引擎同风格）
        val defaultName = if (_state.value.ruleName.isBlank()) {
            val l = text.indexOf('【')
            val r = text.indexOf('】', l + 1)
            if (l >= 0 && r > l + 1) text.substring(l + 1, r) else null
        } else null
        _state.value = _state.value.copy(
            smsText = text,
            ruleName = defaultName ?: _state.value.ruleName
        )
        refreshPreview()
    }

    fun setActiveField(field: ActiveField) {
        _state.value = _state.value.copy(activeField = field)
    }

    fun updateActiveFieldPrefix(value: String) {
        val s = _state.value
        val ns = when (s.activeField) {
            ActiveField.TRACKING -> s.copy(trackingPrefix = value)
            ActiveField.PICKUP_CODE -> s.copy(codePrefix = value)
            ActiveField.ADDRESS -> s.copy(addressPrefix = value)
            ActiveField.PHONE -> s.copy(phonePrefix = value)
            ActiveField.SENDER -> s.copy(senderPattern = value)
            ActiveField.BRAND -> s.copy(brandPrefix = value)
        }
        _state.value = ns
        refreshPreview()
    }

    fun updateActiveFieldSuffix(value: String) {
        val s = _state.value
        val ns = when (s.activeField) {
            ActiveField.TRACKING -> s.copy(trackingSuffix = value)
            ActiveField.PICKUP_CODE -> s.copy(codeSuffix = value)
            ActiveField.ADDRESS -> s.copy(addressSuffix = value)
            ActiveField.PHONE -> s.copy(phoneSuffix = value)
            // senderPattern 是单字符串匹配，无后缀概念（UI 也不显示后缀框），保持不变
            ActiveField.SENDER -> s
            ActiveField.BRAND -> s.copy(brandSuffix = value)
        }
        _state.value = ns
        refreshPreview()
    }

    fun setTokenAsAnchor(token: String) {
        val s = _state.value
        val text = s.smsText
        val idx = text.indexOf(token)
        if (idx == -1) return

        // 前缀 = token 之前的最短唯一文本（**不含 token 本身**，否则提取结果必为空串——初版缺陷）
        val shortPrefix = findShortUniquePrefix(text, idx)
        // 后缀 = token 之后紧跟的标点/空白；没有则取后一个字符
        val afterText = text.substring(idx + token.length)
        val suffix = afterText.takeWhile { it in "，。；、！？,.!?）)】 ：: \n\t" }
            .ifEmpty { afterText.take(1) }

        val ns = when (s.activeField) {
            ActiveField.TRACKING -> s.copy(trackingPrefix = shortPrefix, trackingSuffix = suffix)
            ActiveField.PICKUP_CODE -> s.copy(codePrefix = shortPrefix, codeSuffix = suffix)
            ActiveField.ADDRESS -> s.copy(addressPrefix = shortPrefix, addressSuffix = suffix)
            ActiveField.PHONE -> s.copy(phonePrefix = shortPrefix, phoneSuffix = suffix)
            // senderPattern 匹配发件方号码而非短信正文，不做前后缀锚点计算，直接取词块本身
            ActiveField.SENDER -> s.copy(senderPattern = token)
            ActiveField.BRAND -> s.copy(brandPrefix = shortPrefix, brandSuffix = suffix)
        }
        _state.value = ns
        refreshPreview()
    }

    /** 找 text[0..targetIdx) 的从右往左最短唯一片段作为前缀锚点（首次出现即目标位置即可） */
    private fun findShortUniquePrefix(text: String, targetIdx: Int): String {
        var start = targetIdx - 1
        while (start > 0) {
            val candidate = text.substring(start, targetIdx)
            if (text.indexOf(candidate) == start) return candidate
            start--
        }
        return text.substring(0, targetIdx)
    }

    fun saveRule() {
        val s = _state.value
        when {
            // 温和文案：不用「错误」「失败」
            s.ruleName.isBlank() -> {
                _events.tryEmit("先给规则起个名字吧～")
                return
            }
            listOf(
                s.trackingPrefix, s.trackingSuffix, s.codePrefix, s.codeSuffix,
                s.addressPrefix, s.addressSuffix, s.phonePrefix, s.phoneSuffix
            ).all { it.isBlank() } -> {
                _events.tryEmit("至少配置一个字段，不然规则匹配不到内容哦")
                return
            }
        }

        viewModelScope.launch {
            val rule = MatchRule(
                id = s.ruleId,
                name = s.ruleName,
                trackingPrefix = s.trackingPrefix.ifEmpty { null },
                trackingSuffix = s.trackingSuffix.ifEmpty { null },
                codePrefix = s.codePrefix.ifEmpty { null },
                codeSuffix = s.codeSuffix.ifEmpty { null },
                addressPrefix = s.addressPrefix.ifEmpty { null },
                addressSuffix = s.addressSuffix.ifEmpty { null },
                phonePrefix = s.phonePrefix.ifEmpty { null },
                phoneSuffix = s.phoneSuffix.ifEmpty { null },
                senderPattern = s.senderPattern.ifEmpty { null },
                brandPrefix = s.brandPrefix.ifEmpty { null },
                brandSuffix = s.brandSuffix.ifEmpty { null },
                sampleSms = s.smsText.ifEmpty { null }
            )
            val wasEdit = s.ruleId != 0L
            if (wasEdit) {
                repo.update(rule)
            } else {
                repo.insert(rule)
            }
            // 轻提示引导下拉重扫（不弹窗打断，v1.1.5 用户裁决）
            UiNotifier.notify(
                if (wasEdit) "规则已更新，回快递页下拉重扫生效" else "已保存，回快递页下拉扫描历史短信"
            )
            _savedEvents.emit(wasEdit)
        }
    }

    private fun refreshPreview() {
        val s = _state.value
        if (s.smsText.isBlank()) {
            _state.value = s.copy(preview = null)
            return
        }
        val rule = MatchRule(
            name = s.ruleName.ifEmpty { "预览" },
            trackingPrefix = s.trackingPrefix.ifEmpty { null },
            trackingSuffix = s.trackingSuffix.ifEmpty { null },
            codePrefix = s.codePrefix.ifEmpty { null },
            codeSuffix = s.codeSuffix.ifEmpty { null },
            addressPrefix = s.addressPrefix.ifEmpty { null },
            addressSuffix = s.addressSuffix.ifEmpty { null },
            phonePrefix = s.phonePrefix.ifEmpty { null },
            phoneSuffix = s.phoneSuffix.ifEmpty { null },
            senderPattern = s.senderPattern.ifEmpty { null },
            brandPrefix = s.brandPrefix.ifEmpty { null },
            brandSuffix = s.brandSuffix.ifEmpty { null }
        )
        // 预览不传 smsAddress（编辑器里没有发件方号码），senderPattern 不参与过滤，预览始终可见
        _state.value = s.copy(preview = MatchEngine.extract(s.smsText, rule))
    }
}
