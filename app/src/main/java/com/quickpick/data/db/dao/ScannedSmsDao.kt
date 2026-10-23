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


    @Query("DELETE FROM scanned_sms")
    suspend fun clearAll()
}
