package com.quickpick.domain.engine

import com.quickpick.domain.model.ExtractedFields
import com.quickpick.domain.model.MatchRule

object MatchEngine {

    /**
     * 用前缀/后缀锚点从短信中提取字段内容。
     * 找到 prefix → 取其后内容 → 直到 suffix 截止。
     * 不使用正则，纯 indexOf 定位。
     */
    fun extract(smsBody: String, rule: MatchRule): ExtractedFields {
        return ExtractedFields(
            companyName = rule.name,
            trackingNumber = extractField(smsBody, rule.trackingPrefix, rule.trackingSuffix),
            pickupCode = extractField(smsBody, rule.codePrefix, rule.codeSuffix),
            address = extractField(smsBody, rule.addressPrefix, rule.addressSuffix),
            phone = extractField(smsBody, rule.phonePrefix, rule.phoneSuffix)
        )
    }

    private fun extractField(text: String, prefix: String?, suffix: String?): String? {
        if (prefix.isNullOrEmpty()) return null

        val startIndex = text.indexOf(prefix)
        if (startIndex == -1) return null

        val contentStart = startIndex + prefix.length
        if (contentStart >= text.length) return null

        val remaining = text.substring(contentStart)

        val raw = if (suffix.isNullOrEmpty()) {
            // 无后缀：取到文本末尾
            remaining
        } else {
            val endIndex = remaining.indexOf(suffix)
            // 后缀没找到：取到文本末尾
            if (endIndex == -1) remaining else remaining.substring(0, endIndex)
        }
        // 空串视为未匹配（前缀后紧跟后缀 / 只有空白），否则会污染 matchedCount 优先级比较
        return raw.trim().ifEmpty { null }
    }

    /**
     * 对一条短信跑所有规则，返回匹配字段数最多的结果。
     * 字段数相同时取规则列表顺序靠前的。
     */
    fun findBestMatch(smsBody: String, rules: List<MatchRule>): Pair<MatchRule, ExtractedFields>? {
        var best: Pair<MatchRule, ExtractedFields>? = null

        for (rule in rules) {
            if (!rule.hasAnyField) continue

            val fields = extract(smsBody, rule)
            if (!fields.isPickupRelated) continue

            val current = best
            if (current == null || fields.matchedCount > current.second.matchedCount) {
                best = rule to fields
            }
        }

        return best
    }
}
