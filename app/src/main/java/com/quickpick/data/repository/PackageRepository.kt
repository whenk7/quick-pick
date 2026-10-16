package com.quickpick.data.repository

import com.quickpick.data.db.dao.PackageRecordDao
import com.quickpick.data.db.entity.PackageRecordEntity
import com.quickpick.domain.model.PackageRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PackageRepository(private val dao: PackageRecordDao) {

    fun observeAll(): Flow<List<PackageRecord>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeUnpickedCount(): Flow<Int> = dao.observeUnpickedCount()

    /** 标记已取/未取：专用 UPDATE，不动 created_at 等其他字段 */
    suspend fun setPickedUp(id: Long, picked: Boolean) = dao.updatePickedUp(id, picked)

    suspend fun markAllPickedUp() = dao.markAllPickedUp()

    suspend fun markDayPickedUp(dateMs: Long) = dao.markDayPickedUp(dateMs)

    /**
     * 扫描入库（含去重与换柜覆盖，对应 edge-cases 五）：
     * - 同日同单号且取件码/地址一致 → 重复通知，跳过
     * - 同日同单号但取件码不同 → 同包裹换柜的新通知，覆盖旧记录（保留原 id 和创建时间）
     * - 其余 → 插入新记录
     */
    suspend fun insertFromSms(record: PackageRecord) {
        val tracking = record.trackingNumber
        if (!tracking.isNullOrEmpty()) {
            val existing = dao.findByTrackingAndDate(tracking, record.smsTimestamp)
            if (existing != null) {
                if (existing.pickupCode == record.pickupCode && existing.address == record.address) {
                    return  // 完全重复，忽略
                }
                // 全量重扫按时间倒序处理，旧短信后到——不许用旧取件码覆盖新记录
                if (existing.smsTimestamp >= record.smsTimestamp) return
                dao.update(record.toEntity().copy(id = existing.id, createdAt = existing.createdAt))
                return
            }
        }
        dao.insert(record.toEntity())
    }

    private fun PackageRecordEntity.toDomain() = PackageRecord(
        id = id, companyName = companyName,
        trackingNumber = trackingNumber, pickupCode = pickupCode,
        address = address, phone = phone,
        smsBody = smsBody, smsTimestamp = smsTimestamp,
        isPickedUp = isPickedUp, isIncomplete = isIncomplete,
        ruleId = ruleId, smsId = smsId
    )

    private fun PackageRecord.toEntity() = PackageRecordEntity(
        id = id, companyName = companyName,
        trackingNumber = trackingNumber, pickupCode = pickupCode,
        address = address, phone = phone,
        smsBody = smsBody, smsTimestamp = smsTimestamp,
        isPickedUp = isPickedUp, isIncomplete = isIncomplete,
        ruleId = ruleId, smsId = smsId
    )
}
