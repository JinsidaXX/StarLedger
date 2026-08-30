package com.starledger.app.core.saving

import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.ForcedSavingType

/**
 * 强制存储计算结果。
 *
 * @param forcedSavingTarget 强制存储目标金额（分）
 * @param forcedSavingActual 强制存储实际金额（分，侵蚀后，底线为目标 90%）
 * @param availableSpending 可用支出 = 收入 - 强制存储 - 非医疗支出（可为负）
 * @param surplus 结余 = 强制存储实际 + 可用支出（= 收入 - 非医疗支出 - 医疗支出）
 */
data class ForcedSavingResult(
    val forcedSavingTarget: Long,
    val forcedSavingActual: Long,
    val availableSpending: Long,
    val surplus: Long,
)

/**
 * 强制存储计算引擎（纯函数）。
 *
 * 规则：
 * - 强制存储目标由周期参数决定：固定金额 或 收入百分比；
 * - 可用支出 = 收入 - 强制存储目标 - 非医疗支出，可为负；
 * - 超支时侵蚀强制存储，但最多侵蚀到原定目标的 90%（保留 10% 兜底）；
 * - 结余 = 强制存储实际 + 可用支出。
 */
object ForcedSavingCalculator {

    private const val PERCENT_BASE = 10_000L
    private const val FLOOR_RATIO = 90L // 侵蚀底线：原定 90%

    /** 根据周期参数计算强制存储目标金额 */
    fun targetOf(cycle: BudgetCycle, income: Long): Long = when (cycle.forcedSavingType) {
        ForcedSavingType.NONE -> 0L
        ForcedSavingType.FIXED_AMOUNT -> cycle.forcedSavingValue.coerceAtLeast(0)
        ForcedSavingType.INCOME_PERCENTAGE ->
            (income * cycle.forcedSavingValue / PERCENT_BASE).coerceAtLeast(0)
    }

    /**
     * 计算四项指标。
     *
     * @param income 本周期收入
     * @param nonMedicalExpense 非医疗支出（医疗类已排除，不占可用支出额度）
     */
    fun compute(cycle: BudgetCycle, income: Long, nonMedicalExpense: Long): ForcedSavingResult {
        val target = targetOf(cycle, income)
        // 可用支出 = 收入 - 强制存储目标 - 非医疗支出（可为负）
        val available = income - target - nonMedicalExpense
        // 实际强制存储 = 目标 - 被侵蚀部分；侵蚀 = min(超支, 目标的 10%)
        val actual = if (available >= 0) {
            target
        } else {
            val erosionCap = target * FLOOR_RATIO / 100
            val erosion = (-available).coerceAtMost(erosionCap)
            target - erosion
        }
        // 结余 = 强制存储实际 + 可用支出
        val surplus = actual + available
        return ForcedSavingResult(
            forcedSavingTarget = target,
            forcedSavingActual = actual,
            availableSpending = available,
            surplus = surplus,
        )
    }
}
