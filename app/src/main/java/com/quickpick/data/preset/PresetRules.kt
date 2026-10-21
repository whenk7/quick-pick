package com.quickpick.data.preset

import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.entity.MatchRuleEntity
import com.quickpick.data.local.SettingsStore

object PresetRules {

    val all: List<MatchRuleEntity> = listOf(
        MatchRuleEntity(
            name = "顺丰速运",
            isPreset = true,
            trackingPrefix = "单号",
            trackingSuffix = "，",
            codePrefix = "取件码：",
            codeSuffix = "，",
            addressPrefix = "到达",
            addressSuffix = "，",
            phonePrefix = "电话：",
            phoneSuffix = "",
            senderPattern = "1069",
            // 品牌锚点 = SMS 签名括号：提取【】内完整签名（如「顺丰速运」）；无签名 → null 回退 rule.name
            brandPrefix = "【",
            brandSuffix = "】",
            sampleSms = "【顺丰速运】您的包裹已到达菜鸟驿站(阳光花园店)，取件码：3-2-1088，单号SF1234567890123，请尽快取件。电话：13800001234"
        ),
        MatchRuleEntity(
            name = "京东物流",
            isPreset = true,
            trackingPrefix = "订单",
            trackingSuffix = "已送达",
            codePrefix = "取货码",
            codeSuffix = "，",
            addressPrefix = "送达",
            addressSuffix = "，",
            phonePrefix = "联系电话",
            phoneSuffix = "，",
            senderPattern = "1069",
            brandPrefix = "【",
            brandSuffix = "】",
            sampleSms = "【京东物流】您的订单4901234567已送达京东便利店(万达店)，取货码8-3-1024，联系电话13800005678，请尽快取件。"
        )
    )

    /**
     * 仅首次启动灌入预置规则。
     * 判定用 DataStore 标记而非「表是否为空」——否则用户删光规则后下次启动预置会复活，
     * 「用户可删除预置规则」和「删光规则的空状态」都会被破坏。
     */
    suspend fun seedIfNeeded(store: SettingsStore, dao: MatchRuleDao) {
        if (store.isRulesSeeded()) return
        all.forEach { dao.insert(it) }
        store.setRulesSeeded()
    }

    /**
     * v1.0.0 老用户升级：给已灌入的旧预置规则补 senderPattern/brandPrefix/brandSuffix 三个新字段。
     * 一次性：标记 preset_v2_seeded 后不再跑（幂等）。
     * 只按 name 匹配 isPreset=true 的规则更新，用户自建规则与已删除的预置不受影响；
     * 其余字段（含用户对预置规则的自定义修改）原样保留，只覆写三个新字段。
     */
    suspend fun upgradeIfNeeded(store: SettingsStore, dao: MatchRuleDao) {
        if (store.isPresetV2Seeded()) return
        all.forEach { preset ->
            dao.getByName(preset.name)
                .filter { it.isPreset }
                .forEach { old ->
                    dao.update(
                        old.copy(
                            senderPattern = preset.senderPattern,
                            brandPrefix = preset.brandPrefix,
                            brandSuffix = preset.brandSuffix
                        )
                    )
                }
        }
        store.setPresetV2Seeded()
    }
}
