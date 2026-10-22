package com.quickpick.corpus

import com.quickpick.data.preset.PresetRules
import com.quickpick.domain.model.MatchRule

/**
 * 语料测试用规则集：等价于真实用户对照自己短信手工配置的产物。
 *
 * 设计原则：锚点取「模板特征长锚」，宁长勿短——短锚点会撞 indexOf 首次出现陷阱
 * （如菜鸟「已到站，凭X到Y」的 address 锚「到」）和跨规则误命中（圆通/妈妈驿站句式同构）。
 * 品牌锚点统一【】（SMS 签名）。
 *
 * 发件方端口在同品牌内不统一（语料实证：袋鼠×3、菜鸟×4、兔喜×3、圆通为个人手机号），
 * senderPattern 全部留空——发件方过滤的独立测试见 MatchEngineTest 的合成矩阵。
 */
object TestRules {

    private const val BP = "【"
    private const val BS = "】"

    val all: List<MatchRule> = listOf(
        // 兔喜生活：您有包裹已到达X，取件码为Y，地址:Z（半角冒号）
        MatchRule(
            name = "兔喜生活",
            codePrefix = "取件码为", codeSuffix = "，",
            addressPrefix = "地址:", addressSuffix = "",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 袋鼠智柜·标准：快递员提醒您，请凭取件码X到Y取您的快递
        // address 锚「到」依赖本模板「请凭取件码」之前无「到」字——脆，但这是唯一静态锚
        MatchRule(
            name = "袋鼠智柜",
            codePrefix = "请凭取件码", codeSuffix = "到",
            addressPrefix = "到", addressSuffix = "取您的快递",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 袋鼠智柜·超时：您存放在X的快递Y超时未取，请及时来取，取件码是Z
        MatchRule(
            name = "袋鼠智柜",
            codePrefix = "取件码是", codeSuffix = "",
            addressPrefix = "您存放在", addressSuffix = "的快递",
            trackingPrefix = "的快递", trackingSuffix = "超时未取",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 袋鼠智柜·滞留：同超时模板，但单号 suffix 是「已滞留」——拆两条规则覆盖
        MatchRule(
            name = "袋鼠智柜",
            codePrefix = "取件码是", codeSuffix = "",
            addressPrefix = "您存放在", addressSuffix = "的快递",
            trackingPrefix = "的快递", trackingSuffix = "已滞留",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 菜鸟驿站·单件：您的包裹已到站，凭X到Y取件。
        // address 锚「到」会撞「已到站」（indexOf 首次出现），必须用「到咸阳」级长锚
        MatchRule(
            name = "菜鸟驿站",
            codePrefix = "，凭", codeSuffix = "到",
            addressPrefix = "到咸阳", addressSuffix = "取件",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 菜鸟驿站·多件：您有N个包裹在X，取件码为A, B
        // 多码整串提取（引擎单值模型边界，见 KnownFindingsTest F3 注释）
        MatchRule(
            name = "菜鸟驿站",
            codePrefix = "取件码为", codeSuffix = "",
            addressPrefix = "包裹在", addressSuffix = "，",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 妈妈驿站：取货码X，您有圆通快递包裹，已到Y
        // address 锚用「包裹，已到」（带逗号）与圆通「包裹已到」消歧——短一字符就 tie 贴错牌
        MatchRule(
            name = "妈妈驿站",
            codePrefix = "取货码", codeSuffix = "，",
            addressPrefix = "包裹，已到", addressSuffix = "",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 圆通速递·存放：取货码X，您有圆通包裹存放Y多日，请尽快来取
        MatchRule(
            name = "圆通速递",
            codePrefix = "取货码", codeSuffix = "，",
            addressPrefix = "包裹存放", addressSuffix = "多日",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 圆通速递·已到：取货码X，您有圆通包裹已到Y
        MatchRule(
            name = "圆通速递",
            codePrefix = "取货码", codeSuffix = "，",
            addressPrefix = "包裹已到", addressSuffix = "",
            brandPrefix = BP, brandSuffix = BS
        ),
        // 丰巢：凭取件码X至Y取件。尾部营销文案（靠 suffix「取件」截断）
        MatchRule(
            name = "丰巢",
            codePrefix = "凭取件码", codeSuffix = "至",
            addressPrefix = "至", addressSuffix = "取件",
            brandPrefix = BP, brandSuffix = BS
        ),
    )

    /** 生产预置规则（顺丰/京东），映射为 domain 模型参与全量语料交叉验证 */
    val presets: List<MatchRule> = PresetRules.all.map { e ->
        MatchRule(
            id = e.id, name = e.name, isPreset = e.isPreset,
            trackingPrefix = e.trackingPrefix, trackingSuffix = e.trackingSuffix,
            codePrefix = e.codePrefix, codeSuffix = e.codeSuffix,
            addressPrefix = e.addressPrefix, addressSuffix = e.addressSuffix,
            phonePrefix = e.phonePrefix, phoneSuffix = e.phoneSuffix,
            senderPattern = e.senderPattern,
            brandPrefix = e.brandPrefix, brandSuffix = e.brandSuffix,
            sampleSms = e.sampleSms
        )
    }

    val presetNames: Set<String> get() = presets.map { it.name }.toSet()

    /** 模拟真实用户环境：自建规则 + 预置规则同时在场 */
    val allWithPresets: List<MatchRule> get() = all + presets
}
