package com.starledger.app.widget

import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.TxType
import com.starledger.app.core.saving.ForcedSavingCalculator
import javax.inject.Inject
import javax.inject.Singleton

/** 小组件汇总指标：收入、支出、可用支出、结余 */
data class WidgetSummary(
    val income: Long,
    val expense: Long,
    val available: Long,
    val surplus: Long,
)

/**
 * 小组件数据查询：计算当前周期（或最近周期）的四项汇总指标。
 *
 * 口径（含强制存储）：
 * - 收入 = 本期收入
 * - 支出 = 本期全部支出
 * - 可用支出 = 收入 - 强制存储 - 非医疗支出（可为负）
 * - 结余 = 强制存储实际 + 可用支出
 */
@Singleton
class WidgetDataProvider @Inject constructor(
    private val allocationRepository: AllocationRepository,
    private val transactionDao: TransactionDao,
) {

    suspend fun currentSummary(): WidgetSummary {
        val cycle = pickCurrentCycle()
            ?: return WidgetSummary(0, 0, 0, 0)
        val txs = transactionDao.getBetween(cycle.startDate, cycle.endDate)
        val income = txs.filter {
            it.type == TxType.INCOME || it.type == TxType.REFUND || it.type == TxType.REIMBURSEMENT
        }.sumOf { it.amount }
        val expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
        val nonMedical = transactionDao.sumNonMedicalExpense(cycle.startDate, cycle.endDate)
        val result = ForcedSavingCalculator.compute(cycle, income, nonMedical)
        return WidgetSummary(
            income = income,
            expense = expense,
            available = result.availableSpending,
            surplus = result.surplus,
        )
    }

    private suspend fun pickCurrentCycle(): BudgetCycle? {
        val cycles = allocationRepository.getCycles()
        val now = System.currentTimeMillis()
        return cycles.firstOrNull { it.status == CycleStatus.ACTIVE && it.endDate >= now }
            ?: cycles.maxByOrNull { it.endDate }
    }
}
