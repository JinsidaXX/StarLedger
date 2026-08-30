package com.starledger.app.core.saving

import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.ForcedSavingType
import org.junit.Assert.assertEquals
import org.junit.Test

class ForcedSavingCalculatorTest {

    private fun cycle(
        type: ForcedSavingType = ForcedSavingType.NONE,
        value: Long = 0,
        amount: Long = 0,
    ) = BudgetCycle(
        name = "test", year = 2026, month = 8,
        startDate = 0, endDate = 0,
        forcedSavingType = type,
        forcedSavingValue = value,
        forcedSavingAmount = amount,
    )

    @Test
    fun `无强制存储 可用支出等于收入减支出`() {
        val r = ForcedSavingCalculator.compute(
            cycle(ForcedSavingType.NONE), income = 5000_00, nonMedicalExpense = 1200_00,
        )
        assertEquals(0L, r.forcedSavingTarget)
        assertEquals(3800_00, r.availableSpending)
        assertEquals(3800_00, r.surplus)
    }

    @Test
    fun `固定金额强制存储 正确扣减`() {
        val r = ForcedSavingCalculator.compute(
            cycle(ForcedSavingType.FIXED_AMOUNT, value = 2000_00),
            income = 5000_00, nonMedicalExpense = 1000_00,
        )
        assertEquals(2000_00, r.forcedSavingTarget)
        // 5000 - 2000 - 1000 = 2000
        assertEquals(2000_00, r.availableSpending)
        assertEquals(2000_00, r.forcedSavingActual)
        // 结余 = 强制存储 2000 + 可用 2000 = 4000
        assertEquals(4000_00, r.surplus)
    }

    @Test
    fun `百分比强制存储 按收入比例`() {
        val r = ForcedSavingCalculator.compute(
            cycle(ForcedSavingType.INCOME_PERCENTAGE, value = 2500), // 25%
            income = 5000_00, nonMedicalExpense = 0,
        )
        assertEquals(1250_00, r.forcedSavingTarget)
        assertEquals(3750_00, r.availableSpending)
    }

    @Test
    fun `超支时可用支出为负 侵蚀强制存储但不超过10%`() {
        val r = ForcedSavingCalculator.compute(
            cycle(ForcedSavingType.FIXED_AMOUNT, value = 2000_00),
            income = 5000_00, nonMedicalExpense = 5000_00, // 可用 = 5000-2000-5000 = -2000
        )
        assertEquals(2000_00, r.forcedSavingTarget)
        assertEquals(-2000_00, r.availableSpending)
        // 侵蚀上限 = 2000 * 90% = 1800，实际 = 2000 - 1800 = 200（保底 10%）
        assertEquals(200_00, r.forcedSavingActual)
        // 结余 = 200 + (-2000) = -1800
        assertEquals(-1800_00, r.surplus)
    }

    @Test
    fun `超支不超过侵蚀上限时 结余为强制存储保底`() {
        val r = ForcedSavingCalculator.compute(
            cycle(ForcedSavingType.FIXED_AMOUNT, value = 2000_00),
            income = 5000_00, nonMedicalExpense = 3100_00, // 可用 = 5000-2000-3100 = -100
        )
        // 侵蚀 = 100（未超上限 200）
        assertEquals(1900_00, r.forcedSavingActual)
        assertEquals(-100_00, r.availableSpending)
        assertEquals(1800_00, r.surplus)
    }
}
