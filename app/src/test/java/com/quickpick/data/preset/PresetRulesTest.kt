package com.quickpick.data.preset

import com.quickpick.data.db.dao.MatchRuleDao
import com.quickpick.data.db.entity.MatchRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 预置规则的灌入/升级语义。FakeDao 内存实现；flags 用内存假实现（PresetFlags 接口就是为这里抽的）。 */
class PresetRulesTest {

    private class FakeFlags : PresetFlags {
        var seeded = false
        var v2 = false
        var v3 = false
        var v4 = false
        override suspend fun isRulesSeeded() = seeded
        override suspend fun setRulesSeeded() { seeded = true }
        override suspend fun isPresetV2Seeded() = v2
        override suspend fun setPresetV2Seeded() { v2 = true }
        override suspend fun isPresetV3Seeded() = v3
        override suspend fun setPresetV3Seeded() { v3 = true }
        override suspend fun isPresetV4Seeded() = v4
        override suspend fun setPresetV4Seeded() { v4 = true }
    }

    private class FakeDao : MatchRuleDao {
        val store = mutableListOf<MatchRuleEntity>()
        private var nextId = 1L
        override fun observeAll(): Flow<List<MatchRuleEntity>> = flowOf(store.toList())
        override suspend fun getAll() = store.toList()
        override suspend fun getById(id: Long) = store.firstOrNull { it.id == id }
        override suspend fun getByName(name: String) = store.filter { it.name == name }
        override suspend fun insert(rule: MatchRuleEntity): Long {
            val id = if (rule.id == 0L) nextId++ else rule.id
            store.removeAll { it.id == id }
            store.add(rule.copy(id = id))
            return id
        }
        override suspend fun update(rule: MatchRuleEntity) {
            store.replaceAll { if (it.id == rule.id) rule else it }
        }
        override suspend fun deleteById(id: Long) { store.removeAll { it.id == id } }
    }

    @Test
    fun `fresh install seeds exactly sf and jd minimal presets`() = runBlocking {
        val flags = FakeFlags()
        val dao = FakeDao()
        PresetRules.seedIfNeeded(flags, dao)
        assertEquals(2, dao.store.size)
        val sf = dao.getByName("顺丰速运").single()
        assertEquals("取件码：", sf.codePrefix)
        // 极简：不带代取点/电话/单号锚点
        assertNull(sf.addressPrefix)
        assertNull(sf.phonePrefix)
        assertNull(sf.trackingPrefix)
        assertEquals("【", sf.brandPrefix)
        assertNull(sf.senderPattern, "发件方过滤已移除（端口常变不可靠）")
    }

    @Test
    fun `v3 upgrade overwrites legacy presets to minimal shape`() = runBlocking {
        val flags = FakeFlags().apply { v2 = true }
        val dao = FakeDao()
        // v1.1.x 时代的旧形顺丰预置（带地址/电话/单号锚点）
        dao.insert(
            MatchRuleEntity(
                name = "顺丰速运", isPreset = true,
                trackingPrefix = "单号", trackingSuffix = "，",
                codePrefix = "取件码：", codeSuffix = "，",
                addressPrefix = "到达", addressSuffix = "，",
                phonePrefix = "电话：", phoneSuffix = ""
            )
        )
        PresetRules.upgradeToV3IfNeeded(flags, dao)
        val sf = dao.getByName("顺丰速运").single()
        assertNull(sf.addressPrefix, "v3 应清掉代取点地址锚")
        assertNull(sf.phonePrefix)
        assertNull(sf.trackingPrefix)
        assertEquals("取件码：", sf.codePrefix)
        assertTrue(flags.v3)
    }

    @Test
    fun `v3 never touches user custom rules`() = runBlocking {
        val flags = FakeFlags().apply { v2 = true }
        val dao = FakeDao()
        dao.insert(MatchRuleEntity(name = "顺丰速运", isPreset = true, codePrefix = "取件码："))
        dao.insert(
            MatchRuleEntity(
                name = "顺丰速运", isPreset = false,  // 用户自建同名
                codePrefix = "取件码", addressPrefix = "到"
            )
        )
        PresetRules.upgradeToV3IfNeeded(flags, dao)
        val custom = dao.getByName("顺丰速运").first { !it.isPreset }
        assertEquals("到", custom.addressPrefix, "用户自建规则一个字段都不许动")
    }

    @Test
    fun `v3 does not resurrect deleted presets and is one shot`() = runBlocking {
        val flags = FakeFlags().apply { v2 = true }
        val dao = FakeDao()
        dao.insert(MatchRuleEntity(name = "顺丰速运", isPreset = true, codePrefix = "取件码：", addressPrefix = "到达"))
        dao.insert(MatchRuleEntity(name = "京东物流", isPreset = true, codePrefix = "取货码"))
        PresetRules.upgradeToV3IfNeeded(flags, dao)
        // 用户删了顺丰预置
        dao.store.removeAll { it.name == "顺丰速运" }
        // 再来一次（重启后重跑）——不应复活
        PresetRules.upgradeToV3IfNeeded(flags, dao)
        assertTrue(dao.getByName("顺丰速运").isEmpty(), "已删预置不许复活")
    }

    @Test
    fun `fresh install skips upgrade path`() = runBlocking {
        val flags = FakeFlags()
        val dao = FakeDao()
        PresetRules.seedIfNeeded(flags, dao)
        PresetRules.upgradeToV3IfNeeded(flags, dao)
        assertEquals(2, dao.store.size, "seed 已是最新形态，v3 不应再改出第三条")
        assertEquals("取货码", dao.getByName("京东物流").single().codePrefix)
    }
}
