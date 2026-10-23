package com.quickpick.data.repository

import com.quickpick.corpus.RAW_SMS
import com.quickpick.corpus.TestRules
import com.quickpick.data.db.dao.PackageRecordDao
import com.quickpick.data.db.entity.PackageRecordEntity
import com.quickpick.domain.engine.MatchEngine
import com.quickpick.domain.model.PackageRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 入库去重/归并语义。FakeDao 内存实现（接口自带，不用 Robolectric）。
 * 日界判定用固定 +08:00，与生产 SQL 的 localtime 语义一致且 CI 时区无关。
 */
class PackageRepositoryTest {

    private class FakeDao : PackageRecordDao {
        val store = mutableListOf<PackageRecordEntity>()
        private var nextId = 1L

        companion object {
            val ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Shanghai")
            fun sameDay(a: Long, b: Long): Boolean =
                java.time.Instant.ofEpochMilli(a).atZone(ZONE).toLocalDate() ==
                    java.time.Instant.ofEpochMilli(b).atZone(ZONE).toLocalDate()
        }

        override fun observeAll(): Flow<List<PackageRecordEntity>> =
            flowOf(store.sortedByDescending { it.smsTimestamp })

        override fun observeUnpickedCount(): Flow<Int> = flowOf(store.count { !it.isPickedUp })

        override suspend fun findByTrackingAndDate(tracking: String, dateMs: Long) =
            store.firstOrNull { it.trackingNumber == tracking && sameDay(it.smsTimestamp, dateMs) }

        override suspend fun findBySmsId(smsId: String) = store.firstOrNull { it.smsId == smsId }

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

        override suspend fun markAllPickedUp() {
            store.replaceAll { it.copy(isPickedUp = true) }
        }

        override suspend fun markDayPickedUp(dateMs: Long) {
            store.replaceAll { if (sameDay(it.smsTimestamp, dateMs)) it.copy(isPickedUp = true) else it }
        }

        override suspend fun deleteUnpickedBySmsId(smsId: String): Int {
            val before = store.size
            store.removeAll { it.smsId == smsId && !it.isPickedUp }
            return before - store.size
        }

        override suspend fun deleteAllUnpicked() {
            store.removeAll { !it.isPickedUp }
        }

        override suspend fun deleteUnpickedOlderThan(cutoffMs: Long): Int {
            val before = store.size
            store.removeAll { !it.isPickedUp && it.smsTimestamp < cutoffMs }
            return before - store.size
        }

        override suspend fun deleteById(id: Long) {
            store.removeAll { it.id == id }
        }
    }

    private fun ts(day: Int, hour: Int = 12): Long =
        java.time.ZonedDateTime.of(2026, 9, day, hour, 0, 0, 0, FakeDao.ZONE).toInstant().toEpochMilli()

    private fun rec(
        company: String = "袋鼠智柜",
        tracking: String? = null,
        code: String? = "2877-2837",
        address: String? = "绿洲家园西南门袋鼠柜G59",
        body: String = "sms",
        day: Int = 13,
        hour: Int = 12,
        smsId: String? = null,
        threadId: Long? = null,
        sender: String? = null
    ) = PackageRecord(
        companyName = company, trackingNumber = tracking, pickupCode = code,
        address = address, smsBody = body, smsTimestamp = ts(day, hour),
        smsId = smsId, smsThreadId = threadId, smsSender = sender
    )

    private fun newRepo(): Pair<PackageRepository, FakeDao> {
        val dao = FakeDao()
        return PackageRepository(dao) to dao
    }

    // ---- 既有行为钉死（修复后必须保持绿）----

