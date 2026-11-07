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
        // 用于把「疑似快递」样本挑进 AI 提示词。真实语料实测：漏掉「滞留」会让
        // 「【申通快递】…已在…滞留1天未取，请尽快取件」这种纯催取模板整条漏掉，
        // 所以补上滞留/超时/到站/智柜/便民商店等高频词（2026-11-08）。
        private val EXPRESS_KEYWORDS = listOf(
            "取件码", "取货码", "取件", "包裹", "驿站", "快递柜", "快递", "丰巢", "菜鸟", "袋鼠",
            "兔喜", "妈妈驿站", "滞留", "超时", "到站", "到达", "便民商店", "帮便利"
        )
        private const val NINETY_DAYS_MS = 90L * 24 * 3600 * 1000

        /**
         * AI 提示词模板（2026-10-23 重写）：完整新 schema + 引擎语义约束。
         * 同步维护 .res/ai-prompt-template.md（文档侧描述用，运行时用这份）。
         */
        private val PROMPT_TEMPLATE = """
你是一名快递取件短信解析专家。我会给你一批真实短信样本——含取件短信，也含不该被提取的通知短信。请为每个「取件短信模板」生成提取规则。

# 规则引擎语义（必须严格遵守）
- 引擎不用正则：每个字段靠「前缀锚点 prefix + 后缀锚点 suffix」纯文本定位
- 找到 prefix 后取其后内容，直到 suffix 截止；prefix 留空 = 不提取该字段
- suffix 留空 = 取到短信末尾；suffix 填了但短信里找不到 = 该字段不提取（不会兜底取到文末）
- 定位取「首次出现」的 prefix：若锚词在短信前半段已出现过，锚点必须加长到唯一
- 只支持三个字段：pickupCode（取件码，核心）、address（取件地址）、brand（品牌签名，固定 prefix="【" suffix="】"）

# 锚点的两个硬性要求（最容易出错的地方，务必照做）

1. prefix 必须带上引导词，不要只写最后的那个动词
   ✗ 错：pickupCode prefix="，凭"     → 太短，容易撞到别的句子
   ✓ 对：pickupCode prefix="请凭取件码"
   引导词长一点没坏处，短锚点才是灾难。

2. 多个字段的锚点不要互相撞车
   引擎对一条短信跑所有规则，然后**取提取字段最多的那条**。所以：
   - 如果 A 规则能提 2 个字段、B 规则只能提 1 个，那么 B 能匹配的短信会被 A 抢走
   - address 的 prefix 尤其危险：`到` 这种单字会撞上「已到站」里的「到」
   ✗ 错：address prefix="到"
   ✓ 对：address prefix="到咸阳"（带上地名，确保唯一）

# 输出 JSON Schema（严格输出，不要任何解释文字）
[
  {
    "name": "快递公司/驿站名（会显示在卡片标题，如「兔喜生活」「袋鼠智柜」）",
    "senderPattern": "",
    "fields": {
      "trackingNumber": {"prefix": "", "suffix": ""},
      "pickupCode": {"prefix": "", "suffix": ""},
      "address": {"prefix": "", "suffix": ""},
      "phone": {"prefix": "", "suffix": ""},
      "brand": {"prefix": "【", "suffix": "】"}
    }
  }
]
（trackingNumber / phone 已废弃，一律留空字符串，不要填）

# 一个真实的完整例子

短信：【袋鼠智柜】快递员提醒您，请凭取件码10000001到阳光小区西南门袋鼠柜A01取您的快递
正确规则：pickupCode prefix="请凭取件码" suffix="到"，address prefix="到" suffix="取您的快递"
→ 提取结果：码=10000001，地址=阳光小区西南门袋鼠柜A01

# 要求
- pickupCode 是核心字段，每条取件短信的规则必须能提取到；锚点别把数字吃进去
- 同一家公司可能有好几种模板（到达提醒 / 超时催取 / 滞留），每种模板单独一条规则，用不同 name 区分（如「袋鼠智柜·标准」「袋鼠智柜·超时」）
- 标记「通知」的样本是反面教材：不要为它们生成规则，并确保生成的规则不会误提取它们
- 退货/上门取件的短信里也会出现「取件码」，那是寄件不是取件，不要为它生成规则，也不要让它被已有规则误提取
- 输出必须是合法 JSON 数组；可以带 markdown 代码围栏（导入方会自动剥除）

# 我的短信样本（均为正文，已人工审查脱敏）
{{SAMPLES}}
        """.trimIndent()
    }

    private val ruleRepo = RuleRepository(
        (application as QuickPickApp).database.matchRuleDao()
    )
    private val smsRepo = com.quickpick.data.repository.SmsRepository(
        (application as QuickPickApp).database.scannedSmsDao()
    )
    private val settingsStore = (application as QuickPickApp).settingsStore

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
            // 按「签名 + 句式骨架」去重，不能只按签名（2026-11-08 修复）：
            // 同家公司常有多种模板（袋鼠智柜的「请凭取件码X到Y」/「您存放在X的快递Y超时未取」
            // /「…已滞留」），只按【签名】去重会把后两种整条砍掉——AI 拿不到样本就生成不出对应规则，
            // 结果就是用户导入 AI 规则后仍有大批短信识别不到。
            val expressDedup = express.distinctBy { templateKeyOf(it.body) }
            val noticeDedup = rest.filter { it.body.contains("【") }.distinctBy { templateKeyOf(it.body) }

            _samples.value = expressDedup.take(EXPRESS_TARGET).map { SmsSample(it.id, it.body, true) } +
                noticeDedup.take(NOTICE_TARGET).map { SmsSample(it.id, it.body, false) }
            expressPool = ArrayDeque(expressDedup.drop(EXPRESS_TARGET).map { SmsSample(it.id, it.body, true) })
            noticePool = ArrayDeque(noticeDedup.drop(NOTICE_TARGET).map { SmsSample(it.id, it.body, false) })
            updateCanRefill()
        }
    }

    /** 签名（【】内容）或正文前 12 字 */
    private fun signatureOf(body: String): String =
        if (body.startsWith("【")) body.substringBefore("】") else body.take(12)

    /**
     * 模板指纹：签名 + 句式骨架（去掉所有数字和标点后的前 24 字）。
     *
     * 「袋鼠智柜·请凭取件码01259354到…」和「袋鼠智柜·您存放在…超时未取」签名相同、
     * 骨架不同 → 算两种模板，都要进样本；而「…取件码01259354…」与「…取件码2946-3356…」
     * 骨架相同 → 只留一条。这样既有模板多样性，又不会塞一堆重复。
     */
    private fun templateKeyOf(body: String): String {
        val sig = signatureOf(body)
        val skeleton = body.dropWhile { it != '】' }.drop(1)
            .filterNot { it.isDigit() || it in "，。；、！？,.!?：: （）()【】《》\n\r\t -" }
            .take(24)
        return "$sig|$skeleton"
    }

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
            // 导入结束：打规则变更标记，回首页自动全量重扫（v1.1.11）
            if (importedCount > 0) settingsStore.markRulesDirty()
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
