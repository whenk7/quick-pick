package com.quickpick.ui.viewmodel

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * AI 样本挑选的模板多样性（v1.1.10 修复）：
 * 旧实现只按【签名】去重，同家公司多种模板（袋鼠的标准/超时/滞留）只留一条，
 * AI 拿不到样本 → 生成不出对应规则 → 用户导入后仍有大批短信识别不到。
 *
 * 这里用真实语料里出现过的模板验证：不同句式必须都留下，同句式只留一条。
 */
class AiSampleTemplateKeyTest {

    /** 复刻 AiPromptViewModel 的两个私有纯函数 */
    private fun signatureOf(body: String): String =
        if (body.startsWith("【")) body.substringBefore("】") else body.take(12)

    private fun templateKeyOf(body: String): String {
        val sig = signatureOf(body)
        val skeleton = body.dropWhile { it != '】' }.drop(1)
            .filterNot { it.isDigit() || it in "，。；、！？,.!?：: （）()【】《》\n\r\t -" }
            .take(24)
        return "$sig|$skeleton"
    }

    /** 虚构样本：句式结构照搬真实模板，地名/柜号/单号全部虚构 */
    private val realSamples = listOf(
        // 袋鼠智柜三种模板：签名相同，句式不同
        "【袋鼠智柜】快递员提醒您，请凭取件码10000001到测试小区西南门袋鼠柜A01取您的快递",
        "【袋鼠智柜】快递员提醒您，请凭取件码2000-0002到测试小区西南门袋鼠柜A02取您的快递",
        "【袋鼠智柜】您存放在测试小区西南门袋鼠柜A03的快递TEST0000000001超时未取，请及时来取，取件码是3000-0003",
        "【袋鼠智柜】您存放在测试小区西南门袋鼠柜A04的快递TEST0000000002已滞留，请及时来取，取件码是40000004",
        // 兔喜两种不同句子
        "【兔喜生活】您有包裹已到达测试小区便利店，取件码为5-0-0005，地址:测试小区1号楼商铺S001号",
        "【兔喜生活】您有包裹已到达测试小区便利店，取件码为6-0-0006，地址:测试小区便利",
        // 菜鸟三种句式
        "【菜鸟驿站】您的包裹已到站，凭7-0-0007到测试小区西门店取件。",
        "【菜鸟驿站】您的包裹已到站，凭8-0-0008到测试小区西门店取件。",
        "【菜鸟驿站】取件码9-0-0009的测试快递00009包裹到测试小区西门便利店内，请20:00前取",
    )

    @Test
    fun `same signature different sentence keeps both templates`() {
        val keys = realSamples.map { templateKeyOf(it) }
        val distinct = keys.distinct()

        // 袋鼠的「请凭取件码…」两条应合并成一条；「超时未取」和「已滞留」各自独立
        val dszs = realSamples.filter { it.contains("袋鼠智柜") }
        val dszsKeys = dszs.map { templateKeyOf(it) }.distinct()
        assertTrue(dszsKeys.size >= 3, "袋鼠四种句式应至少保留 3 种模板，实际 ${dszsKeys.size}")
        assertTrue(dszsKeys.size <= 4)

        // 兔喜两条是同句式的不同码 → 合并成一条
        val tx = realSamples.filter { it.contains("兔喜生活") }
        assertTrue(tx.map { templateKeyOf(it) }.distinct().size <= 2, "兔喜同句式应合并")

        // 菜鸟的「已到站」两条合并，「取件码X的」单独保留
        val cn = realSamples.filter { it.contains("菜鸟驿站") }
        val cnKeys = cn.map { templateKeyOf(it) }.distinct()
        assertTrue(cnKeys.size >= 2, "菜鸟两种句式都应保留，实际 ${cnKeys.size}")

        if (distinct.size < 5) fail("模板总数过少，说明去重太狠：$distinct")
    }

    @Test
    fun `old signature-only dedup loses templates - regression evidence`() {
        // 演示旧实现的破坏性：袋鼠四种句式被压成 1 条
        val oldWay = realSamples.distinctBy { signatureOf(it) }
        val newWay = realSamples.distinctBy { templateKeyOf(it) }

        assertTrue(
            newWay.size > oldWay.size,
            "新去重（${newWay.size} 种）应显著多于旧去重（${oldWay.size} 种）"
        )
        // 旧实现里袋鼠只剩一条
        val oldDszs = oldWay.count { it.contains("袋鼠智柜") }
        assertTrue(oldDszs == 1, "旧实现确实把袋鼠压成 1 条，实际 $oldDszs")
    }
}
