package com.quickpick.domain.model

/** 沉浸模式分组（v1.1.7 §11）：按别名后地址分组，无地址归「其他」 */
data class ImmersionGroup(val location: String, val items: List<PackageRecord>)

/**
 * 分组规则：组件数倒序（最多的驿站排最前）；组内时间倒序（新的在前）。
 * 地址已在 VM 层做过别名替换，这里直接用。
 */
fun groupForImmersion(packages: List<PackageRecord>): List<ImmersionGroup> =
    packages
        .groupBy { it.address ?: "其他" }
        .map { (loc, items) -> ImmersionGroup(loc, items.sortedByDescending { it.smsTimestamp }) }
        .sortedWith(compareByDescending<ImmersionGroup> { it.items.size }.thenBy { it.location })
