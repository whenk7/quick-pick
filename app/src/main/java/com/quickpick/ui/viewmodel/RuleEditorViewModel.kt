package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.util.UiNotifier
import com.quickpick.domain.engine.AnchorField
import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.engine.suggestAnchor
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

/** 字段中文名（提示文案用；RuleEditorScreen 有同名私有函数负责 UI 展示） */
private fun fieldLabel(field: ActiveField): String = when (field) {
    ActiveField.TRACKING -> "单号"
    ActiveField.PICKUP_CODE -> "取件码"
    ActiveField.ADDRESS -> "地址"
    ActiveField.PHONE -> "电话"
    ActiveField.SENDER -> "发件方"
    ActiveField.BRAND -> "品牌"
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

    /**
     * 按字段直接写锚点（v1.1.11 修复 FieldCard 串字段）。
     *
     * 原实现只有「写当前激活字段」一种入口。而在真实交互里：TextField 会先消费掉点击
     * （miuix Card 的 combinedClickable 要求 down 未被消费才触发 onClick），所以直接点
     * 「取件地址」卡片的输入框时，activeField 仍是上一个字段（取件码）——地址内容被写进
     * 取件码前缀，且没有任何提示。改为按字段写入，不再依赖激活态。
     */
    fun updatePrefix(field: ActiveField, value: String) {
        val s = _state.value
        _state.value = when (field) {
            ActiveField.TRACKING -> s.copy(trackingPrefix = value)
            ActiveField.PICKUP_CODE -> s.copy(codePrefix = value)
            ActiveField.ADDRESS -> s.copy(addressPrefix = value)
            ActiveField.PHONE -> s.copy(phonePrefix = value)
            ActiveField.SENDER -> s.copy(senderPattern = value)
            ActiveField.BRAND -> s.copy(brandPrefix = value)
        }
        refreshPreview()
    }

    fun updateSuffix(field: ActiveField, value: String) {
        val s = _state.value
        // senderPattern 是单字符串匹配，无后缀概念
        if (field == ActiveField.SENDER) return
        _state.value = when (field) {
            ActiveField.TRACKING -> s.copy(trackingSuffix = value)
            ActiveField.PICKUP_CODE -> s.copy(codeSuffix = value)
            ActiveField.ADDRESS -> s.copy(addressSuffix = value)
            ActiveField.PHONE -> s.copy(phoneSuffix = value)
            ActiveField.BRAND -> s.copy(brandSuffix = value)
            else -> s
        }
        refreshPreview()
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
        if (text.isEmpty() || token !in text) return

        // 发件方匹配的是号码不是正文，直接取词块本身（无前后缀概念，UI 也不显示后缀框）
        if (s.activeField == ActiveField.SENDER) {
            _state.value = s.copy(senderPattern = token)
            refreshPreview()
            return
        }

        // 品牌固定取【】签名
        if (s.activeField == ActiveField.BRAND) {
            if (token != "【" && token != "】") {
                _state.value = s.copy(brandPrefix = "【", brandSuffix = "】")
                refreshPreview()
            }
            return
        }

        // 其余字段：从词块内部识别出真正的值，再算锚点。
        // 分词只按标点切，取件码常与正文粘成一块，直接拿整块当基准会提取出一整段话
        //（2026-11-08 修复：旧实现 31 条真实语料 0 条正确，现 29/29）。
        val field = when (s.activeField) {
            ActiveField.TRACKING -> AnchorField.TRACKING
            ActiveField.PICKUP_CODE -> AnchorField.PICKUP_CODE
            ActiveField.ADDRESS -> AnchorField.ADDRESS
            ActiveField.PHONE -> AnchorField.PHONE
            else -> null
        } ?: return

        val suggestion = suggestAnchor(text, token, field)
        if (suggestion != null) {
            val ns = when (s.activeField) {
                ActiveField.TRACKING -> s.copy(trackingPrefix = suggestion.prefix, trackingSuffix = suggestion.suffix)
                ActiveField.PICKUP_CODE -> s.copy(codePrefix = suggestion.prefix, codeSuffix = suggestion.suffix)
                ActiveField.ADDRESS -> s.copy(addressPrefix = suggestion.prefix, addressSuffix = suggestion.suffix)
                ActiveField.PHONE -> s.copy(phonePrefix = suggestion.prefix, phoneSuffix = suggestion.suffix)
                else -> s
            }
            _state.value = ns
        } else {
            // 推不出发可信锚点：不动输入框，提示用户手动填（不生成坏规则）
            _events.tryEmit("这块读不出${fieldLabel(s.activeField)}，换个词块或手动填前后缀～")
        }
        refreshPreview()
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
            // 规则变了：打标记，首页回到前台时会自动全量重扫（v1.1.11）
            (getApplication() as QuickPickApp).settingsStore.markRulesDirty()
            // 轻提示引导下拉重扫（不弹窗打断，v1.1.5 用户裁决；v1.1.7 §3 文案精简统一）
            UiNotifier.notify("已保存「${s.ruleName}」，请首页下拉刷新")
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
