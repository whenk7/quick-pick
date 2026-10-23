package com.quickpick.domain.model

/**
 * 地址别名（v1.1.7 §6 全局地址簿）：长地址/多种叫法 → 好记的名字。
 * 显示层替换，不动库存数据；改了即时生效无需重扫。
 */
data class AddressAlias(val match: String, val alias: String)

/**
 * 按列表顺序 contains 匹配，首个命中返回 alias。
 * 空白 match 视为无效跳过（contains("") 恒 true，不拦会把所有地址替换掉）。
 */
fun applyAddressAlias(address: String?, aliases: List<AddressAlias>): String? {
    if (address == null) return null
    val hit = aliases.firstOrNull { it.match.isNotBlank() && address.contains(it.match) }
    return hit?.alias ?: address
}
