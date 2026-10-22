package com.quickpick.corpus

/**
 * golden 期望值。标注约定（已和用户对齐）：
 * - company = 胜出规则名（引擎 companyName = rule.name，非签名原文）
 * - address 含柜号/铺号（数据模型无独立柜号字段）
 * - tracking 仅正文真实含单号时标注（E009/E019）
 * - phone 全语料恒 null（正文无电话），不逐条标注
 * - brand = 【】签名原文
 * - 多码短信（E004/E021）按边界裁决：整串提取
 */
data class Expected(
    val match: Boolean,
    val company: String? = null,
    val code: String? = null,
    val address: String? = null,
    val tracking: String? = null,
    val brand: String? = null
)

/** 快递语料 golden（E001–E032；E031 为边界负例：售后上门取件预约，非取件场景） */
val GOLD: Map<String, Expected> = mapOf(
    "E001" to Expected(true, "兔喜生活", "11-2-0272", "绿洲家园7号楼商铺S1001号", null, "兔喜生活"),
    "E002" to Expected(true, "袋鼠智柜", "01259354", "绿洲家园西南门袋鼠柜B09", null, "袋鼠智柜"),
    "E003" to Expected(true, "袋鼠智柜", "2946-3356", "绿洲家园西南门袋鼠柜E25", null, "袋鼠智柜"),
    "E004" to Expected(true, "菜鸟驿站", "2-3-0644, 1-2-0602", "咸阳绿洲家园西门店", null, "菜鸟驿站"),
    "E005" to Expected(true, "菜鸟驿站", "4-5-0877", "绿洲家园西门店", null, "菜鸟驿站"),
    "E006" to Expected(true, "袋鼠智柜", "10358508", "绿洲家园西南门袋鼠柜G57", null, "袋鼠智柜"),
    "E007" to Expected(true, "菜鸟驿站", "6-5-0485", "绿洲家园西门店", null, "菜鸟驿站"),
    "E008" to Expected(true, "袋鼠智柜", "67721113", "绿洲家园西南门袋鼠柜G62", null, "袋鼠智柜"),
    "E009" to Expected(true, "袋鼠智柜", "2877-2837", "绿洲家园西南门袋鼠柜G59", "YT0073828884344", "袋鼠智柜"),
    "E010" to Expected(true, "妈妈驿站", "19-55", "花园路绿洲家园门1002号", null, "妈妈驿站"),
    "E011" to Expected(true, "袋鼠智柜", "2877-2837", "绿洲家园西南门袋鼠柜G59", null, "袋鼠智柜"),
    "E012" to Expected(true, "兔喜生活", "10-4-3185", "绿洲家园", null, "兔喜生活"),
    "E013" to Expected(true, "菜鸟驿站", "4-6-0251", "绿洲家园西门店", null, "菜鸟驿站"),
    "E014" to Expected(true, "袋鼠智柜", "63052007", "绿洲家园西南门袋鼠柜E05", null, "袋鼠智柜"),
    "E015" to Expected(true, "兔喜生活", "8-3-0093", "绿洲家园便利", null, "兔喜生活"),
    "E016" to Expected(true, "兔喜生活", "9-3-1606", "绿洲家园便利", null, "兔喜生活"),
    "E017" to Expected(true, "菜鸟驿站", "4-6-0212", "绿洲家园西门店", null, "菜鸟驿站"),
    "E018" to Expected(true, "丰巢", "28026838", "科创中心橡树A座丰巢柜", null, "丰巢"),
    "E019" to Expected(true, "袋鼠智柜", "36298145", "绿洲家园西南门袋鼠柜E19", "YT8896928182079", "袋鼠智柜"),
    "E020" to Expected(true, "袋鼠智柜", "03679376", "绿洲家园西南门袋鼠柜C07", null, "袋鼠智柜"),
    "E021" to Expected(true, "菜鸟驿站", "4-3-0647, 2-5-0257", "咸阳绿洲家园西门店", null, "菜鸟驿站"),
    "E022" to Expected(true, "菜鸟驿站", "6-6-0241", "绿洲家园西门店", null, "菜鸟驿站"),
    "E023" to Expected(true, "菜鸟驿站", "2-1-0222", "绿洲家园西门店", null, "菜鸟驿站"),
    "E024" to Expected(true, "圆通速递", "44651320", "绿洲家园西南门袋鼠柜A01", null, "圆通速递"),
    "E025" to Expected(true, "圆通速递", "44651320", "绿洲家园西南门袋鼠柜A01", null, "圆通速递"),
    "E026" to Expected(true, "兔喜生活", "10-4-1762", "绿洲家园便利", null, "兔喜生活"),
    "E027" to Expected(true, "兔喜生活", "9-1-0852", "绿洲家园便利", null, "兔喜生活"),
    "E028" to Expected(true, "妈妈驿站", "25-29", "花园路绿洲家园门1002号", null, "妈妈驿站"),
    "E029" to Expected(true, "菜鸟驿站", "6-2-0012", "绿洲家园西门店", null, "菜鸟驿站"),
    "E030" to Expected(true, "菜鸟驿站", "6-2-0012", "绿洲家园西门店", null, "菜鸟驿站"),
    // 售后上门取件预约（含「取件码2025」），非取件场景——期望不提取（已与用户对齐）
    "E031" to Expected(false),
    "E032" to Expected(true, "兔喜生活", "7-1-0679", "绿洲家园便利", null, "兔喜生活"),
)

