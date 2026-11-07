package com.quickpick.data.repository

import com.quickpick.data.db.dao.PackageRecordDao
import com.quickpick.data.db.entity.PackageRecordEntity
import com.quickpick.domain.model.PackageRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 入库去重的两个既有缺陷（v1.1.11 修复），真机上可能造成：
 * 1. 已取件的记录被换柜新短信翻回「待取」
 * 2. 归并过的记录在全量重扫时又冒出一条重复的
 */
class PackageRepositoryMergeTest {

    private class FakeDao : PackageRecordDao {
        val store = mutableListOf<PackageRecordEntity>()
        private var nextId = 1L

        companion object {
            val ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Shanghai")
            fun sameDay(a: Long, b: Long): Boolean =
                java.time.Instant.ofEpochMilli(a).atZone(ZONE).toLocalDate() ==
                    java.time.Instant.ofEpochMilli(b).atZone(ZONE).toLocalDate()
        }

        override fun observeAll(): Flow<List<PackageRecordEntity>> = flowOf(store.toList())
        override fun observeUnpickedCount(): Flow<Int> = flowOf(store.count { !it.isPickedUp })

        override suspend fun findByTrackingAndDate(tracking: String, dateMs: Long) =
            store.firstOrNull { it.trackingNumber == tracking && sameDay(it.smsTimestamp, dateMs) }

        override suspend fun findBySmsId(smsId: String) = store.firstOrNull { it.smsId == smsId }

        /** 复刻 SQL：sms_id 相等，或 merged_sms_ids JSON 里含该 id */
        override suspend fun findByAnySmsId(smsId: String) = store.firstOrNull { e ->
            e.smsId == smsId || PackageRepository.decodeMerged(e.mergedSmsIds).contains(smsId)
        }

        override suspend fun findUnpickedByCode(code: String) =
            store.filter { it.pickupCode == code && !it.isPickedUp }.maxByOrNull { it.smsTimestamp }

        override suspend fun insert(record: PackageRecordEntity): Long {
            val id = if (record.id == 0L) nextId++ else record.id
            store.removeAll { it.id == id }
            store.add(record.copy(id = id))
            return id
        }

        override suspend fun update(record: PackageRecordEntity) {
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

    private fun ts(day: Int, hour: Int = 12): Long =
        java.time.ZonedDateTime.of(2026, 9, day, hour, 0, 0, 0, FakeDao.ZONE).toInstant().toEpochMilli()

    private fun rec(
        tracking: String? = null,
        code: String? = "1111",
        address: String? = "测试驿站",
        day: Int = 13,
        hour: Int = 12,
        smsId: String? = null,
        phone: String? = null
    ) = PackageRecord(
        companyName = "测试快递", trackingNumber = tracking, pickupCode = code,
        address = address, phone = phone, smsBody = "sms", smsTimestamp = ts(day, hour), smsId = smsId
    )

    private fun newRepo(): Pair<PackageRepository, FakeDao> {
        val dao = FakeDao()
        return PackageRepository(dao) to dao
    }

    // ============ 缺陷 1：换柜覆盖把已取件翻回待取 ============

    @Test
    fun `locker change keeps picked-up state`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(tracking = "TEST001", code = "1111", day = 13, smsId = "a"))
        // 用户已取件
        dao.updatePickedUp(dao.store[0].id, true)
        assertTrue(dao.store[0].isPickedUp)

        // 同日同单号、新柜新码 → 覆盖
        repo.insertFromSms(rec(tracking = "TEST001", code = "2222", day = 13, hour = 18, smsId = "b"))

        assertEquals(1, dao.store.size)
        assertEquals("2222", dao.store[0].pickupCode, "取件码应更新为新柜的")
        assertTrue(dao.store[0].isPickedUp, "已取件状态必须保留，不能被新短信翻回待取")
    }

