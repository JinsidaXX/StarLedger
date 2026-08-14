package com.starledger.app.core.starmap

import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.StarColorState
import com.starledger.app.core.model.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StarEngineTest {

    private fun cycle(
        income: Long = 2000_00L,
        allocated: Long = 2000_00L,
        reviewCompleted: Boolean = false,
        markedUnrecorded: Boolean = false,
    ): BudgetCycle {
        val now = System.currentTimeMillis()
        return BudgetCycle(
            id = 1, name = "2026年8月", year = 2026, month = 8,
            startDate = TimeUtil.monthStart(now), endDate = TimeUtil.monthEnd(now),
            totalIncome = income, totalAllocated = allocated,
            reviewCompleted = reviewCompleted, markedUnrecorded = markedUnrecorded,
        )
    }

    @Test
    fun `计划内支出为蓝色`() {
        val envelopes = listOf(
            BudgetEnvelope(id = 1, cycleId = 1, name = "饮食", plannedAmount = 900_00L,
                actualAmount = 800_00L, remainingAmount = 100_00L, categoryId = 1),
        )
        val result = StarEngine.compute(cycle(), envelopes, 800_00L, 0, 10, true)
        assertEquals(StarColorState.BLUE, result.colorState)
        assertTrue(result.brightness > 0.5f)
        assertEquals(1, result.rays.size)
    }

    @Test
    fun `超支10%以内不算超`() {
        val envelopes = listOf(
            BudgetEnvelope(id = 1, cycleId = 1, name = "饮食", plannedAmount = 900_00L,
                actualAmount = 920_00L, remainingAmount = -20_00L, categoryId = 1),
        )
        val result = StarEngine.compute(cycle(), envelopes, 920_00L, 0, 10, true)
        assertEquals(StarColorState.BLUE, result.colorState)
    }

    @Test
    fun `严重超支整星变红`() {
        val envelopes = listOf(
            BudgetEnvelope(id = 1, cycleId = 1, name = "饮食", plannedAmount = 900_00L,
                actualAmount = 1400_00L, remainingAmount = -500_00L, categoryId = 1),
        )
        val result = StarEngine.compute(cycle(), envelopes, 1400_00L, 0, 10, true)
        assertEquals(StarColorState.RED, result.colorState)
    }

    @Test
    fun `无记录显示星雾`() {
        val result = StarEngine.compute(cycle(), emptyList(), 0, 0, 0, false)
        assertEquals(StarColorState.FOG, result.colorState)
        assertEquals(0f, result.observationCompleteness)
    }

    @Test
    fun `标记未记录显示星雾`() {
        val result = StarEngine.compute(cycle(markedUnrecorded = true), emptyList(), 0, 0, 0, false)
        assertEquals(StarColorState.FOG, result.colorState)
        assertEquals(0f, result.observationCompleteness)
    }

    @Test
    fun `结余对亮度贡献封顶20%`() {
        val noSurplus = StarEngine.compute(
            cycle(income = 2000_00L, allocated = 2000_00L), emptyList(), 2000_00L, 0, 10, true
        )
        val bigSurplus = StarEngine.compute(
            cycle(income = 2000_00L, allocated = 2000_00L), emptyList(), 0, 0, 10, true
        )
        assertTrue(bigSurplus.brightness > noSurplus.brightness)
        // 结余超过 20% 的部分不再增加亮度
        val cap1 = StarEngine.compute(
            cycle(income = 10000_00L, allocated = 10000_00L), emptyList(), 8000_00L, 0, 10, true
        )
        val cap2 = StarEngine.compute(
            cycle(income = 10000_00L, allocated = 10000_00L), emptyList(), 0, 0, 10, true
        )
        assertEquals(cap1.brightness, cap2.brightness, 0.0001f)
    }

    @Test
    fun `快照JSON可往返`() {
        val rays = listOf(
            StarRayData(1, "饮食", 900_00L, 800_00L, 0xFFF3B95F, "NECESSARY", 0.89f),
        )
        val json = StarEngine.raysToJson(rays)
        val parsed = StarEngine.raysFromJson(json)
        assertEquals(rays, parsed)
    }

    @Test
    fun `星芒最多8条`() {
        val envelopes = (1..12).map { i ->
            BudgetEnvelope(id = i.toLong(), cycleId = 1, name = "c$i", plannedAmount = 100_00L,
                actualAmount = 0, remainingAmount = 100_00L, categoryId = i.toLong())
        }
        val result = StarEngine.compute(cycle(income = 1200_00L, allocated = 1200_00L), envelopes, 0, 0, 10, true)
        assertEquals(StarEngine.MAX_RAYS, result.rays.size)
    }
}
