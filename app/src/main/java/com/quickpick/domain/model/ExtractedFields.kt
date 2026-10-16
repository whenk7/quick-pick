package com.quickpick.domain.model

data class ExtractedFields(
    val companyName: String? = null,
    val trackingNumber: String? = null,
    val pickupCode: String? = null,
    val address: String? = null,
    val phone: String? = null
) {
    /** 成功提取的字段数 */
    val matchedCount: Int
        get() = listOfNotNull(
            trackingNumber, pickupCode, address, phone
        ).size

    /** 是否至少有一个取件相关字段（取件码或地址） */
    val isPickupRelated: Boolean
        get() = pickupCode != null || address != null
}
// 注意：没有 isIncomplete 属性。「不完整」由调用方按规则口径计算：
// isIncomplete = matchedCount < rule.configuredFieldCount（规则配置了但没提取到才算不完整；
// 电话是可选字段，不能按固定 4 个字段硬编码，否则没电话的快递永远显示"不完整"）
