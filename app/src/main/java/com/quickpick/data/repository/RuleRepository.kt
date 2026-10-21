package com.quickpick.data.repository

import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.entity.MatchRuleEntity
import com.quickpick.domain.model.MatchRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RuleRepository(private val dao: MatchRuleDao) {

    fun observeAll(): Flow<List<MatchRule>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getAll(): List<MatchRule> =
        dao.getAll().map { it.toDomain() }

    suspend fun getById(id: Long): MatchRule? =
        dao.getById(id)?.toDomain()

    /** 导入时的同名冲突检测 */
    suspend fun getByName(name: String): List<MatchRule> =
        dao.getByName(name).map { it.toDomain() }

    suspend fun insert(rule: MatchRule): Long =
        dao.insert(rule.toEntity())

    /**
     * 更新规则。必须先读原记录：保留 isPreset（编辑预置规则不能降级成自定义）
     * 和 createdAt（创建时间不可被更新篡改），只刷新 updatedAt。
     */
    suspend fun update(rule: MatchRule) {
        val origin = dao.getById(rule.id) ?: return
        dao.update(
            rule.toEntity(isPreset = origin.isPreset)
                .copy(createdAt = origin.createdAt, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    private fun MatchRuleEntity.toDomain() = MatchRule(
        id = id, name = name, isPreset = isPreset,
        trackingPrefix = trackingPrefix, trackingSuffix = trackingSuffix,
        codePrefix = codePrefix, codeSuffix = codeSuffix,
        addressPrefix = addressPrefix, addressSuffix = addressSuffix,
        phonePrefix = phonePrefix, phoneSuffix = phoneSuffix,
        senderPattern = senderPattern,
        brandPrefix = brandPrefix, brandSuffix = brandSuffix,
        sampleSms = sampleSms
    )

    private fun MatchRule.toEntity(isPreset: Boolean = this.isPreset) = MatchRuleEntity(
        id = id, name = name, isPreset = isPreset,
        trackingPrefix = trackingPrefix, trackingSuffix = trackingSuffix,
        codePrefix = codePrefix, codeSuffix = codeSuffix,
        addressPrefix = addressPrefix, addressSuffix = addressSuffix,
        phonePrefix = phonePrefix, phoneSuffix = phoneSuffix,
        senderPattern = senderPattern,
        brandPrefix = brandPrefix, brandSuffix = brandSuffix,
        sampleSms = sampleSms,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
