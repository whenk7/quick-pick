package com.quickpick.corpus

import com.quickpick.domain.engine.MatchEngine
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 首红即发现：本类测试演示已确认的真实缺陷/边界。
 * 每个发现两个测试一对：
 * - `Fx currently reproduces`：断言缺陷现状仍复现（绿）。修复后它会变红——提醒你去解除配对的 @Ignore。
 * - `Fx must not ...`：断言修复后期望的行为（@Ignore 中）。修复后解除 Ignore 转为回归守护。
 */
class KnownFindingsTest {

    private val rules = TestRules.allWithPresets

    // ---- F1：预置顺丰 addressPrefix「到达」过松，营销短信可误报入库 ----
    // 修法：预置锚点收紧「已到达」+ preset_v3 升级路径。语料无顺丰/京东实样，
    // 不拿合成对抗样本盲调生产锚点 → v1.1.2 候选。

    @Test
    fun `F1 currently reproduces - marketing sms with arrival keyword is recorded`() {
        val v = KNOWN_FAILURES.first { it.id == "V03" }
        val result = MatchEngine.findBestMatch(v.text, rules)
        assertEquals("顺丰速运", result?.first?.name)
        assertEquals("提醒服务已开通", result?.second?.address)   // 垃圾地址入库
    }

    @Test
    @Ignore("F1 未修：预置顺丰锚点「到达」过松，v1.1.2 收紧为「已到达」+ preset_v3")
    fun `F1 marketing sms with arrival keyword must not be recorded`() {
        val v = KNOWN_FAILURES.first { it.id == "V03" }
        assertNull(MatchEngine.findBestMatch(v.text, rules))
    }

    // ---- F2：退货上门短信含「取货码」，与圆通/妈妈驿站正文锚点不可区分 ----
    // 正解：品牌升格为过滤条件（规则声明「仅当【】签名含 X 才匹配」，E2）→ v1.1.2。

    @Test
    fun `F2 currently reproduces - return pickup sms is recorded as pickup`() {
        val v = KNOWN_FAILURES.first { it.id == "V04" }
        val result = MatchEngine.findBestMatch(v.text, rules)
        assertEquals("8899", result?.second?.pickupCode)   // 退货码被当取件码入库
    }

    @Test
    @Ignore("F2 未修：待品牌过滤升格（E2，v1.1.2）")
    fun `F2 return pickup sms with code wording must not be recorded`() {
        val v = KNOWN_FAILURES.first { it.id == "V04" }
        assertNull(MatchEngine.findBestMatch(v.text, rules))
    }
}
