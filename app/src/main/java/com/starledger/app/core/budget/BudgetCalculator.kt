package com.starledger.app.core.budget

import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.EnvelopeType

/** 预算计算：可放心使用金额等纯函数 */
object BudgetCalculator {

    /**
     * 本期可放心使用金额 =
     * 必要/弹性信封剩余（有关联分类的）+ 可用缓冲 - 尚未支付的必要支出。
     * 储蓄（SAVING）不计入。
     */
    fun safeToSpend(envelopes: List<BudgetEnvelope>): Long {
        val spendable = envelopes
            .filter {
                when (it.type) {
                    EnvelopeType.BUFFER -> true
                    EnvelopeType.NECESSARY, EnvelopeType.FLEXIBLE -> it.categoryId != null
                    else -> false
                }
            }
            .sumOf { it.remainingAmount.coerceAtLeast(0) }
        // 尚未支付的必要支出：MVP 暂无周期账单，预留为 0
        return spendable
    }

    /** 参考每日可用金额 */
    fun dailyReference(safeToSpend: Long, daysLeft: Int): Long =
        if (daysLeft <= 0) safeToSpend else safeToSpend / daysLeft

    /** 预算偏差比例：实际/计划（0 = 未花钱，1 = 刚好用完，>1 = 超支） */
    fun deviationRatio(planned: Long, actual: Long): Float {
        if (planned <= 0) return if (actual > 0) 99f else 0f
        return actual.toFloat() / planned
    }

    /** 分类预算状态文案：不制造羞耻感。10% 以内不算超。 */
    fun statusText(ratio: Float): String = when {
        ratio > 1.5 -> "严重超出计划"
        ratio > 1.2 -> "明显超出计划"
        ratio > 1.1 -> "轻微超出计划"
        else -> "计划内"
    }
}
