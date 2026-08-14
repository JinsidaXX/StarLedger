package com.starledger.app.core.allocation

import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.EnvelopeType
import com.starledger.app.core.model.RuleType
import org.junit.Assert.assertEquals
import org.junit.Test

class AllocationEngineTest {

    private fun rule(
        id: Long,
        name: String,
        type: RuleType,
        value: Long = 0,
        percent: Int = 0,
        order: Int = 0,
        envelopeType: EnvelopeType = EnvelopeType.NECESSARY,
    ) = AllocationRule(
        id = id, templateId = 1, name = name, ruleType = type,
        value = value, percent = percent, sortOrder = order,
        envelopeType = envelopeType,
    )

    @Test
    fun `固定金额加全部剩余 恰好分配完`() {
        val rules = listOf(
            rule(1, "饮食", RuleType.FIXED_AMOUNT, value = 900_00L, order = 0),
            rule(2, "交通", RuleType.FIXED_AMOUNT, value = 200_00L, order = 1),
            rule(3, "储蓄", RuleType.REMAINDER, order = 2, envelopeType = EnvelopeType.SAVING),
        )
        val result = AllocationEngine.allocate(2000_00L, rules)
        assertEquals(3, result.items.size)
        assertEquals(900_00L, result.items[0].amount)
        assertEquals(200_00L, result.items[1].amount)
        assertEquals(900_00L, result.items[2].amount)
        assertEquals(2000_00L, result.totalAllocated)
        assertEquals(0, result.unallocated)
    }

    @Test
    fun `收入不足时按顺序截断 不出现负数`() {
        val rules = listOf(
            rule(1, "饮食", RuleType.FIXED_AMOUNT, value = 900_00L, order = 0),
            rule(2, "交通", RuleType.FIXED_AMOUNT, value = 200_00L, order = 1),
            rule(3, "储备", RuleType.REMAINDER, order = 2),
        )
        val result = AllocationEngine.allocate(1000_00L, rules)
        // 剩余为 0 时，全部剩余规则不再产生信封
        assertEquals(2, result.items.size)
        assertEquals(900_00L, result.items[0].amount)
        assertEquals(100_00L, result.items[1].amount)
        assertEquals(0, result.unallocated)
        assertEquals(1000_00L, result.totalAllocated)
    }

    @Test
    fun `百分比规则按总收入计算`() {
        val rules = listOf(
            rule(1, "饮食", RuleType.INCOME_PERCENTAGE, percent = 5000, order = 0),
            rule(2, "储备", RuleType.REMAINDER, order = 1),
        )
        val result = AllocationEngine.allocate(2000_00L, rules)
        assertEquals(1000_00L, result.items[0].amount)
        assertEquals(1000_00L, result.items[1].amount)
    }

    @Test
    fun `剩余比例规则按扣除后的剩余计算`() {
        val rules = listOf(
            rule(1, "固定项", RuleType.FIXED_AMOUNT, value = 1000_00L, order = 0),
            rule(2, "弹性项", RuleType.REMAINING_PERCENTAGE, percent = 5000, order = 1),
            rule(3, "储备", RuleType.REMAINDER, order = 2),
        )
        val result = AllocationEngine.allocate(2000_00L, rules)
        assertEquals(1000_00L, result.items[0].amount)
        assertEquals(500_00L, result.items[1].amount)
        assertEquals(500_00L, result.items[2].amount)
    }

    @Test
    fun `停用规则不参与分配`() {
        val rules = listOf(
            rule(1, "饮食", RuleType.FIXED_AMOUNT, value = 900_00L, order = 0),
            rule(2, "停用项", RuleType.FIXED_AMOUNT, value = 500_00L, order = 1).copy(enabled = false),
            rule(3, "储备", RuleType.REMAINDER, order = 2),
        )
        val result = AllocationEngine.allocate(2000_00L, rules)
        assertEquals(2, result.items.size)
        assertEquals(1100_00L, result.items[1].amount)
    }
}
