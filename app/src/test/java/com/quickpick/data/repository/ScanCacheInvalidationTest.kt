package com.quickpick.data.repository

import com.quickpick.corpus.RAW_SMS
import com.quickpick.corpus.TestRules
import com.quickpick.data.db.entity.ScannedSmsEntity
import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.model.PackageRecord
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 回归守护（v1.1.10）：规则导入/修改后，已标记「已扫描」的短信必须能被重新判定。
 *
 * 现场故障：用户装 App 时只有 2 条预置规则，扫完一遍后所有短信进 scanned_sms。
 * 后来导入 14 条自定义规则，增量扫描把所有短信当「已处理」直接跳过 →
 * 首页一直是「暂未发现快递短信」，逼得只能卸载重装。
 */
class ScanCacheInvalidationTest {

    /** 模拟 scanned_sms 表：只记 id，够验证「跳过/重扫」语义 */
    private class FakeScannedDao : com.quickpick.data.db.dao.ScannedSmsDao {
        val ids = mutableSetOf<String>()

        override suspend fun exists(smsId: String) = smsId in ids
        override suspend fun insert(entity: ScannedSmsEntity) { ids += entity.smsId }
        override suspend fun count() = ids.size
        override suspend fun clearAll() { ids.clear() }
    }

    private class FakePackageDao : com.quickpick.data.db.dao.PackageRecordDao {
        val store = mutableListOf<com.quickpick.data.db.entity.PackageRecordEntity>()
        private var nextId = 1L
        override fun observeAll() = kotlinx.coroutines.flow.flowOf(store.toList())
        override fun observeUnpickedCount() = kotlinx.coroutines.flow.flowOf(store.count { !it.isPickedUp })
        override suspend fun findByTrackingAndDate(tracking: String, dateMs: Long) = null
        override suspend fun findBySmsId(smsId: String) = store.firstOrNull { it.smsId == smsId }
        override suspend fun findUnpickedByCode(code: String) =
            store.filter { it.pickupCode == code && !it.isPickedUp }.maxByOrNull { it.smsTimestamp }
        override suspend fun insert(record: com.quickpick.data.db.entity.PackageRecordEntity): Long {
            val id = if (record.id == 0L) nextId++ else record.id
            store.removeAll { it.id == id }
            store.add(record.copy(id = id))
            return id
        }
        override suspend fun update(record: com.quickpick.data.db.entity.PackageRecordEntity) {
            store.replaceAll { if (it.id == record.id) record else it }
        }
        override suspend fun updatePickedUp(id: Long, picked: Boolean) {
            store.replaceAll { if (it.id == id) it.copy(isPickedUp = picked) else it }
        }
        override suspend fun markAllPickedUp() { store.replaceAll { it.copy(isPickedUp = true) } }
        override suspend fun markDayPickedUp(dateMs: Long) {}
        override suspend fun deleteUnpickedBySmsId(smsId: String): Int = 0
        override suspend fun deleteAllUnpicked() { store.removeAll { !it.isPickedUp } }
        override suspend fun deleteUnpickedOlderThan(cutoffMs: Long): Int = 0
        override suspend fun deleteById(id: Long) { store.removeAll { it.id == id } }
    }

    /**
     * 复刻 PackageListViewModel.scanSms 的增量扫描循环（只保留 skip/入库判定）。
     * 返回本轮新入库条数。
     */
    private suspend fun scan(
        scanned: FakeScannedDao,
        packages: FakePackageDao,
        rules: List<com.quickpick.domain.model.MatchRule>
    ): Int {
        val pkgRepo = PackageRepository(packages)
        val smsRepo = SmsRepository(scanned)
        var inserted = 0
        RAW_SMS.forEach { sms ->
            if (smsRepo.isScanned(sms.id)) return@forEach
            val result = MatchEngine.findBestMatch(sms.text, rules, null)
            if (result != null) {
                val (rule, f) = result
                val before = packages.store.size
                pkgRepo.insertFromSms(
                    PackageRecord(
                        companyName = f.brandName ?: "快递",
                        trackingNumber = f.trackingNumber, pickupCode = f.pickupCode,
                        address = f.address, phone = f.phone,
                        smsBody = sms.text, smsTimestamp = 1_700_000_000_000L + inserted,
                        ruleId = rule.id, smsId = sms.id, smsThreadId = 1L, smsSender = "106"
                    )
                )
                if (packages.store.size > before) inserted++
            }
            smsRepo.markScanned(sms.id)
        }
        return inserted
    }

    @Test
    fun `without cache invalidation imported rules never take effect - the reported bug`() = runBlocking {
        val scanned = FakeScannedDao()
        val packages = FakePackageDao()

        // 第一次扫描：只有 2 条预置规则。带「取货码」的几条会被京东规则蒙对，其余全漏。
        // 但所有短信都会被标为「已扫」——这正是现场故障的起点。
        val presets = TestRules.presets
        val firstRound = scan(scanned, packages, presets)
        assertEquals(RAW_SMS.size, scanned.ids.size, "所有短信都被标为已扫描")
        val afterFirst = packages.store.size
        assertTrue(firstRound <= 4, "预置规则最多蒙中 4 条，实际 $firstRound")

        // 导入用户规则，但**不清标记**（修复前的行为）→ 增量扫描把剩下的全跳过
        val userRules = TestRules.all
        assertEquals(0, scan(scanned, packages, userRules), "不清标记时新规则完全失效（现场故障）")
        assertEquals(afterFirst, packages.store.size, "记录数一点没变，用户看到的就是空/残缺列表")
    }

    @Test
    fun `clearing scanned cache makes imported rules take effect`() = runBlocking {
        val scanned = FakeScannedDao()
        val packages = FakePackageDao()

        scan(scanned, packages, TestRules.presets)

        // 修复后的行为：导入结束时 clearScanned() → 下一轮重新判定
        scanned.clearAll()
        scan(scanned, packages, TestRules.all)

        // 32 条快递语料：E031 是负例（不该提取），同码的会归并成一条，
        // 所以入库数少于 31 是正常语义，关键是从「预置蒙中的几条」跳到「几乎全部」。
        assertTrue(
            packages.store.size >= 25,
            "清标记后应扫出绝大部分快递短信，实际 ${packages.store.size}"
        )
        assertTrue(
            packages.store.count { it.pickupCode != null } >= 27,
            "绝大多数记录应带取件码"
        )
    }

    @Test
    fun `deleting a rule also invalidates the cache`() = runBlocking {
        val scanned = FakeScannedDao()
        RAW_SMS.take(5).forEach { scanned.insert(ScannedSmsEntity(smsId = it.id)) }
        assertEquals(5, scanned.count())

        scanned.clearAll()   // deleteRule 现在会调这个
        assertEquals(0, scanned.count())
    }
}
