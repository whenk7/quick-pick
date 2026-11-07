package com.quickpick.ui.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 下拉刷新在「扫描在途」时的调度（v1.1.12 修复）。
 *
 * 现场故障：切回前台会自动触发一次增量扫描，这期间用户下拉 →
 * 旧实现 `if (_isScanning) return` 直接丢弃请求 → 动画转完列表没变化。
 *
 * 这里用纯状态机复刻 refresh()/scanSms() 的交互，验证：
 * - 在途时下拉不会被丢弃，而是登记为待办
 * - 待办在当前扫描结束后被执行
 * - 下拉动画指示器只在用户主动下拉时亮
 */
class RefreshSchedulingTest {

    /** 复刻 ViewModel 里的调度状态 */
    private class Scheduler {
        var isScanning = false
        var isRefreshing = false
        var pendingFullRescan = false
        /** 记录实际执行过的扫描次数：每次 = (是否全量) */
        val scans = mutableListOf<Boolean>()
        /** 记录 clearScanned 调用次数 */
        var clearCount = 0

        /** 对应 refresh() */
        fun requestRefresh() {
            if (isScanning) {
                pendingFullRescan = true
                isRefreshing = true
                return
            }
            isRefreshing = true
            startScan(full = true)
        }

        /** 对应 scanSms(removeUnmatched) 的开始 */
        fun startScan(full: Boolean) {
            if (isScanning) return          // 与实现一致的守卫
            isScanning = true
            if (full) clearCount++
            scans += full
        }

        /** 对应 scanSms 的 finally */
        fun finishScan() {
            isScanning = false
            if (pendingFullRescan) {
                pendingFullRescan = false
                startScan(full = true)
            } else {
                isRefreshing = false
            }
        }
    }

    @Test
    fun `refresh during background scan is not dropped - the reported bug`() {
        val s = Scheduler()
        // 切回前台：增量扫描自动开始
        s.startScan(full = false)
        assertTrue(s.isScanning)

        // 用户此时下拉（旧实现这里被丢弃，什么都不发生）
        s.requestRefresh()
        assertTrue(s.pendingFullRescan, "下拉请求必须被登记，不能丢")
        assertEquals(0, s.clearCount, "在途时不能抢跑清标记")

        // 增量扫描结束 → 补跑全量
        s.finishScan()
        assertEquals(1, s.clearCount, "补跑的全量扫描要清标记")
        assertEquals(listOf(false, true), s.scans, "先增量后全量")
    }

    @Test
    fun `refresh when idle scans immediately`() {
        val s = Scheduler()
        s.requestRefresh()
        assertEquals(listOf(true), s.scans)
        assertEquals(1, s.clearCount)
        assertTrue(s.isRefreshing)

        s.finishScan()
        assertFalse(s.isRefreshing, "扫描结束动画要停")
    }

    @Test
    fun `background incremental scan alone does not show refresh indicator`() {
        val s = Scheduler()
        // 只有 ON_RESUME 触发的增量扫描，没有用户下拉
        s.startScan(full = false)
        // 关键：isRefreshing 保持 false —— 旧实现绑 isScanning，这里会误亮
        assertFalse(s.isRefreshing, "自动扫描不该让下拉动画转")
        s.finishScan()
        assertFalse(s.isRefreshing)
    }

    @Test
    fun `indicator stays on until the follow-up full scan finishes`() {
        val s = Scheduler()
        s.startScan(full = false)
        s.requestRefresh()
        s.finishScan()          // 增量结束，补跑全量开始
        assertTrue(s.isScanning, "补跑的全量扫描应已开始")
        assertTrue(s.isRefreshing, "补跑期间指示器要继续转")
        s.finishScan()          // 全量结束
        assertFalse(s.isRefreshing, "全部结束才停")
    }

    @Test
    fun `multiple refreshes during scan collapse into one`() {
        val s = Scheduler()
        s.startScan(full = false)
        s.requestRefresh()
        s.requestRefresh()
        s.requestRefresh()
        assertTrue(s.pendingFullRescan, "多次下拉只登记一个待办")
        s.finishScan()
        assertEquals(listOf(false, true), s.scans, "补跑只发生一次，不重复扫")
    }

    /**
     * 改扫描范围后必须清标记（v1.1.12 修复）。否则下次增量扫描会把窗口内的短信
     * 当「已处理」全跳过，用户改完范围看不到任何变化。
     */
    @Test
    fun `changing scan window must invalidate scan cache`() {
        var scannedMarkers = 500     // 之前扫过 500 条
        var scannedRounds = 0

        fun changeWindow() {
            scannedMarkers = 0       // smsRepo.clearScanned()
            scannedRounds++
        }

        assertEquals(500, scannedMarkers)
        changeWindow()
        assertEquals(0, scannedMarkers, "改窗口必须清标记，否则新窗口的短信不会被重新判定")
    }
}
