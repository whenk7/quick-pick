package com.quickpick.corpus

import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.model.ExtractedFields
import com.quickpick.domain.model.MatchRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 语料级测试：50 条真实短信（32 快递 + 18 通知）+ 变体，对全规则集（自建 + 预置）。
 * 发件方过滤在此恒为旁路（smsAddress=null，纯文本）——其独立矩阵见 MatchEngineTest。
 *
 * 断言组：
 * R1 召回——快递逐条逐字段 = golden
 * R2 精确——通知零提取
 * R3 同码——同包裹不同模板/日期提取同码
 * R4 优先级——字段数多者胜的语料实证
 * R6 预置防误报——顺丰/京东对真实语料永不胜出
 * R7 变体——同码异模板 + 边界刻画
 */
class CorpusExtractionTest {

    private val rules = TestRules.allWithPresets

    private fun textOf(id: String): String =
        RAW_SMS.firstOrNull { it.id == id }?.text
            ?: VARIANTS.firstOrNull { it.id == id }?.text
            ?: KNOWN_FAILURES.firstOrNull { it.id == id }?.text
            ?: error("未知语料 id: $id")

    private fun extract(id: String): Pair<MatchRule, ExtractedFields>? =
        MatchEngine.findBestMatch(textOf(id), rules)

    /** 逐字段比对，返回失败行列表（空=全对）。聚合格式：一次报全部，便于分诊 */
    private fun diff(id: String, gold: Expected, result: Pair<MatchRule, ExtractedFields>?): List<String> {
        if (!gold.match) {
            return if (result == null) emptyList()
            else listOf("$id 应不提取，实际命中「${result.first.name}」${result.second}")
        }
        if (result == null) return listOf("$id 应命中「${gold.company}」，实际未匹配")
        val (rule, f) = result
        val out = mutableListOf<String>()
        fun check(field: String, expected: String?, actual: String?) {
            if (expected != actual) out += "$id.$field 期望<$expected> 实际<$actual>"
        }
        check("company", gold.company, rule.name)
        check("companyName", gold.company, f.companyName)
        check("code", gold.code, f.pickupCode)
        check("address", gold.address, f.address)
        check("tracking", gold.tracking, f.trackingNumber)
        check("brand", gold.brand, f.brandName)
        check("phone", null, f.phone)
        return out
    }

    @Test
    fun `R1 every express sms extracts exactly the golden fields`() {
        val failures = RAW_SMS.filter { it.category == SmsCategory.EXPRESS }
            .flatMap { sms -> diff(sms.id, GOLD.getValue(sms.id), extract(sms.id)) }
        if (failures.isNotEmpty()) fail("R1 失败 ${failures.size} 处：\n" + failures.joinToString("\n"))
    }

    @Test
    fun `R2 no notice sms is extracted`() {
        val hits = RAW_SMS.filter { it.category == SmsCategory.NOTICE }
            .mapNotNull { sms -> extract(sms.id)?.let { "${sms.id}→${it.first.name}" } }
        assertTrue(hits.isEmpty(), "通知被误提取：$hits")
    }

    @Test
    fun `R3 same parcel different templates yield same code`() {
        SAME_CODE_GROUPS.forEach { group ->
            val codes = group.map { id -> extract(id)?.second?.pickupCode }
            assertTrue(codes.all { it != null }, "$group 存在未提取：$codes")
            assertEquals(1, codes.distinct().size, "$group 提取码不一致：$codes")
        }
    }

    @Test
    fun `R4 field count priority picks the more specific template rule`() {
        // 超时/滞留两条规则同名字段数不同：tracking 提出来才证明 3 字段规则赢了 2 字段规则
        assertEquals("YT0073828884344", extract("E009")!!.second.trackingNumber)
        assertEquals("YT8896928182079", extract("E019")!!.second.trackingNumber)
    }

    @Test
    fun `R6 preset rules never win on real corpus`() {
        RAW_SMS.forEach { sms ->
            val result = extract(sms.id)
            if (result != null) {
                assertTrue(
                    result.first.name !in TestRules.presetNames,
                    "${sms.id} 被预置规则「${result.first.name}」胜出"
                )
            }
        }
    }

    @Test
    fun `R7 variants behave as designed`() {
        val failures = VARIANTS.flatMap { v -> diff(v.id, v.expected, extract(v.id)) }
        if (failures.isNotEmpty()) fail("R7 失败 ${failures.size} 处：\n" + failures.joinToString("\n"))
    }

    @Test
    fun `corpus integrity - 32 express 31 goldens 18 notices`() {
        assertEquals(50, RAW_SMS.size)
        assertEquals(32, RAW_SMS.count { it.category == SmsCategory.EXPRESS })
        assertEquals(18, RAW_SMS.count { it.category == SmsCategory.NOTICE })
        assertEquals(32, GOLD.size)
        // golden 覆盖每条快递语料，无悬空
        RAW_SMS.filter { it.category == SmsCategory.EXPRESS }.forEach {
            assertNotNull(GOLD[it.id], "${it.id} 缺 golden")
        }
    }
}
