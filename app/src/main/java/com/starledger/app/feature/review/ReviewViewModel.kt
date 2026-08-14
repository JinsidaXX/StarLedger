package com.starledger.app.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.allocation.SurplusMode
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.starmap.StarRepository
import com.starledger.app.core.starmap.StarVisual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val loading: Boolean = true,
    val cycle: BudgetCycle? = null,
    val envelopes: List<BudgetEnvelope> = emptyList(),
    val income: Long = 0,
    val expense: Long = 0,
    val surplus: Long = 0,
    val visual: StarVisual = StarVisual(),
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val allocationRepository: AllocationRepository,
    private val ledgerRepository: LedgerRepository,
    private val starRepository: StarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _state

    private var cycleId: Long = 0

    fun load(id: Long) {
        if (cycleId == id && !_state.value.loading) return
        cycleId = id
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        val cycle = allocationRepository.getCycle(cycleId) ?: run {
            _state.update { it.copy(loading = false) }
            return
        }
        val envelopes = allocationRepository.getEnvelopes(cycleId)
        val expense = ledgerRepository.sumExpense(cycle.startDate, cycle.endDate)
        val income = cycle.totalIncome + ledgerRepository.sumIncome(cycle.startDate, cycle.endDate)
        val computed = starRepository.computeStar(cycle)
        _state.update {
            it.copy(
                loading = false,
                cycle = cycle,
                envelopes = envelopes,
                income = income,
                expense = expense,
                surplus = (cycle.totalIncome - expense),
                visual = StarVisual.from(computed),
            )
        }
    }

    fun completeReview() {
        val cycle = _state.value.cycle ?: return
        viewModelScope.launch {
            allocationRepository.updateCycle(cycle.copy(reviewCompleted = true))
            refresh()
        }
    }

    fun handleSurplus(mode: SurplusMode) {
        val cycle = _state.value.cycle ?: return
        viewModelScope.launch {
            allocationRepository.handleSurplus(cycle, mode)
            refresh()
        }
    }
}
