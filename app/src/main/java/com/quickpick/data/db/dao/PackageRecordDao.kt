package com.quickpick.data.db.dao

import androidx.room.*
import com.quickpick.data.db.entity.PackageRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageRecordDao {
    @Query("SELECT * FROM package_records ORDER BY sms_timestamp DESC")
    fun observeAll(): Flow<List<PackageRecordEntity>>

    @Query("SELECT COUNT(*) FROM package_records WHERE is_picked_up = 0")
    fun observeUnpickedCount(): Flow<Int>

    // 注意必须带 'localtime'：date() 默认 UTC 日界，UTC+8 下会标错天
    @Query("SELECT * FROM package_records WHERE tracking_number = :tracking AND date(sms_timestamp / 1000, 'unixepoch', 'localtime') = date(:dateMs / 1000, 'unixepoch', 'localtime') LIMIT 1")
    suspend fun findByTrackingAndDate(tracking: String, dateMs: Long): PackageRecordEntity?

    /** 按来源短信 id 查重：无单号规则命中的短信全量重扫时也不能重复入库 */
    @Query("SELECT * FROM package_records WHERE sms_id = :smsId LIMIT 1")
    suspend fun findBySmsId(smsId: String): PackageRecordEntity?

    /**
     * 按短信 id 查重，**连同归并历史一起找**（v1.1.11 修复）。
     *
     * 归并/换柜覆盖后，旧短信 id 只存在 merged_sms_ids（JSON）里，sms_id 已被新短信顶替。
     * 全量重扫时那条旧短信会再次被读到：findBySmsId 查不到 → 按「已取件同码 = 新包裹」
     * 又插一条，用户看到重复记录。用 LIKE 粗筛（JSON 里 ID 是带引号的字符串，
     * 用 '"id"' 形式避免 123 命中 1234），再在仓库层做精确校验。
     */
    @Query("SELECT * FROM package_records WHERE sms_id = :smsId OR merged_sms_ids LIKE '%\"' || :smsId || '\"%'")
    suspend fun findByAnySmsId(smsId: String): PackageRecordEntity?

    /** 同码未取件记录（重复/催取提醒归并判定用；码会循环复用，已取件记录不参与归并） */
    @Query("SELECT * FROM package_records WHERE pickup_code = :code AND is_picked_up = 0 ORDER BY sms_timestamp DESC LIMIT 1")
    suspend fun findUnpickedByCode(code: String): PackageRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PackageRecordEntity): Long

    @Update
    suspend fun update(record: PackageRecordEntity)

    /** 标记已取/未取的专用更新：不动 created_at 等其他字段 */
    @Query("UPDATE package_records SET is_picked_up = :picked WHERE id = :id")
    suspend fun updatePickedUp(id: Long, picked: Boolean)

    @Query("UPDATE package_records SET is_picked_up = 1")
    suspend fun markAllPickedUp()

    @Query("UPDATE package_records SET is_picked_up = 1 WHERE date(sms_timestamp / 1000, 'unixepoch', 'localtime') = date(:dateMs / 1000, 'unixepoch', 'localtime')")
    suspend fun markDayPickedUp(dateMs: Long)

    /** 全量重扫时移除失效记录：当前规则不再匹配的未取件记录（已取件的是历史，保留置灰） */
    @Query("DELETE FROM package_records WHERE sms_id = :smsId AND is_picked_up = 0")
    suspend fun deleteUnpickedBySmsId(smsId: String): Int

    /** 删光全部规则后的全量重扫（v1.1.7）：无规则 = 一切不匹配，未取件全清；已取件历史保留 */
    @Query("DELETE FROM package_records WHERE is_picked_up = 0")
    suspend fun deleteAllUnpicked()

    /** 扫描窗口调小（v1.1.8）：移除窗口外的未取件记录；已取件历史保留 */
    @Query("DELETE FROM package_records WHERE is_picked_up = 0 AND sms_timestamp < :cutoffMs")
    suspend fun deleteUnpickedOlderThan(cutoffMs: Long): Int

    /** 用户手动删除单条记录（v1.1.8 原文弹窗删除按钮）；无论是否已取件 */
    @Query("DELETE FROM package_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
