package com.quickpick.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AI 页第 3 步「检查并导入」的完整链路（v1.1.11 复查）。
 *
 * 用户实际会贴进来的东西五花八门：带 ```json 围栏的、前后带解释文字的、
 * 单个对象而不是数组的、字段缺省的。这条链路必须全部接得住。
 */
class AiImportChainTest {

    private val validRule = """
        {
          "name": "测试快递",
          "senderPattern": "",
          "fields": {
            "pickupCode": {"prefix": "取件码为", "suffix": "，"},
            "address": {"prefix": "地址:", "suffix": ""},
            "brand": {"prefix": "【", "suffix": "】"}
          }
        }
    """.trimIndent()

    /** 复刻 AiPromptViewModel.importPasted 的解析部分 */
    private fun parse(raw: String) =
        JsonExporter.import(JsonExporter.sanitizeAiJson(raw)).getOrNull()

    @Test
    fun `plain json array imports`() {
        val list = assertNotNull(parse("[$validRule]"))
        assertEquals(1, list.size)
        assertEquals("测试快递", list[0].name)
        assertEquals("取件码为", list[0].codePrefix)
        assertEquals("，", list[0].codeSuffix)
        assertEquals("地址:", list[0].addressPrefix)
        assertEquals("【", list[0].brandPrefix)
    }

    @Test
    fun `markdown fenced json imports`() {
        val raw = "```json\n[$validRule]\n```"
        val list = assertNotNull(parse(raw))
        assertEquals(1, list.size)
    }

    @Test
    fun `fenced json without language tag imports`() {
        assertNotNull(parse("```\n[$validRule]\n```"))
    }

    @Test
    fun `json surrounded by ai explanation imports`() {
        val raw = "好的，这是为你生成的规则：\n\n[$validRule]\n\n希望对你有帮助！"
        val list = assertNotNull(parse(raw))
        assertEquals(1, list.size)
    }

    @Test
    fun `single object not array imports`() {
        val list = assertNotNull(parse(validRule))
        assertEquals(1, list.size)
    }

    @Test
    fun `multiple rules import`() {
        val second = validRule.replace("测试快递", "测试快递二")
            .replace("取件码为", "取货码")
        val list = assertNotNull(parse("[$validRule, $second]"))
        assertEquals(2, list.size)
        assertEquals(listOf("测试快递", "测试快递二"), list.map { it.name })
    }

    @Test
    fun `rule without any field is rejected`() {
        val bad = """{"name": "空规则", "fields": {}}"""
        assertNull(parse(bad), "没有任何锚点的规则应该被拒，否则永远匹配不到")
    }

    @Test
    fun `rule without name is rejected`() {
        val bad = """
            {"fields": {"pickupCode": {"prefix": "取件码", "suffix": ""}}}
        """.trimIndent()
        assertNull(parse(bad))
    }

    @Test
    fun `empty anchor strings normalize to null`() {
        val list = assertNotNull(parse(validRule))
        val r = list[0]
        // JSON 里空串应转成 null，否则 extractField 会把空串当「已配置」
        assertNull(r.trackingPrefix, "未提供的字段应保持 null")
        assertNull(r.phonePrefix)
        assertEquals("", r.senderPattern ?: "")
    }

    @Test
    fun `empty input yields nothing`() {
        assertNull(parse(""))
        assertNull(parse("   "))
    }

    @Test
    fun `garbage input yields nothing instead of crashing`() {
        assertNull(parse("这不是 JSON"))
        assertNull(parse("{broken"))
        assertNull(parse("<html></html>"))
    }

    @Test
    fun `round trip export then import keeps anchors`() {
        val original = assertNotNull(parse("[$validRule]"))[0]
        val exported = JsonExporter.exportAll(listOf(original))
        val reimported = assertNotNull(parse(exported))
        assertEquals(1, reimported.size)
        assertEquals(original.name, reimported[0].name)
        assertEquals(original.codePrefix, reimported[0].codePrefix)
        assertEquals(original.codeSuffix, reimported[0].codeSuffix)
        assertEquals(original.addressPrefix, reimported[0].addressPrefix)
        assertEquals(original.brandPrefix, reimported[0].brandPrefix)
    }

    @Test
    fun `imported rule actually extracts from a matching sms`() {
        // 端到端：导入的规则必须能真的从短信里提出东西，否则导入成功也没意义
        val list = assertNotNull(parse("[$validRule]"))
        val sms = "【测试快递】您的包裹已到达，取件码为9-9-9999，地址:测试小区门口"
        val fields = com.quickpick.domain.engine.MatchEngine.findBestMatch(sms, list, null)
        assertNotNull(fields, "导入的规则应能匹配这类短信")
        assertEquals("9-9-9999", fields.second.pickupCode)
        assertEquals("测试小区门口", fields.second.address)
        assertEquals("测试快递", fields.second.brandName)
    }
}