    @Test
    fun `same smsId is skipped on rescan`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "s1"))
        repo.insertFromSms(rec(day = 13, smsId = "s1"))
        assertEquals(1, dao.store.size)
    }

    @Test
    fun `same smsId backfills smsThreadId on rescan`() = runBlocking {
        // v1.1.1 修复的回归守护：老记录 threadId 为 NULL，重扫回填
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "s1", threadId = null))
        repo.insertFromSms(rec(day = 13, smsId = "s1", threadId = 555L))
        assertEquals(1, dao.store.size)
        assertEquals(555L, dao.store[0].smsThreadId)
    }

    @Test
    fun `same smsId backfills smsSender on rescan`() = runBlocking {
        // v3 老记录 sender 为 NULL，重扫回填（短信原文弹窗的发件人）
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "s1", threadId = null))
        dao.store[0] = dao.store[0].copy(smsSender = null)   // 模拟 v2 老记录
        repo.insertFromSms(rec(day = 13, smsId = "s1", threadId = 555L, sender = "10684110405676900"))
        assertEquals(1, dao.store.size)
        assertEquals("10684110405676900", dao.store[0].smsSender)
    }

    @Test
    fun `same day same tracking same code and address is skipped`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(tracking = "YT1", day = 13, smsId = "a"))
        repo.insertFromSms(rec(tracking = "YT1", day = 13, smsId = "b"))
        assertEquals(1, dao.store.size)
    }

    @Test
    fun `same day same tracking different code overwrites keeping id`() = runBlocking {
        // 换柜：同包裹新柜新码 → 覆盖旧记录
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(tracking = "YT1", code = "1111", day = 13, smsId = "a"))
        repo.insertFromSms(rec(tracking = "YT1", code = "2222", day = 13, hour = 18, smsId = "b"))
        assertEquals(1, dao.store.size)
        assertEquals("2222", dao.store[0].pickupCode)
        assertEquals("b", dao.store[0].smsId)
    }

    @Test
    fun `older sms does not overwrite newer on locker change`() = runBlocking {
        // 全量重扫按时间倒序：旧短信后到，不许回滚
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(tracking = "YT1", code = "2222", day = 13, hour = 18, smsId = "b"))
        repo.insertFromSms(rec(tracking = "YT1", code = "1111", day = 13, hour = 9, smsId = "a"))
        assertEquals("2222", dao.store[0].pickupCode)
    }

    @Test
    fun `record without code and tracking inserts directly`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(code = null, day = 13, smsId = "a"))
        repo.insertFromSms(rec(code = null, day = 14, smsId = "b"))
        assertEquals(2, dao.store.size, "无码无单号无法查重，各自入库")
    }

    // ---- 新行为：无单号重复/催取归并（修前 RED）----

    @Test
    fun `cross day reminder with same code merges without new record`() = runBlocking {
        // 用户场景：13 日入柜，20 日超时催取 → 应只有一条记录，内容刷新为最新
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, body = "入柜", smsId = "a"))
        repo.insertFromSms(rec(day = 20, body = "超时催取", smsId = "b"))
        assertEquals(1, dao.store.size, "同包裹催取不应新增记录")
        assertEquals("超时催取", dao.store[0].smsBody, "正文应刷新为最新短信")
        assertEquals(ts(20), dao.store[0].smsTimestamp)
        assertEquals("b", dao.store[0].smsId)
    }

    @Test
    fun `merge gains tracking number from newer richer sms`() = runBlocking {
        // E011(标准,无单号) → E009(超时,带 YT 单号)：合并后记录应补上单号
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 18, smsId = "a"))
        repo.insertFromSms(rec(day = 20, tracking = "YT0073828884344", smsId = "b"))
        assertEquals(1, dao.store.size)
        assertEquals("YT0073828884344", dao.store[0].trackingNumber)
    }

    @Test
    fun `older reminder arriving later does not overwrite newer record`() = runBlocking {
        // 全量重扫倒序：新记录已在库，旧催取后到 → 不降级
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 20, body = "超时带单号", tracking = "YT1", smsId = "b"))
        repo.insertFromSms(rec(day = 13, body = "入柜无单号", smsId = "a"))
        assertEquals(1, dao.store.size)
        assertEquals("超时带单号", dao.store[0].smsBody)
        assertEquals("YT1", dao.store[0].trackingNumber, "旧短信后到不许把单号回滚成 null")
        assertEquals(ts(20), dao.store[0].smsTimestamp)
    }

    @Test
    fun `picked up record plus same code sms inserts as new parcel`() = runBlocking {
        // 码会循环复用：已取件的同码再来 = 新包裹，不许合掉
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "a"))
        dao.updatePickedUp(dao.store[0].id, true)
        repo.insertFromSms(rec(day = 27, smsId = "b"))
        assertEquals(2, dao.store.size, "已取后同码再来应视为新包裹")
        assertEquals(1, dao.store.count { !it.isPickedUp })
    }

    @Test
    fun `same address different codes insert as separate parcels`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(code = "1111", day = 13, smsId = "a"))
        repo.insertFromSms(rec(code = "2222", day = 13, smsId = "b"))
        assertEquals(2, dao.store.size, "同柜两个包裹不许合并")
    }

    @Test
    fun `same code different address inserts separately`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(address = "绿洲家园袋鼠柜G59", day = 13, smsId = "a"))
        repo.insertFromSms(rec(address = "科创中心丰巢柜A01", day = 13, smsId = "b"))
        assertEquals(2, dao.store.size, "不同驿站同码巧合不许合并")
    }

    @Test
    fun `merge keeps old address when newer sms lacks one`() = runBlocking {
        // 催取模板常缺地址（兔喜 V01 实证）→ 合并时保留旧地址
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "a"))
        repo.insertFromSms(rec(day = 20, address = null, body = "催取无地址", smsId = "b"))
        assertEquals(1, dao.store.size)
        assertEquals("绿洲家园西南门袋鼠柜G59", dao.store[0].address)
        assertEquals("催取无地址", dao.store[0].smsBody)
    }

    // ---- 语料实证（引擎 + 仓库联测）----

    private fun scan(text: String, day: Int, smsId: String): PackageRecord {
        val (rule, f) = MatchEngine.findBestMatch(text, TestRules.all)!!
        return PackageRecord(
            companyName = rule.name,
            trackingNumber = f.trackingNumber, pickupCode = f.pickupCode,
            address = f.address, phone = f.phone,
            smsBody = text, smsTimestamp = ts(day), smsId = smsId
        )
    }

    private fun corpus(id: String) = RAW_SMS.first { it.id == id }.text

    @Test
    fun `corpus E009 plus E011 same parcel single record with tracking`() = runBlocking {
        // 9-18 标准提醒(无单号) + 9-20 超时提醒(带单号)：同码同柜同包裹
        val (repo, dao) = newRepo()
        repo.insertFromSms(scan(corpus("E011"), 18, "sms-e011"))
        repo.insertFromSms(scan(corpus("E009"), 20, "sms-e009"))
        assertEquals(1, dao.store.size, "同包裹标准+超时双通知应归并为一条")
        assertEquals("2877-2837", dao.store[0].pickupCode)
        assertEquals("YT0073828884344", dao.store[0].trackingNumber, "归并后应补上超时模板带出的单号")
        assertEquals("sms-e009", dao.store[0].smsId)
    }

    @Test
    fun `corpus E029 E030 duplicate reminder single record`() = runBlocking {
        // 菜鸟同码同文三日两推
        val (repo, dao) = newRepo()
        repo.insertFromSms(scan(corpus("E029"), 23, "sms-e029"))
        repo.insertFromSms(scan(corpus("E030"), 25, "sms-e030"))
        assertEquals(1, dao.store.size, "菜鸟同码重复推送应归并")
        assertEquals(ts(25), dao.store[0].smsTimestamp)
    }

    // ---- 全量重扫移除失效记录（v1.1.5 用户裁决：规则不匹配即移除，不保留无效取件信息）----

    @Test
    fun `unmatched unpicked record is removed on refresh`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "gone-rule"))
        repo.deleteUnpickedBySmsId("gone-rule")
        assertEquals(0, dao.store.size, "规则不再匹配的未取件记录应被移除")
    }

    @Test
    fun `unmatched picked record survives refresh`() = runBlocking {
        // 已取件的是历史记录，不是「待办信息」——保留置灰
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "gone-rule"))
        dao.updatePickedUp(dao.store[0].id, true)
        repo.deleteUnpickedBySmsId("gone-rule")
        assertEquals(1, dao.store.size, "已取件历史不随规则失效移除")
        assertTrue(dao.store[0].isPickedUp)
    }

    @Test
    fun `unrelated records untouched by removal`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "keep", code = "1111"))
        repo.insertFromSms(rec(day = 14, smsId = "gone", code = "2222"))
        repo.deleteUnpickedBySmsId("gone")
        assertEquals(1, dao.store.size)
        assertEquals("1111", dao.store[0].pickupCode)
    }

    // ---- 删光规则后的全量重扫（v1.1.7 §1.1：无规则 = 一切不匹配，未取件清空）----

    @Test
    fun `deleteAllUnpicked removes only unpicked records`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "a", code = "1111"))
        repo.insertFromSms(rec(day = 14, smsId = "b", code = "2222"))
        repo.insertFromSms(rec(day = 15, smsId = "c", code = "3333"))
        dao.updatePickedUp(dao.store.first { it.smsId == "b" }.id, true)
        repo.deleteAllUnpicked()
        assertEquals(1, dao.store.size, "未取件全删，已取件历史保留")
        assertTrue(dao.store[0].isPickedUp)
        assertEquals("2222", dao.store[0].pickupCode)
    }

    @Test
    fun `deleteAllUnpicked on empty store is no-op`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.deleteAllUnpicked()
        assertEquals(0, dao.store.size)
    }

    // ---- 扫描窗口变更清理（v1.1.8：调小窗口即移除窗口外未取件；已取件保留）----

    @Test
    fun `shrink window removes unpicked outside window, keeps picked`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "old-unpicked", code = "1111"))
        repo.insertFromSms(rec(day = 14, smsId = "old-picked", code = "2222"))
        repo.insertFromSms(rec(day = 25, smsId = "new", code = "3333"))
        dao.updatePickedUp(dao.store.first { it.smsId == "old-picked" }.id, true)
        // 窗口调到只覆盖 20 号之后
        repo.deleteUnpickedOlderThan(ts(20))
        assertEquals(listOf("new", "old-picked"), dao.store.map { it.smsId ?: "" }.sorted())
        assertTrue(dao.store.first { it.smsId == "old-picked" }.isPickedUp, "已取件历史保留")
    }

    @Test
    fun `deleteById removes record regardless of picked state`() = runBlocking {
        val (repo, dao) = newRepo()
        repo.insertFromSms(rec(day = 13, smsId = "a", code = "1111"))
        val id = dao.store[0].id
        dao.updatePickedUp(id, true)
        repo.deleteById(id)
        assertEquals(0, dao.store.size)
    }
}
