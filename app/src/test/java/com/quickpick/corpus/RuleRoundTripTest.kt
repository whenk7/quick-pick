package com.quickpick.corpus

import com.quickpick.util.JsonExporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 语料规则集的导出→导入往返一致性（也是给用户分发 JSON 前的最后把关） */
class RuleRoundTripTest {

    @Test
    fun `corpus test rules round-trip through JsonExporter losslessly`() {
        val json = JsonExporter.exportAll(TestRules.all)
        println("===RULES-JSON-BEGIN===")
        println(json)
        println("===RULES-JSON-END===")
        val parsed = JsonExporter.import(json).getOrThrow()
        assertEquals(TestRules.all.size, parsed.size)
        // 导入侧约定空串归一为 null（JsonExporter.parseRule），比较前同样归一
        fun norm(s: String?) = s?.takeIf { it.isNotEmpty() }
        TestRules.all.zip(parsed).forEach { (a, b) ->
            assertEquals(a.name, b.name)
            assertEquals(norm(a.codePrefix), b.codePrefix)
            assertEquals(norm(a.codeSuffix), b.codeSuffix)
            assertEquals(norm(a.addressPrefix), b.addressPrefix)
            assertEquals(norm(a.addressSuffix), b.addressSuffix)
            assertEquals(norm(a.trackingPrefix), b.trackingPrefix)
            assertEquals(norm(a.trackingSuffix), b.trackingSuffix)
            assertEquals(norm(a.brandPrefix), b.brandPrefix)
            assertEquals(norm(a.brandSuffix), b.brandSuffix)
            assertEquals(norm(a.senderPattern), b.senderPattern)
        }
    }
}
