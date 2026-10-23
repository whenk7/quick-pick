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
    // 已修（2026-10-23 preset_v3 极简重置）：顺丰/京东预置不再有地址锚点，F1 根除。
    // 期望态转正为常驻回归守护（原「现状复现」测试按设计随修复删除）。

    @Test
    fun `F1 fixed - marketing sms with arrival keyword must not be recorded`() {
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
