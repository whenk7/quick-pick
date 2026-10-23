package com.quickpick.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 地址别名映射（v1.1.7 §6）：按列表顺序 contains 首个命中；空白 match 无效跳过 */
class AddressAliasTest {

    private val aliases = listOf(
        AddressAlias("绿洲家园", "家门口"),
        AddressAlias("科创中心", "公司")
    )

    @Test
    fun `first match wins`() {
        assertEquals("家门口", applyAddressAlias("绿洲家园西南门袋鼠柜A01", aliases))
        assertEquals("公司", applyAddressAlias("科创中心 B2 栋丰巢柜", aliases))
    }

    @Test
    fun `no match returns original`() {
        assertEquals("人民广场驿站", applyAddressAlias("人民广场驿站", aliases))
    }

    @Test
    fun `null address stays null`() {
        assertNull(applyAddressAlias(null, aliases))
    }

    @Test
    fun `empty list returns original`() {
        assertEquals("绿洲家园", applyAddressAlias("绿洲家园", emptyList()))
    }

    @Test
    fun `blank match never matches anything`() {
        // contains("") 恒 true——空白 match 会把所有地址都替换掉，必须跳过
        val bad = listOf(AddressAlias("  ", "垃圾"), AddressAlias("绿洲家园", "家门口"))
        assertEquals("家门口", applyAddressAlias("绿洲家园3号柜", bad))
        assertEquals("别处", applyAddressAlias("别处", bad))
    }

    @Test
    fun `order matters when overlapping`() {
        val ordered = listOf(
            AddressAlias("绿洲家园西南门", "西南门"),
            AddressAlias("绿洲家园", "家门口")
        )
        assertEquals("西南门", applyAddressAlias("绿洲家园西南门袋鼠柜", ordered))
    }

    // ---- upsert（终审 Important #1：重复 match → LazyColumn key 冲突崩溃循环）----

    @Test
    fun `upsert same match replaces old entry`() {
        val list = listOf(AddressAlias("绿洲家园", "家门口"), AddressAlias("科创中心", "公司"))
        val result = upsertAddressAlias(list, AddressAlias("绿洲家园", "家"))
        assertEquals(2, result.size, "同 match 覆盖不新增")
        assertEquals("家", result.first { it.match == "绿洲家园" }.alias, "旧别名被新值替换")
        assertEquals("科创中心", result[0].match, "其余条目顺序不动")
        assertEquals("家", applyAddressAlias("绿洲家园3号柜", result), "覆盖后命中新值")
    }

    @Test
    fun `upsert new match appends`() {
        val result = upsertAddressAlias(emptyList(), AddressAlias("a", "b"))
        assertEquals(listOf(AddressAlias("a", "b")), result)
    }
}
