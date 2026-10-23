package com.quickpick.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 沉浸模式分组（v1.1.7 §11） */
class ImmersionGroupTest {

    private fun pkg(id: Long, address: String?, ts: Long) = PackageRecord(
        id = id, companyName = "快递", pickupCode = "C$id",
        address = address, smsBody = "", smsTimestamp = ts
    )

    @Test
    fun `groups by address, null goes to 其他`() {
        val groups = groupForImmersion(
            listOf(
                pkg(1, "家门口", 100), pkg(2, null, 200), pkg(3, "家门口", 300)
            )
        )
        assertEquals(2, groups.size)
        assertEquals(setOf("家门口", "其他"), groups.map { it.location }.toSet())
    }

    @Test
    fun `groups sorted by count desc`() {
        val groups = groupForImmersion(
            listOf(
                pkg(1, "公司", 100),
                pkg(2, "家门口", 100), pkg(3, "家门口", 200), pkg(4, "家门口", 300)
            )
        )
        assertEquals("家门口", groups[0].location)
        assertEquals(3, groups[0].items.size)
    }

    @Test
    fun `items within group sorted by time desc`() {
        val groups = groupForImmersion(
            listOf(pkg(1, "家门口", 100), pkg(2, "家门口", 300), pkg(3, "家门口", 200))
        )
        assertEquals(listOf(2L, 3L, 1L), groups[0].items.map { it.id })
    }

    @Test
    fun `empty input gives empty groups`() {
        assertTrue(groupForImmersion(emptyList()).isEmpty())
    }

    @Test
    fun `equal counts ordered by location name for stability`() {
        val groups = groupForImmersion(
            listOf(pkg(1, "公司", 100), pkg(2, "家门口", 100))
        )
        // 同件数按 location 字典序（「公」U+516C < 「家」U+5BB6）保证稳定
        assertEquals(listOf("公司", "家门口"), groups.map { it.location })
    }
}
