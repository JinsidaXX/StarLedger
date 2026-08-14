package com.starledger.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.budget.BudgetCalculator
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.ledger.TransactionWithDetails
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.PlannedPurchase
import com.starledger.app.core.model.TimeUtil
import com.starledger.app.core.model.TxType
import com.starledger.app.core.starmap.StarEngine
import com.starledger.app.core.starmap.StarVisual
import com.starledger.app.feature.planning.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val cycle: BudgetCycle? = null,
    val envelopes: List<BudgetEnvelope> = emptyList(),
    val safeToSpend: Long = 0,
    val income: Long = 0,
    val expense: Long = 0,
    val allocated: Long = 0,
    val daysLeft: Int = 0,
    val showDailyAmount: Boolean = false,
    val dailyReference: Long = 0,
    val recent: List<TransactionWithDetails> = emptyList(),
    val activePlans: List<PlannedPurchase> = emptyList(),
    val totalAssets: Long = 0,
    val starVisual: StarVisual = StarVisual(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val allocationRepository: AllocationRepository,
    private val ledgerRepository: LedgerRepository,
    private val planRepository: PlanRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        allocationRepository.observeCycles(),
        planRepository.observeActivePlans(),
        settingsStore.settings,
    ) { cycles, plans, settings ->
        val current = pickCurrentCycle(cycles)
        Triple(current, plans, settings)
    }.flatMapLatest { (cycle, plans, settings) ->
        if (cycle == null) {
            flowOf(HomeUiState(activePlans = plans, showDailyAmount = settings.showDailyAmount))
        } else {
            combine(
                allocationRepository.observeEnvelopes(cycle.id),
                ledgerRepository.observeTransactionsBetween(cycle.startDate, cycle.endDate),
                ledgerRepository.observeAccountsWithBalance(),
                ledgerRepository.observeCategories(),
            ) { envelopes, txs, accountsWithBalance, categories ->
                val accounts = accountsWithBalance.map { it.account }
                val totalAssets = accountsWithBalance
                    .filter { it.account.includeInTotal && !it.account.isCredit }
                    .sumOf { it.balance }
                val expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                val income = txs.filter {
                    it.type == TxType.INCOME || it.type == TxType.REFUND || it.type == TxType.REIMBURSEMENT
                }.sumOf { it.amount }
                val activeDays = txs.map { it.date / 86_400_000L }.distinct().size
                val activePlanCount = plans.count { it.createdAt in cycle.startDate..cycle.endDate }
                val computed = StarEngine.compute(
                    cycle = cycle,
                    envelopes = envelopes,
                    expense = expense,
                    activePlanCount = activePlanCount,
                    activeDays = activeDays,
                    hasTransactions = activeDays > 0,
                )
                val safe = BudgetCalculator.safeToSpend(envelopes)
                val daysLeft = (TimeUtil.daysFromToday(cycle.endDate) + 1).toInt().coerceAtLeast(0)
                HomeUiState(
                    cycle = cycle,
                    envelopes = envelopes,
                    safeToSpend = safe,
                    income = income,
                    expense = expense,
                    allocated = cycle.totalAllocated,
                    daysLeft = daysLeft,
                    showDailyAmount = settings.showDailyAmount,
                    dailyReference = BudgetCalculator.dailyReference(safe, daysLeft),
                    recent = ledgerRepository.withDetails(
                        txs.filter { tx ->
                            val today = TimeUtil.todayMillis()
                            tx.date >= today && tx.date < today + 86_400_000L
                        },
                        accounts,
                        categories,
                    ),
                    activePlans = plans,
                    totalAssets = totalAssets,
                    starVisual = StarVisual.from(computed),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun pickCurrentCycle(cycles: List<BudgetCycle>): BudgetCycle? {
        val now = System.currentTimeMillis()
        return cycles.firstOrNull { it.status == CycleStatus.ACTIVE && it.endDate >= now }
            ?: cycles.maxByOrNull { it.endDate }
    }
}
