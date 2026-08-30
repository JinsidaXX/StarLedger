package com.starledger.app.widget

import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.budget.BudgetCalculator
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.TxType
import javax.inject.Inject
import javax.inject.Singleton

/** 小组件四指标：收入、支出、可用支出、结余 */
data class WidgetSummary(
    val income: Long,
    val expense: Long,
    val available: Long,
    val surplus: Long,
)

/**
 * 小组件数据查询：计算当前周期（或最近周期）的四项汇总指标。
 * 收入/支出来自交易流水；可用支出复用 BudgetCalculator；结余 = 收入 - 支出（非负）。
 */
@Singleton
class WidgetDataProvider @Inject constructor(
    private val allocationRepository: AllocationRepository,
    private val ledgerRepository: LedgerRepository,
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
        val envelopes = allocationRepository.getEnvelopes(cycle.id)
        val available = BudgetCalculator.safeToSpend(envelopes)
        val surplus = (income - expense).coerceAtLeast(0)
        return WidgetSummary(
            income = income,
            expense = expense,
            available = available,
            surplus = surplus,
        )
    }

    private suspend fun pickCurrentCycle(): BudgetCycle? {
        val cycles = allocationRepository.getCycles()
        val now = System.currentTimeMillis()
        return cycles.firstOrNull { it.status == CycleStatus.ACTIVE && it.endDate >= now }
            ?: cycles.maxByOrNull { it.endDate }
    }
}
