package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.quickpick.util.JsonExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AiPromptViewModel(application: Application) : AndroidViewModel(application) {

    private val _smsText = MutableStateFlow("")
    val smsText: StateFlow<String> = _smsText

    private val _generatedPrompt = MutableStateFlow("")
    val generatedPrompt: StateFlow<String> = _generatedPrompt

    private val _importedJson = MutableStateFlow("")
    val importedJson: StateFlow<String> = _importedJson

    fun updateSmsText(text: String) {
        _smsText.value = text
    }

    fun generatePrompt() {
        val sms = _smsText.value
        if (sms.isBlank()) return

        _generatedPrompt.value = PROMPT_TEMPLATE.replace("{{SMS_CONTENT}}", sms)
    }

    fun updateImportedJson(json: String) {
        _importedJson.value = json
    }

    /**
     * 校验用户粘贴的 AI 返回内容。
     * 先剥离 markdown 围栏等杂质，合法则返回清理后的 JSON 字符串，否则 null。
     * （字符串传递到编辑器，避开 Parcelable 和进程重建问题）
     */
    fun validatedJson(): String? {
        val json = _importedJson.value
        if (json.isBlank()) return null
        val clean = JsonExporter.sanitizeAiJson(json)
        val result = JsonExporter.import(clean)
        return if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) clean else null
    }

    companion object {
        private val PROMPT_TEMPLATE = """
你是一个快递短信解析专家。我会给你一条快递取件通知短信，请你分析短信内容，
为每个字段提取"前缀"和"后缀"锚点，输出 JSON 格式的提取规则。

字段说明：
- trackingNumber: 快递单号（运单号，如 SF1234567890）
- pickupCode: 取件码 / 快递柜号 / 货架号（取件凭证）
- address: 取件地址 / 驿站名 / 快递柜位置
- phone: 快递员或客服电话（短信中有则提取，无则留空）

规则格式：
每个字段用 prefix（前缀固定文本）和 suffix（后缀固定文本）定位。
提取逻辑：在短信中找到 prefix，取其后内容，直到 suffix 截止。
prefix 和 suffix 尽量取最短、最能唯一定位的文本。
如果短信中没有该字段，prefix 和 suffix 都留空字符串 ""。

输出严格 JSON，不要任何解释：
{
  "name": "快递公司名",
  "fields": {
    "trackingNumber": { "prefix": "单号", "suffix": "，" },
    "pickupCode":    { "prefix": "取件码：", "suffix": "，" },
    "address":       { "prefix": "到达", "suffix": "，" },
    "phone":         { "prefix": "电话：", "suffix": "" }
  }
}

短信内容：
---
{{SMS_CONTENT}}
---

请输出 JSON：
        """.trimIndent()
    }
}
