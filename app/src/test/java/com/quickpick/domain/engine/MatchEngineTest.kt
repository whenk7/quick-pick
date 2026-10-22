package com.quickpick.domain.engine

import com.quickpick.domain.model.MatchRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 引擎语义钉死：extractField 锚点行为 + senderPattern 过滤 + findBestMatch 优先级。
 * 语料级测试见 CorpusExtractionTest。
 */
class MatchEngineTest {

    private fun rule(
        name: String = "测试快递",
        codePrefix: String? = null, codeSuffix: String? = null,
        addressPrefix: String? = null, addressSuffix: String? = null,
        trackingPrefix: String? = null, trackingSuffix: String? = null,
        senderPattern: String? = null
    ) = MatchRule(
        name = name,
        codePrefix = codePrefix, codeSuffix = codeSuffix,
        addressPrefix = addressPrefix, addressSuffix = addressSuffix,
        trackingPrefix = trackingPrefix, trackingSuffix = trackingSuffix,
        senderPattern = senderPattern
    )

    // ---- 基础锚点语义（现状正确行为，E1 改动后必须保持绿）----

    @Test
    fun `extracts field between prefix and suffix`() {
        val r = rule(codePrefix = "取件码", codeSuffix = "，")
        val f = MatchEngine.extract("凭取件码12345，请取件", r)
        assertEquals("12345", f.pickupCode)
    }

    @Test
    fun `empty suffix reads to end of text`() {
        val r = rule(addressPrefix = "地址:", addressSuffix = "")
        val f = MatchEngine.extract("取件码1-2-3，地址:绿洲家园便利", r)
        assertEquals("绿洲家园便利", f.address)
    }

    @Test
    fun `null suffix also reads to end of text`() {
        val r = rule(addressPrefix = "地址:", addressSuffix = null)
        val f = MatchEngine.extract("地址:科创中心A座", r)
        assertEquals("科创中心A座", f.address)
    }

    @Test
    fun `prefix not found returns null`() {
        val r = rule(codePrefix = "取件码", codeSuffix = "，")
        val f = MatchEngine.extract("完全没有锚点的短信", r)
        assertNull(f.pickupCode)
    }

    @Test
    fun `prefix at very end of text returns null`() {
        val r = rule(codePrefix = "取件码", codeSuffix = null)
        val f = MatchEngine.extract("你的取件码", r)
        assertNull(f.pickupCode)
    }

    @Test
    fun `empty extracted content returns null not empty string`() {
        // prefix 紧跟 suffix，中间是空的 → 视为未匹配，防止空串污染 matchedCount
        val r = rule(codePrefix = "取件码为", codeSuffix = "，")
        val f = MatchEngine.extract("取件码为，地址:某地", r)
        assertNull(f.pickupCode)
    }

    @Test
    fun `extracted content is trimmed`() {
        val r = rule(codePrefix = "取件码为", codeSuffix = "，")
        val f = MatchEngine.extract("取件码为  8-3-0093 ，地址:x", r)
        assertEquals("8-3-0093", f.pickupCode)
    }

    // ---- E1：suffix 配了但没找到 → null，不吞文末 ----

    @Test
    fun `configured suffix missing yields null instead of trailing garbage`() {
        // 旧行为：「8888 请尽快取件」整段当取件码入库（垃圾且计入 matchedCount 抢优先级）
        val r = rule(codePrefix = "取件码：", codeSuffix = "，")
        val f = MatchEngine.extract("【顺丰速运】您的包裹已到，取件码：8888 请尽快取件", r)
        assertNull(f.pickupCode, "suffix「，」在 prefix 之后不存在时应判未匹配，而不是吞到文末")
    }

    @Test
    fun `configured suffix missing on one field does not affect other fields`() {
        val r = rule(
            codePrefix = "取件码：", codeSuffix = "，",      // 会 miss → null
            addressPrefix = "地址:", addressSuffix = ""     // 空 suffix 仍取到文末
        )
        val f = MatchEngine.extract("取件码：8888 地址:绿洲家园", r)
        assertNull(f.pickupCode)
        assertEquals("绿洲家园", f.address)
    }

    // ---- senderPattern 发件方过滤 ----

    @Test
    fun `senderPattern null never filters`() {
        val r = rule(codePrefix = "取件码", codeSuffix = "，", senderPattern = null)
        val f = MatchEngine.extract("取件码123，", r, smsAddress = "95588")
        assertEquals("123", f.pickupCode)
    }

    @Test
    fun `null smsAddress skips sender filter`() {
        // 编辑器预览路径：无发件方信息时不过滤（向后兼容）
        val r = rule(codePrefix = "取件码", codeSuffix = "，", senderPattern = "1069")
        val f = MatchEngine.extract("取件码123，", r, smsAddress = null)
        assertEquals("123", f.pickupCode)
    }

    @Test
    fun `empty smsAddress is filtered by senderPattern`() {
        // 真实扫描路径：SmsReader 把 null ADDRESS 兜底成 ""，空串不可能含锚点 → 被过滤
        val r = rule(codePrefix = "取件码", codeSuffix = "，", senderPattern = "1069")
        val f = MatchEngine.extract("取件码123，", r, smsAddress = "")
        assertNull(f.pickupCode)
    }

    @Test
    fun `sender containing pattern passes filter`() {
        val r = rule(codePrefix = "取件码", codeSuffix = "，", senderPattern = "1069")
        val f = MatchEngine.extract("取件码123，", r, smsAddress = "1069411071189531")
        assertEquals("123", f.pickupCode)
    }

    @Test
    fun `sender not containing pattern is filtered`() {
        val r = rule(codePrefix = "取件码", codeSuffix = "，", senderPattern = "1069")
        val f = MatchEngine.extract("取件码123，", r, smsAddress = "95588")
        assertNull(f.pickupCode)
    }

    // ---- findBestMatch 优先级与闸口 ----

    @Test
    fun `rule with more matched fields wins`() {
        val weak = rule(name = "弱", codePrefix = "取件码", codeSuffix = "，")
        val strong = rule(
            name = "强", codePrefix = "取件码", codeSuffix = "，",
            addressPrefix = "地址:", addressSuffix = ""
        )
        // 顺序无关：字段多者胜
        val result = MatchEngine.findBestMatch(
            "取件码123，地址:某地", listOf(strong, weak)
        )
        assertNotNull(result)
        assertEquals("强", result.first.name)
    }

    @Test
    fun `tie on field count picks earlier rule in list`() {
        val a = rule(name = "先", codePrefix = "取件码", codeSuffix = "，")
        val b = rule(name = "后", codePrefix = "取件码", codeSuffix = "，")
        val result = MatchEngine.findBestMatch("取件码123，", listOf(a, b))
        assertEquals("先", result?.first?.name)
        val reversed = MatchEngine.findBestMatch("取件码123，", listOf(b, a))
        assertEquals("后", reversed?.first?.name)
    }

    @Test
    fun `rule without any configured field is skipped`() {
        val empty = rule(name = "空规则")
        val result = MatchEngine.findBestMatch("取件码123，", listOf(empty))
        assertNull(result)
    }

    @Test
    fun `match without code or address is not pickup related`() {
        // 闸口：只命中 tracking/phone 不算取件短信（抖音售后类误报的最后防线）
        val r = rule(name = "仅单号", trackingPrefix = "订单", trackingSuffix = "，")
        val result = MatchEngine.findBestMatch("订单GJ008，售后已通过", listOf(r))
        assertNull(result)
    }
}
