package com.quickpick.data.db.entity

import androidx.room.*

@Entity(
    tableName = "scanned_sms",
    indices = [Index(value = ["sms_id"], unique = true)]
)
data class ScannedSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sms_id") val smsId: String,
    @ColumnInfo(name = "scanned_at") val scannedAt: Long = System.currentTimeMillis()
)
