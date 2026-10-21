package com.quickpick.util

import com.google.gson.*
import com.quickpick.domain.model.MatchRule

object JsonExporter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /** 全量导出：所有规则 → JSON 数组字符串 */
    fun exportAll(rules: List<MatchRule>): String {
        val array = JsonArray()
        rules.forEach { array.add(ruleToJson(it)) }
        return gson.toJson(array)
    }

    /** 单条导出 */
    fun exportSingle(rule: MatchRule): String {
        return gson.toJson(ruleToJson(rule))
    }

    /** 导入：JSON 字符串 → MatchRule 列表 */
    fun import(json: String): Result<List<MatchRule>> {
        return try {
            val element = JsonParser.parseString(json)
            val rules = when {
                element.isJsonArray -> {
                    element.asJsonArray.map { parseRule(it.asJsonObject) }
                }
                element.isJsonObject -> {
                    listOf(parseRule(element.asJsonObject))
                }
                else -> throw JsonParseException("不是有效的规则 JSON")
            }
            // 校验每条规则
            rules.forEach { validateRule(it) }
            Result.success(rules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ruleToJson(rule: MatchRule): JsonObject {
        return JsonObject().apply {
            addProperty("name", rule.name)
            // 发件方过滤：顶级字段，空值导出为 ""（与 fields 的空串风格一致）
            addProperty("senderPattern", rule.senderPattern ?: "")
            add("fields", JsonObject().apply {
                add("trackingNumber", fieldToJson(rule.trackingPrefix, rule.trackingSuffix))
                add("pickupCode", fieldToJson(rule.codePrefix, rule.codeSuffix))
                add("address", fieldToJson(rule.addressPrefix, rule.addressSuffix))
                add("phone", fieldToJson(rule.phonePrefix, rule.phoneSuffix))
                add("brand", fieldToJson(rule.brandPrefix, rule.brandSuffix))
            })
        }
    }

    private fun fieldToJson(prefix: String?, suffix: String?): JsonObject {
        return JsonObject().apply {
            addProperty("prefix", prefix ?: "")
            addProperty("suffix", suffix ?: "")
        }
    }

    private fun parseRule(obj: JsonObject): MatchRule {
        val name = obj.get("name")?.asString ?: throw JsonParseException("缺少 name")
        val fields = obj.getAsJsonObject("fields") ?: throw JsonParseException("缺少 fields")

        fun getField(key: String): Pair<String?, String?> {
            val f = fields.getAsJsonObject(key) ?: return null to null
            val p = f.get("prefix")?.asString?.takeIf { it.isNotEmpty() }
            val s = f.get("suffix")?.asString?.takeIf { it.isNotEmpty() }
            return p to s
        }

        val (tp, ts) = getField("trackingNumber")
        val (cp, cs) = getField("pickupCode")
        val (ap, as_) = getField("address")
        val (pp, ps) = getField("phone")
        // 向后兼容：旧 JSON 无 fields.brand → getField 返回 null to null
        val (bp, bs) = getField("brand")
        // 向后兼容：旧 JSON 无顶级 senderPattern → get 返回 null；空串同样归一为 null
        val sender = obj.get("senderPattern")?.takeIf { !it.isJsonNull }?.asString
            ?.takeIf { it.isNotEmpty() }

        return MatchRule(
            name = name,
            trackingPrefix = tp, trackingSuffix = ts,
            codePrefix = cp, codeSuffix = cs,
            addressPrefix = ap, addressSuffix = as_,
            phonePrefix = pp, phoneSuffix = ps,
            senderPattern = sender,
            brandPrefix = bp, brandSuffix = bs
        )
    }

    private fun validateRule(rule: MatchRule) {
        if (rule.name.isBlank()) throw JsonParseException("规则名称不能为空")
        if (!rule.hasAnyField) throw JsonParseException("至少需要配置一个字段")
    }

    /**
     * 清理 AI 返回的 JSON 文本：剥离 markdown 代码围栏（```json）和首尾杂文本。
     * 真实 LLM 经常带围栏输出，不剥离会直接解析失败。
     */
    fun sanitizeAiJson(raw: String): String {
        var text = raw.trim()
        text = text.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
        text = text.removeSuffix("```").trim()
        // 截取第一个 { 或 [ 到最后一个 } 或 ]，容忍 AI 在 JSON 前后加解释性文字
        val start = text.indexOfFirst { it == '{' || it == '[' }
        val end = text.indexOfLast { it == '}' || it == ']' }
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }
}