/** 变体（用户要求的同码异模板 + 边界）。期望全部为「按设计应如此」的行为。 */
data class VariantSms(val id: String, val text: String, val expected: Expected, val note: String)

val VARIANTS: List<VariantSms> = listOf(
    // ---- 同包裹不同模板 → 同码（R3）----
    VariantSms(
        "V01",
        "【兔喜生活】您有包裹滞留绿洲家园便利店超48小时，取件码为11-2-0272，请尽快取件",
        Expected(true, "兔喜生活", "11-2-0272", null, null, "兔喜生活"),
        "与 E001 同码；催取模板无「地址:」锚 → 仅提码。注意：措辞不得含「包裹在」——那是菜鸟多件规则的地址锚（撞锚会被 2 字段对 1 字段抢走，首版 V01 踩过）"
    ),
    VariantSms(
        "V02",
        "【丰巢】您的快递已超期，凭取件码28026838至科创中心橡树A座丰巢柜取件，逾期将收取占位费",
        Expected(true, "丰巢", "28026838", "科创中心橡树A座丰巢柜", null, "丰巢"),
        "与 E018 同码同柜；超期模板"
    ),
    VariantSms(
        "V10",
        "【菜鸟驿站】您的包裹已到站，凭6-2-0012到咸阳绿洲家园西门店取件。",
        Expected(true, "菜鸟驿站", "6-2-0012", "绿洲家园西门店", null, "菜鸟驿站"),
        "与 E029/E030 同码；第三日重复推送"
    ),

    // ---- 边界行为刻画（characterization：记录引擎现状语义，非缺陷）----
    VariantSms(
        "V05",
        "【菜鸟驿站】您尾号0877的包裹已于今日18:20被取走，感谢使用",
        Expected(false),
        "取件完成通知无锚点 → 不提取"
    ),
    VariantSms(
        "V06",
        "【兔喜生活】您有包裹已到达绿洲家园便利店，取件码为，地址:绿洲家园便利",
        Expected(true, "兔喜生活", null, "绿洲家园便利", null, "兔喜生活"),
        "码为空串 → null；仅地址命中仍入库（isPickupRelated 只守 code/address）"
    ),
    VariantSms(
        "V07",
        "【兔喜生活】您有包裹已到达绿洲家园便利店，取件码为10-4-3185，地址：绿洲家园",
        Expected(true, "兔喜生活", "10-4-3185", null, null, "兔喜生活"),
        "全角冒号≠半角锚「地址:」→ 地址 miss，仅提码（全半角精确匹配是设计）"
    ),
    VariantSms(
        "V08",
        "【丰巢】凭取件码\n28026838至科创中心橡树A座丰巢柜取件。",
        Expected(true, "丰巢", "28026838", "科创中心橡树A座丰巢柜", null, "丰巢"),
        "跨行锚点：换行视为普通字符，trim 后码干净"
    ),
    VariantSms(
        "V09",
        "【兔喜生活】" + "天气提示占位文字".repeat(150) + "您有包裹已到达绿洲家园便利店，取件码为8-3-0093，地址:绿洲家园便利",
        Expected(true, "兔喜生活", "8-3-0093", "绿洲家园便利", null, "兔喜生活"),
        "超长正文（>1500 字）不影响锚点定位。填充只在前面——「地址:」suffix 为空读到文末，后面有字就会灌进地址"
    ),
)

/**
 * 已知发现（首红即缺陷的证据，修复前保持 @Ignore）：
 * F1 = 预置顺丰 addressPrefix「到达」过松，营销短信可误报（见 KnownFindingsTest）
 * F2 = 退货上门短信含「取货码」时纯正文锚点不可区分（正解=品牌过滤升格，v1.1.2）
 */
val KNOWN_FAILURES: List<VariantSms> = listOf(
    VariantSms(
        "V03",
        "【优选商城】您的包裹到达提醒服务已开通，回复T退订",
        Expected(false),
        "F1：顺丰预置 addressPrefix「到达」+ suffix「，」会提出垃圾地址「提醒服务已开通」入库"
    ),
    VariantSms(
        "V04",
        "【抖音】取货码8899，您的退货申请已通过，快递员明天上门取件",
        Expected(false),
        "F2: 「取货码」锚点在圆通/妈妈驿站规则上命中，1 字段即过闸（isPickupRelated）"
    ),
)

/** R3：同包裹不同模板的同码组（组内所有条目的提取码必须相等） */
val SAME_CODE_GROUPS: List<List<String>> = listOf(
    listOf("E024", "E025"),          // 圆通 44651320：存放多日 / 已到
    listOf("E009", "E011"),          // 袋鼠 2877-2837：超时未取 / 标准提醒
    listOf("E029", "E030", "V10"),   // 菜鸟 6-2-0012：三日重复推送
    listOf("E001", "V01"),           // 兔喜 11-2-0272：到达 / 催取
    listOf("E018", "V02"),           // 丰巢 28026838：存入 / 超期
)
