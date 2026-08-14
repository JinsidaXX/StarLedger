package com.starledger.app.core.budget

import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.EnvelopeType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCalculatorTest {

    private fun env(
        id: Long,
        type: EnvelopeType,
        planned: Long,
        actual: Long,
        categoryId: Long? = 1,
    ) = BudgetEnvelope(
        id = id, cycleId = 1, name = "x", plannedAmount = planned,
        actualAmount = actual, remainingAmount = planned - actual,
        type = type, categoryId = categoryId,
    )

    @Test
    fun `可放心使用不含储蓄`() {
        val envelopes = listOf(
            env(1, EnvelopeType.NECESSARY, 900, 700),
            env(2, EnvelopeType.FLEXIBLE, 250, 100),
            env(3, EnvelopeType.SAVING, 150, 0, categoryId = null),
            env(4, EnvelopeType.BUFFER, 200, 0, categoryId = null),
        )
        assertEquals(200 + 150 + 200, BudgetCalculator.safeToSpend(envelopes))
    }

    @Test
    fun `超支信封不计入可放心使用`() {
        val envelopes = listOf(
            env(1, EnvelopeType.NECESSARY, 900, 1000),
            env(2, EnvelopeType.FLEXIBLE, 250, 100),
        )
        assertEquals(150, BudgetCalculator.safeToSpend(envelopes))
    }

    @Test
    fun `无分类的必要信封不计入`() {
        val envelopes = listOf(
            env(1, EnvelopeType.NECESSARY, 900, 0, categoryId = null),
        )
        assertEquals(0, BudgetCalculator.safeToSpend(envelopes))
    }

    @Test
    fun `每日参考金额`() {
        assertEquals(34, BudgetCalculator.dailyReference(680, 20))
        assertEquals(680, BudgetCalculator.dailyReference(680, 0))
    }

    @Test
    fun `偏差文案不制造羞耻感`() {
        assertEquals("计划内", BudgetCalculator.statusText(0.5f))
        assertEquals("计划内", BudgetCalculator.statusText(1.05f))
        assertEquals("轻微超出计划", BudgetCalculator.statusText(1.15f))
        assertEquals("明显超出计划", BudgetCalculator.statusText(1.3f))
        assertEquals("严重超出计划", BudgetCalculator.statusText(1.6f))
    }
}
