package com.quickpick.domain.engine

import com.quickpick.corpus.GOLD
import com.quickpick.corpus.RAW_SMS
import com.quickpick.corpus.SmsCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 锚点推导（2026-11-08 修复）：
 * 旧实现拿「用户点中的整个词块」当锚点基准，而分词只按标点切，取件码常和正文粘在一块的
 * （「请凭取件码01259354到绿洲家园西南门袋鼠柜B09取您的快递」），生成的规则会把整段话
 * 当取件码。实测真实语料 31 条里 0 条正确。
 *
 * 现在的契约：suggestAnchor 必须做到「按它给出的 prefix/suffix 提取，恰好得到确认过的值」，
 * 做不到就返回 null（宁可不填，也不生成坏规则）。
 */
class AnchorSuggestionTest {

    /** 复刻 RuleEditorScreen.TokenGrid 的分词（与 UI 保持一致，改动时两边同步） */
    private fun tokenize(smsText: String): List<String> {
        val result = mutableListOf<String>()
        var current = ""
        for (ch in smsText) {
            if (ch in "【】《》（）()，。；、：！？,.!? \n\t") {
                if (current.isNotEmpty()) { result.add(current); current = "" }
                if (!ch.isWhitespace()) result.add(ch.toString())
            } else {
                current += ch
            }
        }
        if (current.isNotEmpty()) result.add(current)
        return result
    }

    // ============ 核心契约：真实语料全量自洽 ============

    /**
     * 多码短信（E004/E021）单列：一条短信两个包裹两个码，而 golden 记为「整串」，
     * 分词后码本身被逗号切成两块——单值锚点模型对它天然无解，属于已知边界
     * （见 KnownFindings/SmsCorpusGold 注释），不纳入本契约。
     */
    private val MULTI_CODE_IDS = setOf("E004", "E021")

    @Test
    fun `every express code chip yields an anchor that reproduces the golden code`() {
        val express = RAW_SMS.filter {
            it.category == SmsCategory.EXPRESS && GOLD[it.id]?.match == true && it.id !in MULTI_CODE_IDS
        }
        val failures = mutableListOf<String>()
        var checked = 0

        express.forEach { sms ->
            val golden = GOLD.getValue(sms.id).code ?: return@forEach
            val chip = tokenize(sms.text).firstOrNull { it.contains(golden) }
                ?: run { failures += "${sms.id} 词块里找不到取件码<$golden>"; return@forEach }
            val sug = suggestAnchor(sms.text, chip, AnchorField.PICKUP_CODE)
            if (sug == null) {
                failures += "${sms.id} 未推导出锚点（点「$chip」）"
                return@forEach
            }
            val got = extractByAnchor(sms.text, sug.prefix, sug.suffix)
            if (got != golden) {
                failures += "${sms.id} 值=<$golden> 实得<$got> prefix=<${sug.prefix}> suffix=<${sug.suffix}>"
            } else {
                checked++
            }
        }

        if (failures.isNotEmpty()) fail("锚点自洽失败 ${failures.size} 处：\n" + failures.joinToString("\n"))
        assertTrue(checked >= 27, "至少覆盖 27 条单码语料，实际 $checked")
    }

    /** 生成的规则集在全部语料上互相不干扰（不是只测单条） */
    @Test
    fun `rules generated from chip clicks do not interfere with each other`() {
        val express = RAW_SMS.filter {
            it.category == SmsCategory.EXPRESS && GOLD[it.id]?.match == true && it.id !in MULTI_CODE_IDS
        }
        val failures = mutableListOf<String>()
        val rules = express.mapNotNull { sms ->
            val golden = GOLD.getValue(sms.id).code ?: return@mapNotNull null
            val chip = tokenize(sms.text).firstOrNull { it.contains(golden) } ?: return@mapNotNull null
            suggestAnchor(sms.text, chip, AnchorField.PICKUP_CODE)?.let { s ->
                com.quickpick.domain.model.MatchRule(
                    name = "R-${sms.id}",
                    codePrefix = s.prefix, codeSuffix = s.suffix,
                    brandPrefix = "【", brandSuffix = "】"
                )
            } ?: run { failures += "${sms.id} 无锚点"; null }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))

