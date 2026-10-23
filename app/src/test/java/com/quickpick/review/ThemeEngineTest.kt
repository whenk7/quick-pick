package com.quickpick.review

import com.quickpick.ui.theme.resolveTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 主题决策纯函数（v1.1.7 §7 砍引擎）：
 * 动态取色开+API≥31→壁纸 Monet；关动态+种子色→种子 Monet；关动态+无种子（默认）→miuix 内置板。
 * API<31 无视动态取色（无壁纸色源）。
 */
class ThemeEngineTest {

    @Test
    fun `dynamic color on modern api uses wallpaper monet`() {
        val d = resolveTheme(dynamicColor = true, seedColor = null, sdkInt = 35)
        assertTrue(d.monet)
        assertEquals(null, d.keyColor)
    }

    @Test
    fun `seed color selected uses seed monet`() {
        val d = resolveTheme(dynamicColor = false, seedColor = 0xFF3482FF.toInt(), sdkInt = 35)
        assertTrue(d.monet)
        assertEquals(0xFF3482FF.toInt(), d.keyColor)
    }

    @Test
    fun `no seed means miuix builtin palette`() {
        // 默认态/原 FIXED 用户升级路径：dynamicColor 缺失默认 false + seedColor 缺失为 null
        val d = resolveTheme(dynamicColor = false, seedColor = null, sdkInt = 35)
        assertFalse(d.monet)
        assertNull(d.keyColor)
    }

    @Test
    fun `dynamic color ignored below api31`() {
        val d = resolveTheme(dynamicColor = true, seedColor = 0xFF3482FF.toInt(), sdkInt = 30)
        assertTrue(d.monet)
        assertEquals(0xFF3482FF.toInt(), d.keyColor)
    }

    @Test
    fun `dynamic color below api31 without seed falls to builtin`() {
        val d = resolveTheme(dynamicColor = true, seedColor = null, sdkInt = 30)
        assertFalse(d.monet)
        assertNull(d.keyColor)
    }
}
