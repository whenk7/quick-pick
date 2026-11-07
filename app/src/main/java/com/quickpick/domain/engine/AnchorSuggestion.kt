package com.quickpick.domain.engine

/**
 * 从「用户点中的词块」推导字段值 + 前后缀锚点。
 *
 * 背景（2026-11-08 修复）：编辑器分词只按标点/空白切，取件码常与正文粘成一块——
 * 例如「请凭取件码01259354到绿洲家园西南门袋鼠柜B09取您的快递」。用户只能点这一整块，
 * 旧实现拿整块当锚点基准，生成规则的提取结果是整段话（实测 31 条语料 0 条正确）。
 *
 * 新做法：
 * 1. 在词块内部识别出「值」——取件码/单号按数字串形态，地址按尾部地名串；
 * 2. 以值为界，prefix = 值左侧文本，suffix = 值右侧的分隔符；
 * 3. **强制自检**：算出的 prefix/suffix 必须能在正文里还原出该值，否则逐步加长前缀重试，
 *    仍不成立就返回 null（宁可让用户手动填，也不生成一条坏规则）。
 *
 * 纯 Kotlin、无 Android 依赖，便于单测。
 */

/** 编辑器可配置的锚点字段（domain 侧定义，避免依赖 UI 层 ActiveField） */
enum class AnchorField { PICKUP_CODE, ADDRESS, TRACKING, PHONE, BRAND, SENDER }

/** 锚点推导结果；[usable] 为 false 时调用方应保留用户原输入或提示手动填写 */
data class AnchorSuggestion(
    val value: String,
    val prefix: String,
    val suffix: String
)

/** 会中断字段值的中文/全角标点与空白：字段值在此截止 */
private const val STOP_CHARS = "，。；、！？：,;!?（）()【】《》「」\n\r\t 　"

/** 取件码允许字符：数字、常见分隔符、ASCII 字母（部分驿站码带字母，中文不算） */
private fun isCodeChar(c: Char): Boolean =
    c.isDigit() || c == '-' || c == '_' || c in 'A'..'Z' || c in 'a'..'z'

/** 取件码形态：数字/分隔符/字母组成，至少 2 个数字，长度 3..24 */
fun looksLikeCode(s: String): Boolean {
    if (s.length !in 3..24) return false
    if (s.any { !isCodeChar(it) }) return false
    return s.count { it.isDigit() } >= 2
}

private val ADDRESS_HINTS = listOf(
    "驿站", "快递", "柜", "店", "门", "号", "楼", "路", "街", "小区", "大厦", "中心", "园区",
    "仓", "站", "点", "室", "铺", "村", "镇", "区", "县", "市", "家园", "花园", "公寓"
)

/** 地址形态：中文且命中地名关键词（短串必须命中关键词，长串放宽） */
fun looksLikeAddress(s: String): Boolean {
    if (s.length < 2) return false
    if (s.none { it in '\u4e00'..'\u9fff' }) return false
    return ADDRESS_HINTS.any { s.contains(it) } || s.length >= 6
}

/** 分词块里常见的地址引导词，剥离后才是地址本体（含冒号变体：`地址:xxx`） */
private val ADDRESS_LEADINS = listOf(
    "取件地址:", "取件地址：", "地址:", "地址：", "地址",
    "已到达", "已到", "到达", "存放于", "存放在", "位于", "在", "至", "到"
)

/** 在 token 里找最像取件码的连续片段：优先最长，同长取最靠右（取件码通常在引导词之后） */
private fun findCodeInside(token: String): String? {
    val runs = mutableListOf<String>()
    val sb = StringBuilder()
    for (c in token) {
        if (isCodeChar(c)) sb.append(c) else { if (sb.isNotEmpty()) { runs += sb.toString(); sb.clear() } }
    }
    if (sb.isNotEmpty()) runs += sb.toString()
    return runs.filter { looksLikeCode(it) }
        .maxWithOrNull(compareBy<String> { it.length }.thenBy { token.lastIndexOf(it) })
}

/** 地址：剥掉引导词后取词块尾部（地址通常占据尾部） */
private fun findAddressInside(token: String): String? {
    var t = token.trim()
    ADDRESS_LEADINS.forEach { lead -> if (t.startsWith(lead)) t = t.removePrefix(lead).trim() }
    if (looksLikeAddress(t)) return t
    // 从右往左缩，找最短的、仍像地址的尾部（「绿洲家园门1002号」优于整句）
    for (start in t.indices) {
        val candidate = t.substring(start).trim()
        if (looksLikeAddress(candidate) && ADDRESS_HINTS.any { candidate.contains(it) }) return candidate
    }
    return t.takeIf { it.isNotEmpty() }
}