        express.forEach { sms ->
            val golden = GOLD.getValue(sms.id).code
            val r = MatchEngine.findBestMatch(sms.text, rules, null)
            if (r?.second?.pickupCode != golden) failures += "${sms.id} 期望<$golden> 实得<${r?.second?.pickupCode}>"
        }
        if (failures.isNotEmpty()) fail("规则互相干扰：\n" + failures.joinToString("\n"))
    }

    // ============ 形态识别 ============

    @Test
    fun `code detection accepts real pickup code shapes`() {
        listOf("01259354", "2946-3356", "11-2-0272", "6-2-0012", "19-55", "4-5-0877").forEach {
            assertTrue(looksLikeCode(it), "<$it> 应识别为取件码形态")
        }
    }

    @Test
    fun `code detection rejects prose and single digits`() {
        listOf("取件码为", "请凭取件码", "绿洲家园", "7", "12").forEach {
            assertTrue(!looksLikeCode(it), "<$it> 不应识别为取件码形态")
        }
    }

    @Test
    fun `address detection accepts station and street names`() {
        listOf("绿洲家园7号楼商铺S1001号", "咸阳绿洲家园西门店", "科创中心橡树A座丰巢柜", "花园路绿洲家园门1002号")
            .forEach { assertTrue(looksLikeAddress(it), "<$it> 应识别为地址") }
    }

    // ============ 边界 ============

    @Test
    fun `value glued to leading prose still extracts only the value`() {
        val text = "【袋鼠智柜】快递员提醒您，请凭取件码01259354到绿洲家园西南门袋鼠柜B09取您的快递"
        val chip = "请凭取件码01259354到绿洲家园西南门袋鼠柜B09取您的快递"
        val sug = assertNotNull(suggestAnchor(text, chip, AnchorField.PICKUP_CODE))
        assertEquals("01259354", sug.value)
        assertEquals("01259354", extractByAnchor(text, sug.prefix, sug.suffix))
    }

    @Test
    fun `code at end of sms with no trailing delimiter falls back to end of text`() {
        val text = "【丰巢】凭取件码28026838至科创中心橡树A座丰巢柜取件。"
        val chip = "凭取件码28026838至科创中心橡树A座丰巢柜取件"
        val sug = assertNotNull(suggestAnchor(text, chip, AnchorField.PICKUP_CODE))
        assertEquals("28026838", extractByAnchor(text, sug.prefix, sug.suffix))
    }

    @Test
    fun `chip that is pure prose yields no anchor`() {
        // 「您有圆通快递包裹」里没有数字，不该为取件码字段推出锚点
        val text = "【妈妈驿站】取货码19-55，您有圆通快递包裹，已到花园路绿洲家园门1002号"
        assertNull(suggestAnchor(text, "您有圆通快递包裹", AnchorField.PICKUP_CODE))
    }

    @Test
    fun `chip not present in text yields no anchor`() {
        assertNull(suggestAnchor("【兔喜生活】取件码为11-2-0272", "不存在的块", AnchorField.PICKUP_CODE))
    }

    @Test
    fun `empty text or token yields no anchor`() {
        assertNull(suggestAnchor("", "11-2-0272", AnchorField.PICKUP_CODE))
        assertNull(suggestAnchor("取件码11-2-0272", "", AnchorField.PICKUP_CODE))
    }

    @Test
    fun `brand field always uses sms signature brackets`() {
        val text = "【兔喜生活】您有包裹已到达绿洲家园便利店，取件码为11-2-0272"
        val sug = assertNotNull(suggestAnchor(text, "兔喜生活", AnchorField.BRAND))
        assertEquals("【", sug.prefix)
        assertEquals("】", sug.suffix)
        assertEquals("兔喜生活", extractByAnchor(text, sug.prefix, sug.suffix))
    }

    @Test
    fun `sender field is not a body anchor`() {
        // 发件方匹配号码不是正文，由 ViewModel 直接取词块，这里返回 null
        assertNull(suggestAnchor("【兔喜生活】取件码为11-2-0272", "兔喜生活", AnchorField.SENDER))
    }

    @Test
    fun `address chip yields an anchor that reproduces the address`() {
        val text = "【兔喜生活】您有包裹已到达绿洲家园便利店，取件码为11-2-0272，地址:绿洲家园7号楼商铺S1001号"
        val chip = tokenize(text).first { it.startsWith("地址:") }
        val sug = assertNotNull(suggestAnchor(text, chip, AnchorField.ADDRESS))
        assertEquals("绿洲家园7号楼商铺S1001号", extractByAnchor(text, sug.prefix, sug.suffix))
    }

    @Test
    fun `extractByAnchor respects first occurrence and missing suffix`() {
        assertEquals("11-2-0272", extractByAnchor("取件码为11-2-0272，地址:家", "取件码为", "，"))
        assertNull(extractByAnchor("取件码为11-2-0272", "取件码为", "不存在的后缀"))
        assertNull(extractByAnchor("没有这个锚点", "取件码为", "，"))
        // suffix 为空 → 读到文末
        assertEquals("11-2-0272", extractByAnchor("取件码为11-2-0272", "取件码为", ""))
    }
}
