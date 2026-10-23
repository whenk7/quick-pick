package com.quickpick.data.preset

import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.entity.MatchRuleEntity

/** 预置灌入/升级需要的持久化标记（SettingsStore 实现；接口是为了纯 JVM 单测可假造） */
interface PresetFlags {
    suspend fun isRulesSeeded(): Boolean
    suspend fun setRulesSeeded()
    suspend fun isPresetV2Seeded(): Boolean
    suspend fun setPresetV2Seeded()
    suspend fun isPresetV3Seeded(): Boolean
    suspend fun setPresetV3Seeded()
}

object PresetRules {

    /**
     * 预置规则（v3 极简形，2026-10-23 裁决）：只留取件码 + 品牌签名。
     * 不带代取点/电话/单号锚点——公共包不配具体地点；senderPattern=1069 防政务端口误报。
     */
    val all: List<MatchRuleEntity> = listOf(
        MatchRuleEntity(
            name = "顺丰速运",
            isPreset = true,
            codePrefix = "取件码：",
            codeSuffix = "，",
            senderPattern = "1069",
            brandPrefix = "【",
            brandSuffix = "】",
            sampleSms = "【顺丰速运】您的包裹已到达驿站，取件码：3-2-1088，请尽快取件。"
        ),
        MatchRuleEntity(
            name = "京东物流",
            isPreset = true,
            codePrefix = "取货码",
            codeSuffix = "，",
            senderPattern = "1069",
            brandPrefix = "【",
            brandSuffix = "】",
            sampleSms = "【京东物流】您的订单已送达，取货码8-3-1024，请尽快取件。"
        )
    )

    /**
     * 仅首次启动灌入预置规则。
     * 判定用 DataStore 标记而非「表是否为空」——否则用户删光规则后下次启动预置会复活，
     * 「用户可删除预置规则」和「删光规则的空状态」都会被破坏。
     */
    suspend fun seedIfNeeded(store: PresetFlags, dao: MatchRuleDao) {
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
    suspend fun upgradeIfNeeded(store: PresetFlags, dao: MatchRuleDao) {
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

    /**
     * v1.1.4：预置极简重置（去代取点/电话/单号锚点，只留取件码+品牌）。
     * 按名覆盖 isPreset=true 的存量两条（含用户不曾自定义的情况）；用户自建规则不碰，
     * 已删除的预置不复活（只 update 不 insert）。
     */
    suspend fun upgradeToV3IfNeeded(store: PresetFlags, dao: MatchRuleDao) {
        if (store.isPresetV3Seeded()) return
        all.forEach { preset ->
            dao.getByName(preset.name)
                .filter { it.isPreset }
                .forEach { old ->
                    dao.update(
                        old.copy(
                            trackingPrefix = null, trackingSuffix = null,
                            codePrefix = preset.codePrefix, codeSuffix = preset.codeSuffix,
                            addressPrefix = null, addressSuffix = null,
                            phonePrefix = null, phoneSuffix = null,
                            senderPattern = preset.senderPattern,
                            brandPrefix = preset.brandPrefix, brandSuffix = preset.brandSuffix,
                            sampleSms = preset.sampleSms
                        )
                    )
                }
        }
        store.setPresetV3Seeded()
    }
}
