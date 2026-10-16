package com.quickpick.data.db.entity

import androidx.room.*

@Entity(
    tableName = "match_rules",
    indices = [Index(value = ["name"])]   // 按名查找（导入同名冲突检测）
)
data class MatchRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "is_preset") val isPreset: Boolean = false,
    @ColumnInfo(name = "tracking_prefix") val trackingPrefix: String? = null,
    @ColumnInfo(name = "tracking_suffix") val trackingSuffix: String? = null,
    @ColumnInfo(name = "code_prefix") val codePrefix: String? = null,
    @ColumnInfo(name = "code_suffix") val codeSuffix: String? = null,
    @ColumnInfo(name = "address_prefix") val addressPrefix: String? = null,
    @ColumnInfo(name = "address_suffix") val addressSuffix: String? = null,
    @ColumnInfo(name = "phone_prefix") val phonePrefix: String? = null,
    @ColumnInfo(name = "phone_suffix") val phoneSuffix: String? = null,
    @ColumnInfo(name = "sample_sms") val sampleSms: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
