package com.quickpick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickpick.QuickPickApp
import com.quickpick.data.repository.RuleRepository
import com.quickpick.domain.model.MatchRule
import com.quickpick.util.JsonExporter
import com.quickpick.util.SmsReader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AI 生成规则页（v1.1.4 重写）：
 * 自动挑短信样本（快递为主 + 少量通知当反面教材）→ 用户逐条审查移除 → 手动补齐 →
 * 生成含样本的完整提示词一键复制 → 粘贴 AI 返回的 JSON → 校验入库。
 * 隐私：只取正文进样本，发送方/时间不进提示词。
 */
class AiPromptViewModel(application: Application) : AndroidViewModel(application) {

    data class SmsSample(val id: String, val body: String, val isExpress: Boolean)

    companion object {
        private const val EXPRESS_TARGET = 15
        private const val NOTICE_TARGET = 5
        private val EXPRESS_KEYWORDS = listOf("取件码", "取货码", "包裹", "驿站", "快递柜", "丰巢", "菜鸟", "取件")
        private const val NINETY_DAYS_MS = 90L * 24 * 3600 * 1000

        /**
         * AI 提示词模板（2026-10-23 重写）：完整新 schema + 引擎语义约束。
         * 同步维护 .res/ai-prompt-template.md（文档侧描述用，运行时用这份）。
         */
        private val PROMPT_TEMPLATE = """
你是一名快递取件短信解析专家。我会给你一批真实短信样本——含取件短信，也含不该被提取的通知短信。请为每个「取件短信模板」生成提取规则。

# 规则引擎语义（必须严格遵守）
- 引擎不用正则：每个字段靠「前缀锚点 prefix + 后缀锚点 suffix」纯文本定位
- 找到 prefix 后取其后内容，直到 suffix 截止；prefix 或 suffix 留空字符串 = 不提取该字段
- suffix 留空 = 取到短信末尾（适合字段在句尾）；suffix 填了但短信里找不到 = 该字段不提取（不会兜底取到文末）
- 定位取「首次出现」：若锚词在短信前半段已出现（如「到」在「已到站」里），锚点必须加长到唯一（如「到咸阳」），宁长勿短
- brand 字段统一 prefix="【" suffix="】"（取短信签名作品牌）

# 输出 JSON Schema（严格输出，不要任何解释文字）
[
  {
    "name": "快递公司/驿站名（会显示在卡片标题）",
    "senderPattern": "发件方号码子串过滤，没把握就留空字符串",
    "fields": {
      "trackingNumber": {"prefix": "", "suffix": ""},
      "pickupCode": {"prefix": "", "suffix": ""},
      "address": {"prefix": "", "suffix": ""},
      "phone": {"prefix": "", "suffix": ""},
      "brand": {"prefix": "【", "suffix": "】"}
    }
  }
]

# 要求
- pickupCode 是核心字段，每条取件短信的规则必须能提取到；取件码形态多样（纯数字/横杠分段），锚点别把数字吃进去
- 同一发送方可能有多种模板（到达提醒/超时催取），生成多条同名规则覆盖
- 标记「通知」的样本是反面教材：不要为它们生成规则，并确保生成的规则不会误提取它们
- 输出必须是合法 JSON 数组；可以带 markdown 代码围栏（导入方会自动剥除）

# 我的短信样本（均为正文，已人工审查脱敏）
{{SAMPLES}}
        """.trimIndent()
    }

