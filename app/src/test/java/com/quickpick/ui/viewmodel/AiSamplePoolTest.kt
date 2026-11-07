package com.quickpick.ui.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AI 页 UI 重做（v1.1.11）配套的样本池行为。
 *
 * 改动点：样本列表从「无限铺开」改成「定高容器内滚动」，
 * 因此补齐/移除后的条数必须始终可控，不能无限增长把容器撑爆。
 */
class AiSamplePoolTest {

    private data class Sample(val id: String, val isExpress: Boolean)

    /** 复刻 refill() 的补齐语义：只补到目标数 */
    private fun refill(
        current: List<Sample>,
        expressPool: ArrayDeque<Sample>,
        noticePool: ArrayDeque<Sample>,
        expressTarget: Int,
        noticeTarget: Int
    ): List<Sample> {
        val expressNeed = expressTarget - current.count { it.isExpress }
        val noticeNeed = noticeTarget - current.count { !it.isExpress }
        val picked = mutableListOf<Sample>()
        repeat(expressNeed.coerceAtLeast(0)) { expressPool.removeFirstOrNull()?.let(picked::add) }
        repeat(noticeNeed.coerceAtLeast(0)) { noticePool.removeFirstOrNull()?.let(picked::add) }
        return current + picked
    }

    private fun pool(prefix: String, n: Int, express: Boolean) =
        ArrayDeque((1..n).map { Sample("$prefix$it", express) })

    @Test
    fun `refill tops up to target and never exceeds`() {
        val current = listOf(Sample("e1", true), Sample("e2", true))
        val exprPool = pool("pe", 20, true)
        val noticePool = pool("pn", 10, false)

        val after = refill(current, exprPool, noticePool, expressTarget = 15, noticeTarget = 5)

        assertEquals(20, after.size, "目标 = 15 快递 + 5 通知")
        assertEquals(15, after.count { it.isExpress })
        assertEquals(5, after.count { !it.isExpress })
    }

    @Test
    fun `refill is a no-op when already at target`() {
        val current = (1..15).map { Sample("e$it", true) } + (1..5).map { Sample("n$it", false) }
        val after = refill(
            current, pool("pe", 10, true), pool("pn", 10, false),
            expressTarget = 15, noticeTarget = 5
        )
        assertEquals(current.size, after.size, "已满时不补，避免容器内容无上限增长")
    }

    @Test
    fun `refill respects exhausted pool`() {
        val current = listOf(Sample("e1", true))
        val after = refill(
            current, ArrayDeque(), ArrayDeque(),
            expressTarget = 15, noticeTarget = 5
        )
        assertEquals(1, after.size, "池子空了就补不了，不能凭空造样本")
    }

    @Test
    fun `removing samples lets refill bring them back`() {
        var current = (1..15).map { Sample("e$it", true) } + (1..5).map { Sample("n$it", false) }
        val exprPool = pool("pe", 10, true)

        // 用户删掉 3 条快递（隐私审查）
        current = current.filterNot { it.id in listOf("e1", "e2", "e3") }
        assertEquals(17, current.size)

        val after = refill(current, exprPool, ArrayDeque(), expressTarget = 15, noticeTarget = 5)
        assertEquals(20, after.size, "补齐应回到 20 条")
        assertEquals(15, after.count { it.isExpress })
    }

    @Test
    fun `sample count stays within fixed-height container budget`() {
        // 定高容器 180..360dp，每行约 52dp，最多约 7 行可见；20 条靠内部滚动查看。
        // 这里只做数量上的守卫：默认目标 20 条，不会因为反复补齐越变越多。
        val target = 15 + 5
        var current = emptyList<Sample>()
        val exprPool = pool("pe", 100, true)
        val noticePool = pool("pn", 100, false)

        repeat(10) {
            current = refill(current, exprPool, noticePool, expressTarget = 15, noticeTarget = 5)
        }
        assertTrue(current.size <= target, "反复补齐后条数必须稳定在 $target，实际 ${current.size}")
    }
}
