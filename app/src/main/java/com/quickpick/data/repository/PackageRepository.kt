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

    /** 全量重扫新语义（v1.1.5 用户裁决）：当前规则匹配不上 → 移除失效的未取件记录 */
    suspend fun deleteUnpickedBySmsId(smsId: String) = dao.deleteUnpickedBySmsId(smsId)

    /** 删光全部规则后的重扫（v1.1.7）：清空全部未取件记录，已取件历史保留 */
    suspend fun deleteAllUnpicked() = dao.deleteAllUnpicked()

    /** 扫描窗口调小（v1.1.8）：移除窗口外未取件；已取件保留。幂等（调大时无窗口外未取件可删） */
    suspend fun deleteUnpickedOlderThan(cutoffMs: Long) = dao.deleteUnpickedOlderThan(cutoffMs)

    /** 用户手动删除单条（无论是否已取件），调用方负责把 smsId 加入忽略名单防重扫恢复 */
    suspend fun deleteById(id: Long) = dao.deleteById(id)

    /**
     * 扫描入库（含去重与换柜覆盖，对应 edge-cases 五）：
     * - 同一条短信（smsId）→ 跳过（顺带回填老记录的 smsThreadId）
     * - 同日同单号且取件码/地址一致 → 重复通知，跳过
     * - 同日同单号但取件码不同 → 同包裹换柜的新通知，覆盖旧记录（保留原 id 和创建时间）
     * - 无单号时同码归并：同取件码的未取件记录 = 同一包裹的重复/催取提醒，
     *   刷新正文/时间/来源且不新增（码会循环复用，已取件记录不参与归并）
     * - 其余 → 插入新记录
     */
    suspend fun insertFromSms(record: PackageRecord) {
        // 同一条短信只入库一次：全量重扫（clearScanned）时的兜底去重，
        // 不依赖单号有无——无单号规则命中的短信此前每次刷新都会重复插入
        val smsId = record.smsId
        if (smsId != null) {
            val existing = dao.findBySmsId(smsId)
            if (existing != null) {
                // 迁移后老记录的新列（threadId v2 / sender v3）为 NULL，重扫命中查重时回填，
                // 否则升级用户的历史记录永远看不到「查看短信」/发件人（spec §九「重扫后补齐」）
                val needThread = existing.smsThreadId == null && record.smsThreadId != null
                val needSender = existing.smsSender == null && record.smsSender != null
                if (needThread || needSender) {
                    dao.update(
                        existing.copy(
                            smsThreadId = record.smsThreadId ?: existing.smsThreadId,
                            smsSender = record.smsSender ?: existing.smsSender
                        )
                    )
                }
                return
            }
        }
        val tracking = record.trackingNumber
        if (!tracking.isNullOrEmpty()) {
            val existing = dao.findByTrackingAndDate(tracking, record.smsTimestamp)
            if (existing != null) {
                if (existing.pickupCode == record.pickupCode && existing.address == record.address) {
                    return  // 完全重复，忽略
                }
                // 全量重扫按时间倒序处理，旧短信后到——不许用旧取件码覆盖新记录
                if (existing.smsTimestamp >= record.smsTimestamp) return
                // 覆盖时把被顶掉的旧短信 id 收进 mergedSmsIds（删除时全忽略，防重扫复活）
                dao.update(
                    record.toEntity().copy(
                        id = existing.id, createdAt = existing.createdAt,
                        mergedSmsIds = encodeMerged(decodeMerged(existing.mergedSmsIds) + listOfNotNull(existing.smsId))
                    )
                )
                return
            }
        }
        // 无单号短信的跨日重复/催取归并（语料实证：E029/E030 菜鸟三日两推、
        // E009/E011 袋鼠标准+超时双模板同包裹）。以码为键、仅归并未取件记录：
        // 已取件后同码再来视为码复用的新包裹（漏归并只会多条记录，漏新包裹是静默丢失）
        val code = record.pickupCode
        if (!code.isNullOrEmpty()) {
            val existing = dao.findUnpickedByCode(code)
            if (existing != null) {
                // 双方地址都提取到且不同 → 不同驿站同码巧合，不当同一包裹
                if (existing.address != null && record.address != null &&
                    existing.address != record.address
                ) {
                    dao.insert(record.toEntity())
                    return
                }
                // 旧短信后到（全量重扫倒序）不降级较新记录
                if (existing.smsTimestamp >= record.smsTimestamp) return
                dao.update(
                    record.toEntity().copy(
                        id = existing.id,
                        createdAt = existing.createdAt,
                        isPickedUp = existing.isPickedUp,
                        // 新短信缺的字段从旧记录保留（催取模板常缺地址/单号）
                        trackingNumber = record.trackingNumber ?: existing.trackingNumber,
                        address = record.address ?: existing.address,
                        phone = record.phone ?: existing.phone,
                        smsThreadId = record.smsThreadId ?: existing.smsThreadId,
                        smsSender = record.smsSender ?: existing.smsSender,
                        // 归并时把被顶掉的旧短信 id 收进 mergedSmsIds（删除时全忽略，防重扫复活）
                        mergedSmsIds = encodeMerged(decodeMerged(existing.mergedSmsIds) + listOfNotNull(existing.smsId))
                    )
                )
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
        isPickedUp = isPickedUp,
        ruleId = ruleId, smsId = smsId, smsThreadId = smsThreadId,
        smsSender = smsSender,
        mergedSmsIds = decodeMerged(mergedSmsIds)
    )

    private fun PackageRecord.toEntity() = PackageRecordEntity(
        id = id, companyName = companyName,
        trackingNumber = trackingNumber, pickupCode = pickupCode,
        address = address, phone = phone,
        smsBody = smsBody, smsTimestamp = smsTimestamp,
        isPickedUp = isPickedUp,
        ruleId = ruleId, smsId = smsId, smsThreadId = smsThreadId,
        smsSender = smsSender,
        mergedSmsIds = encodeMerged(mergedSmsIds)
    )

    companion object {
        private val gson = com.google.gson.Gson()
        private val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type

        fun decodeMerged(json: String): List<String> =
            runCatching { gson.fromJson<List<String>>(json, listType) }.getOrNull() ?: emptyList()

        fun encodeMerged(ids: List<String>): String = gson.toJson(ids.distinct())
    }
}
