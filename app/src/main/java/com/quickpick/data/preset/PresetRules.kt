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
}
