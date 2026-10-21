package com.quickpick.data.db.entity

import androidx.room.*

@Entity(
    tableName = "package_records",
    foreignKeys = [
        ForeignKey(
            entity = MatchRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.SET_NULL   // 删除规则后历史记录保留，引用置空
        )
    ],
    indices = [Index(value = ["sms_timestamp"]), Index(value = ["rule_id"])]
)
data class PackageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "company_name") val companyName: String,
    @ColumnInfo(name = "tracking_number") val trackingNumber: String? = null,
    @ColumnInfo(name = "pickup_code") val pickupCode: String? = null,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "sms_body") val smsBody: String,
    @ColumnInfo(name = "sms_timestamp") val smsTimestamp: Long,
    @ColumnInfo(name = "is_picked_up") val isPickedUp: Boolean = false,
    @ColumnInfo(name = "rule_id") val ruleId: Long? = null,
    @ColumnInfo(name = "sms_id") val smsId: String? = null,
    @ColumnInfo(name = "sms_thread_id") val smsThreadId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
