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
}
