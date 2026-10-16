package com.quickpick.data.db.dao

import androidx.room.*
import com.quickpick.data.db.entity.ScannedSmsEntity

@Dao
interface ScannedSmsDao {
    @Query("SELECT EXISTS(SELECT 1 FROM scanned_sms WHERE sms_id = :smsId)")
    suspend fun exists(smsId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ScannedSmsEntity)

    @Query("SELECT COUNT(*) FROM scanned_sms")
    suspend fun count(): Int

    /** 重扫未匹配短信：清除没有对应快递记录的已扫标记，下次扫描自动重试 */
    @Query("DELETE FROM scanned_sms WHERE sms_id NOT IN (SELECT sms_id FROM package_records WHERE sms_id IS NOT NULL)")
    suspend fun deleteUnmatched()

    @Query("DELETE FROM scanned_sms")
    suspend fun clearAll()
}