    private val ruleRepo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )

    private val _samples = MutableStateFlow<List<SmsSample>>(emptyList())
    val samples: StateFlow<List<SmsSample>> = _samples

    /** 候选池还有存货可补 */
    private val _canRefill = MutableStateFlow(false)
    val canRefill: StateFlow<Boolean> = _canRefill

    /** 短信读取失败（权限等）：samples 为空且此标记为真 → 页面显示引导 */
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events

    // 导入冲突裁决（与规则页同一套流程）
    data class ImportConflict(val incoming: MatchRule, val existingId: Long)

    private val _conflict = MutableStateFlow<ImportConflict?>(null)
    val conflict: StateFlow<ImportConflict?> = _conflict

    private var pendingImport = mutableListOf<MatchRule>()
    private var importedCount = 0
    private var skippedCount = 0

    // 补齐候选池（已按时间倒序、按签名去重）
    private var expressPool = ArrayDeque<SmsSample>()
    private var noticePool = ArrayDeque<SmsSample>()

    private var loaded = false

    /** 首次进入页面时加载样本（重复调用幂等） */
    fun loadSamplesOnce() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val list = runCatching {
                SmsReader.readSince(getApplication(), System.currentTimeMillis() - NINETY_DAYS_MS)
            }.getOrNull()
            if (list == null) {
                _loadFailed.value = true
                return@launch
            }
            val (express, rest) = list.partition { s -> EXPRESS_KEYWORDS.any { k -> s.body.contains(k) } }
            // 同签名（【】内容）去重：AI 要模板多样性，不要 7 条一样的兔喜
            val expressDedup = express.distinctBy { signatureOf(it.body) }
            val noticeDedup = rest.filter { it.body.contains("【") }.distinctBy { signatureOf(it.body) }

            _samples.value = expressDedup.take(EXPRESS_TARGET).map { SmsSample(it.id, it.body, true) } +
                noticeDedup.take(NOTICE_TARGET).map { SmsSample(it.id, it.body, false) }
            expressPool = ArrayDeque(expressDedup.drop(EXPRESS_TARGET).map { SmsSample(it.id, it.body, true) })
            noticePool = ArrayDeque(noticeDedup.drop(NOTICE_TARGET).map { SmsSample(it.id, it.body, false) })
            updateCanRefill()
        }
    }

    /** 签名（【】内容）或正文前 12 字作去重键 */
    private fun signatureOf(body: String): String =
        if (body.startsWith("【")) body.substringBefore("】") else body.take(12)

    fun removeSample(id: String) {
        _samples.value = _samples.value.filterNot { it.id == id }
        updateCanRefill()
    }

    /** 手动补齐（用户裁决：不自动塞，避免一直补进不合适的短信） */
    fun refill() {
        val current = _samples.value
        val expressNeed = EXPRESS_TARGET - current.count { it.isExpress }
        val noticeNeed = NOTICE_TARGET - current.count { !it.isExpress }
        val picked = mutableListOf<SmsSample>()
        repeat(expressNeed.coerceAtLeast(0)) { expressPool.removeFirstOrNull()?.let(picked::add) }
        repeat(noticeNeed.coerceAtLeast(0)) { noticePool.removeFirstOrNull()?.let(picked::add) }
        if (picked.isNotEmpty()) _samples.value = current + picked
        updateCanRefill()
    }

    private fun updateCanRefill() {
        val current = _samples.value
        val short = current.count { it.isExpress } < EXPRESS_TARGET ||
            current.count { !it.isExpress } < NOTICE_TARGET
        _canRefill.value = short && (expressPool.isNotEmpty() || noticePool.isNotEmpty())
    }

    /** 生成完整提示词（含已确认的样本正文） */
    fun buildPrompt(): String {
        val sampleText = _samples.value.joinToString("\n\n") { s ->
            (if (s.isExpress) "【样本·取件】\n" else "【样本·通知（不要为它生成规则）】\n") + s.body
        }
        return PROMPT_TEMPLATE.replace("{{SAMPLES}}", sampleText)
    }

    /** 粘贴的 JSON 入库：无同名直接新增；有同名进冲突队列（覆盖/跳过），同规则页流程 */
    fun importPasted(raw: String) {
        if (raw.isBlank()) {
            _events.tryEmit("先把 AI 返回的 JSON 贴进来～")
            return
        }
        val result = JsonExporter.import(JsonExporter.sanitizeAiJson(raw))
        val list = result.getOrNull()
        if (list == null) {
            _events.tryEmit("JSON 没解析出来，检查下是不是完整复制了")
            return
        }
        importedCount = 0
        skippedCount = 0
        pendingImport = list.toMutableList()
        viewModelScope.launch { processNext() }
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

    fun resolveConflict(overwrite: Boolean) {
        val c = _conflict.value ?: return
        _conflict.value = null
        viewModelScope.launch {
            if (overwrite) {
                ruleRepo.update(c.incoming.copy(id = c.existingId))
                importedCount++
            } else {
                skippedCount++
            }
            processNext()
        }
    }
}