    @Test
    fun `locker change keeps address when new sms lacks it`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(tracking = "TEST001", code = "1111", address = "原驿站地址", day = 13, smsId = "a"))
        repo.insertFromSms(rec(tracking = "TEST001", code = "2222", address = null, day = 13, hour = 18, smsId = "b"))

        assertEquals(1, dao.store.size)
        assertEquals("原驿站地址", dao.store[0].address, "新短信没地址时不能把旧地址冲成 null")
    }

    @Test
    fun `locker change keeps phone when new sms lacks it`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(tracking = "TEST001", code = "1111", phone = "10000000000", day = 13, smsId = "a"))
        repo.insertFromSms(rec(tracking = "TEST001", code = "2222", phone = null, day = 13, hour = 18, smsId = "b"))
        assertEquals("10000000000", dao.store[0].phone)
    }

    // ============ 缺陷 2：归并记录在全量重扫时复活 ============

    @Test
    fun `merged sms does not come back on full rescan after picked up`() = runBlocking {
        val (repo, dao) = newRepo()
        // 同码两条：后到的归并掉先到的，先到的 id 进 mergedSmsIds
        repo.insertFromSms(rec(code = "8888", day = 13, smsId = "first"))
        repo.insertFromSms(rec(code = "8888", day = 14, smsId = "second"))
        assertEquals(1, dao.store.size)
        assertEquals("second", dao.store[0].smsId)
        assertEquals(listOf("first"), PackageRepository.decodeMerged(dao.store[0].mergedSmsIds))

        // 用户取件
        dao.updatePickedUp(dao.store[0].id, true)

        // 全量重扫：旧短信 first 又被读到了
        repo.insertFromSms(rec(code = "8888", day = 13, smsId = "first"))

        assertEquals(1, dao.store.size, "归并历史里的短信不能再次入库（否则用户看到重复记录）")
        assertTrue(dao.store[0].isPickedUp, "已有记录不受影响")
    }

    @Test
    fun `merged sms does not come back even when still unpicked`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(code = "8888", day = 13, smsId = "first"))
        repo.insertFromSms(rec(code = "8888", day = 14, smsId = "second"))
        assertEquals(1, dao.store.size)

        // 未取件状态下重扫旧短信，同样不能新增
        repo.insertFromSms(rec(code = "8888", day = 13, smsId = "first"))
        assertEquals(1, dao.store.size)
    }

    @Test
    fun `different sms ids are still inserted normally`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(code = "1111", day = 13, smsId = "a"))
        repo.insertFromSms(rec(code = "2222", day = 13, smsId = "b"))
        assertEquals(2, dao.store.size, "不同短信不同码应各自入库")
    }

    @Test
    fun `sms id that merely contains another id as substring is not confused`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(code = "1111", day = 13, smsId = "12"))
        // "12" 是 "123" 的子串：精确校验必须区分开
        repo.insertFromSms(rec(code = "2222", day = 13, smsId = "123"))
        assertEquals(2, dao.store.size, "子串 id 不能误判为同一条短信")
    }

    // ============ 回归守护：原有语义不能被改坏 ============

    @Test
    fun `same sms id still backfills thread and sender`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(code = "1111", day = 13, smsId = "a"))
        dao.store[0] = dao.store[0].copy(smsThreadId = null, smsSender = null)
        val same = rec(code = "1111", day = 13, smsId = "a")
        repo.insertFromSms(same.copy(smsThreadId = 7L, smsSender = "106"))

        assertEquals(1, dao.store.size)
        assertEquals(7L, dao.store[0].smsThreadId)
        assertEquals("106", dao.store[0].smsSender)
    }

    @Test
    fun `picked record plus same code sms still inserts as new parcel`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(code = "5555", day = 13, smsId = "a"))
        dao.updatePickedUp(dao.store[0].id, true)
        // 不同短信、同码、已取件 → 视为码复用的新包裹（原语义，不能因修复而改变）
        repo.insertFromSms(rec(code = "5555", day = 27, smsId = "b"))
        assertEquals(2, dao.store.size, "已取件后同码的新短信仍是新包裹")
        assertEquals(1, dao.store.count { !it.isPickedUp })
    }

    @Test
    fun `older sms still cannot overwrite newer on locker change`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(tracking = "T1", code = "2222", day = 13, hour = 18, smsId = "b"))
        repo.insertFromSms(rec(tracking = "T1", code = "1111", day = 13, hour = 9, smsId = "a"))
        assertEquals("2222", dao.store[0].pickupCode)
        assertFalse(dao.store[0].smsId == "a")
    }
}
