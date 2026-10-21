package com.quickpick.domain.model

data class ExtractedFields(
    val companyName: String? = null,
    val trackingNumber: String? = null,
    val pickupCode: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val brandName: String? = null
) {
    /** 成功提取的字段数（brandName 是附加信息，不计入） */
    val matchedCount: Int
        get() = listOfNotNull(
            trackingNumber, pickupCode, address, phone
        ).size

    /** 是否至少有一个取件相关字段（取件码或地址） */
    val isPickupRelated: Boolean
        get() = pickupCode != null || address != null
}
