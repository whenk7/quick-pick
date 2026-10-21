package com.quickpick.domain.model

data class MatchRule(
    val id: Long = 0,
    val name: String,
    val isPreset: Boolean = false,
    val trackingPrefix: String? = null,
    val trackingSuffix: String? = null,
    val codePrefix: String? = null,
    val codeSuffix: String? = null,
    val addressPrefix: String? = null,
    val addressSuffix: String? = null,
    val phonePrefix: String? = null,
    val phoneSuffix: String? = null,
    val senderPattern: String? = null,
    val brandPrefix: String? = null,
    val brandSuffix: String? = null,
    val sampleSms: String? = null
) {
    /** 有配置锚点的字段数（senderPattern/brandPrefix/brandSuffix 是过滤器/附加提取器，不计入） */
    val configuredFieldCount: Int
        get() = listOf(
            trackingPrefix != null || trackingSuffix != null,
            codePrefix != null || codeSuffix != null,
            addressPrefix != null || addressSuffix != null,
            phonePrefix != null || phoneSuffix != null
        ).count { it }

    /** 是否有任何字段配置了锚点 */
    val hasAnyField: Boolean
        get() = configuredFieldCount > 0
}
