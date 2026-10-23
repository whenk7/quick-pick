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
    val smsSender: String? = null,
    /** 归并/换柜时被顶掉的旧短信 id（v1.1.8）：删除记录时全部进忽略名单防重扫复活 */
    val mergedSmsIds: List<String> = emptyList()
)
