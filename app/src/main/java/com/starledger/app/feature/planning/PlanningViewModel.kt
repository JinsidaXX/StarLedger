package com.starledger.app.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.allocation.SurplusMode
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.model.AllocationTemplate
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.PlannedPurchase
import com.starledger.app.core.model.TxType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanningUiState(
    val cycle: BudgetCycle? = null,
    val envelopes: List<BudgetEnvelope> = emptyList(),
    val templates: List<AllocationTemplate> = emptyList(),
    val plans: List<PlannedPurchase> = emptyList(),
    val expense: Long = 0,
    val surplus: Long = 0,
    val cycleEnded: Boolean = false,
    /** 可分配资金 = 收入 - 强制存储目标 */
    val allocatable: Long = 0,
)

@HiltViewModel
class PlanningViewModel @Inject constructor(
    private val allocationRepository: AllocationRepository,
    private val planRepository: PlanRepository,
    private val ledgerRepository: LedgerRepository,
) : ViewModel() {

    val uiState: StateFlow<PlanningUiState> = allocationRepository.observeCycles()
        .flatMapLatest { cycles ->
            val cycle = cycles.firstOrNull { it.status == CycleStatus.ACTIVE && it.endDate >= System.currentTimeMillis() }
                ?: cycles.maxByOrNull { it.endDate }
            if (cycle == null) {
                flowOf(PlanningUiState())
            } else {
                combine(
                    allocationRepository.observeEnvelopes(cycle.id),
                    allocationRepository.observeTemplates(),
                    planRepository.observePlans(),
                    ledgerRepository.observeTransactionsBetween(cycle.startDate, cycle.endDate),
                ) { envelopes, templates, plans, txs ->
                    val expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                    PlanningUiState(
                        cycle = cycle,
                        envelopes = envelopes,
                        templates = templates,
                        plans = plans,
                        expense = expense,
                        surplus = (cycle.totalIncome - expense).coerceAtLeast(0),
                        cycleEnded = cycle.endDate < System.currentTimeMillis(),
                        allocatable = com.starledger.app.core.saving.ForcedSavingCalculator
                            .allocatableFunds(cycle, cycle.totalIncome),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanningUiState())

    fun applyTemplate(template: AllocationTemplate) {
        val state = uiState.value
        val cycle = state.cycle ?: return
        viewModelScope.launch {
            allocationRepository.applyAllocation(cycle, template, cycle.totalIncome)
        }
    }

    fun adjustEnvelope(envelope: BudgetEnvelope, newPlanned: Long) {
        viewModelScope.launch {
            allocationRepository.updateEnvelope(
                envelope.copy(
                    plannedAmount = newPlanned,
                    remainingAmount = newPlanned - envelope.actualAmount,
                )
            )
        }
    }

    fun handleSurplus(mode: SurplusMode) {
        val cycle = uiState.value.cycle ?: return
        viewModelScope.launch {
            allocationRepository.handleSurplus(cycle, mode)
        }
    }

    fun createTemplate(name: String) {
        viewModelScope.launch { allocationRepository.createTemplate(name.trim()) }
    }

    fun renameTemplate(template: AllocationTemplate, newName: String) {
        viewModelScope.launch { allocationRepository.renameTemplate(template, newName.trim()) }
    }

    fun setDefaultTemplate(template: AllocationTemplate) {
        viewModelScope.launch { allocationRepository.setDefaultTemplate(template) }
    }

    fun deleteTemplate(template: AllocationTemplate) {
        viewModelScope.launch { allocationRepository.deleteTemplate(template) }
    }
}
