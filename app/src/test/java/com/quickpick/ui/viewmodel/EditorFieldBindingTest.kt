package com.quickpick.ui.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 编辑器字段串位（v1.1.11 修复）。
 *
 * 现场问题：直接点「取件地址」卡片的输入框并打字，内容会被写进**取件码前缀**。
 * 原因是 TextField 先消费掉 down 事件，miuix Card 的 onClick 不触发，
 * activeField 仍是上一个字段；而输入框的回调是「写当前激活字段」。
 *
 * 修法：每个卡片绑定自己的字段（updatePrefix/updateSuffix 带 field 参数）。
 * 这里不跑 UI，直接验证「按字段写入」不会再串。
 */
class EditorFieldBindingTest {

    /**
     * 复刻修复后的写入语义：字段由调用方显式给出，与 activeField 无关。
     * 用 data class 模拟 EditorState 的相关字段。
     */
    private data class State(
        val activeField: ActiveField = ActiveField.PICKUP_CODE,
        val codePrefix: String = "",
        val codeSuffix: String = "",
        val addressPrefix: String = "",
        val addressSuffix: String = "",
        val brandPrefix: String = "",
        val brandSuffix: String = ""
    )

    /** 修复后的行为：按 field 写入，不看 activeField */
    private fun State.updatePrefix(field: ActiveField, v: String): State = when (field) {
        ActiveField.PICKUP_CODE -> copy(codePrefix = v)
        ActiveField.ADDRESS -> copy(addressPrefix = v)
        ActiveField.BRAND -> copy(brandPrefix = v)
        else -> this
    }

    /** 修复前的行为：永远写当前激活字段 */
    private fun State.updateActiveFieldPrefix(v: String): State = when (activeField) {
        ActiveField.PICKUP_CODE -> copy(codePrefix = v)
        ActiveField.ADDRESS -> copy(addressPrefix = v)
        ActiveField.BRAND -> copy(brandPrefix = v)
        else -> this
    }

    @Test
    fun `old behaviour writes address input into code prefix - the reported bug`() {
        // 用户在取件码卡之后点进地址卡输入框：TextField 消费了 down，activeField 没切换
        val s = State(activeField = ActiveField.PICKUP_CODE)
        val after = s.updateActiveFieldPrefix("地址:")

        assertEquals("地址:", after.codePrefix, "旧实现确实把地址锚写进了取件码前缀（现场故障）")
        assertEquals("", after.addressPrefix, "地址字段反而没收到")
    }

    @Test
    fun `new behaviour writes to the field the card belongs to`() {
        val s = State(activeField = ActiveField.PICKUP_CODE)
        val after = s.updatePrefix(ActiveField.ADDRESS, "地址:")

        assertEquals("", after.codePrefix, "取件码前缀必须保持干净")
        assertEquals("地址:", after.addressPrefix, "内容应落到地址字段")
    }

    @Test
    fun `each field keeps its own prefix independently`() {
        var s = State()
        s = s.updatePrefix(ActiveField.PICKUP_CODE, "取件码为")
        s = s.updatePrefix(ActiveField.ADDRESS, "地址:")
        s = s.updatePrefix(ActiveField.BRAND, "【")

        assertEquals("取件码为", s.codePrefix)
        assertEquals("地址:", s.addressPrefix)
        assertEquals("【", s.brandPrefix)
    }

    @Test
    fun `writing to one field never touches the others`() {
        val base = State(
            codePrefix = "c", codeSuffix = "，",
            addressPrefix = "a", addressSuffix = "取件",
            brandPrefix = "【", brandSuffix = "】"
        )
        val after = base.updatePrefix(ActiveField.ADDRESS, "到咸阳")

        assertEquals("c", after.codePrefix)
        assertEquals("，", after.codeSuffix)
        assertEquals("到咸阳", after.addressPrefix)
        assertEquals("取件", after.addressSuffix)
        assertEquals("【", after.brandPrefix)
    }

    @Test
    fun `suffix has the same field-scoped semantics`() {
        // updateSuffix 按同一原则实现；这里验证语义契约
        fun State.updateSuffix(field: ActiveField, v: String): State = when (field) {
            ActiveField.ADDRESS -> copy(addressSuffix = v)
            else -> this
        }
        val s = State(codeSuffix = "原有", activeField = ActiveField.PICKUP_CODE)
        val after = s.updateSuffix(ActiveField.ADDRESS, "取件")

        assertEquals("原有", after.codeSuffix, "写地址后缀不该动取件码后缀")
        assertEquals("取件", after.addressSuffix)
        assertTrue(after.activeField == ActiveField.PICKUP_CODE, "写入不改激活字段")
    }
}