/** 按 prefix/suffix 从正文取字段值；prefix 找不到或 suffix 找不到返回 null */
fun extractByAnchor(text: String, prefix: String, suffix: String): String? {
    if (prefix.isEmpty()) return null
    val start = text.indexOf(prefix)
    if (start < 0) return null
    val from = start + prefix.length
    val end = if (suffix.isEmpty()) text.length else {
        val at = text.indexOf(suffix, from)
        if (at < 0) return null
        at
    }
    return text.substring(from, end).trim().ifEmpty { null }
}

/** 值右侧最近的分隔符；没有则空串（读到文末） */
private fun suffixAfterValue(text: String, valueEnd: Int): String {
    var i = valueEnd
    while (i < text.length) {
        if (text[i] in STOP_CHARS) return text[i].toString()
        i++
    }
    return ""
}

/**
 * 生成候选前缀：从值左侧最短文本开始，逐步向左加长。
 * 每次加长都会重新自检，只有能还原出值的候选才会被采用。
 */
private fun resolvePrefix(text: String, token: String, valueInTokenAt: Int, value: String, suffix: String): String? {
    val tokenIdx = text.indexOf(token).let { if (it < 0) return null else it }
    val valueAbs = tokenIdx + valueInTokenAt

    // 候选 1：词块内部值左侧的文本（最贴合模板语义，如「请凭取件码」）
    val inToken = token.substring(0, valueInTokenAt)
    // 候选 2：再往前吃掉正文里的前一个字符（分隔符/字），逐字加长
    val candidates = mutableListOf<String>()
    if (inToken.isNotEmpty()) candidates += inToken
    if (inToken.isEmpty()) {
        var i = valueAbs - 1
        while (i >= 0) {
            candidates += text.substring(i, valueAbs)
            i--
        }
    } else {
        var i = tokenIdx - 1
        while (i >= 0) {
            candidates += text.substring(i, tokenIdx) + inToken
            i--
        }
    }

    return candidates.firstOrNull { extractByAnchor(text, it, suffix) == value }
}

/**
 * 从用户点中的 [token] 推导锚点。
 *
 * @param text  完整短信正文
 * @param token 用户点中的词块
 * @param field 当前配置的字段
 * @return 推导结果；返回 null 表示无法可靠推导，调用方应提示用户手动填写
 */
fun suggestAnchor(text: String, token: String, field: AnchorField): AnchorSuggestion? {
    if (token.isEmpty() || token !in text) return null

    // 品牌 = 短信签名【】内部
    if (field == AnchorField.BRAND) {
        val inner = token.takeIf { it != "【" && it != "】" } ?: return null
        if (extractByAnchor(text, "【", "】") == inner) return AnchorSuggestion(inner, "【", "】")
        return null
    }
    // 发件方不是正文锚点，由调用方直接取 token
    if (field == AnchorField.SENDER) return null

    val value = when (field) {
        AnchorField.PICKUP_CODE, AnchorField.TRACKING, AnchorField.PHONE -> findCodeInside(token)
        AnchorField.ADDRESS -> findAddressInside(token)
        else -> null
    } ?: return null

    val valueAt = token.indexOf(value)
    if (valueAt < 0) return null

    // 后缀候选（按优先级，第一个能自检还原出值的胜出）：
    // 1. 词块内值右侧的分隔符（「，」这类，最贴近模板语义）
    // 2. 值右侧正文里的分隔符（跨过词块内的正文，如「…到绿洲家园…」的「，」）
    // 3. 值右侧紧跟的那一个字（兜底终止符，避免 suffix 为空时读到文末）
    // 4. 空串（值在短信末尾）
    val tokenIdx = text.indexOf(token)
    val valueEndAbs = tokenIdx + valueAt + value.length
    val restInToken = token.substring(valueAt + value.length)
    val nextCharAbs = text.getOrNull(valueEndAbs)?.toString()
    val suffixCandidates = buildList {
        if (restInToken.isNotEmpty() && restInToken.first() in STOP_CHARS) add(restInToken.first().toString())
        suffixAfterValue(text, valueEndAbs).takeIf { it.isNotEmpty() }?.let { add(it) }
        if (restInToken.isNotEmpty()) add(restInToken.take(1))
        if (nextCharAbs != null) add(nextCharAbs)
        add("")
    }.distinct()

    for (suffix in suffixCandidates) {
        val prefix = resolvePrefix(text, token, valueAt, value, suffix) ?: continue
        return AnchorSuggestion(value = value, prefix = prefix, suffix = suffix)
    }
    return null
}
