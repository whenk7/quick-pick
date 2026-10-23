package com.quickpick.domain.model

data class PackageRecord(
    val id: Long = 0,
    val companyName: String,
    val trackingNumber: String? = null,
    val pickupCode: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val smsBody: String,
    val smsTimestamp: Long,
    val isPickedUp: Boolean = false,
    val ruleId: Long? = null,
    val smsId: String? = null,
    val smsThreadId: Long? = null,
    val smsSender: String? = null
)
