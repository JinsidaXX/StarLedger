package com.starledger.app.feature.starmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.MonthlyStar
import com.starledger.app.core.starmap.StarComputed
import com.starledger.app.core.starmap.StarRepository
import com.starledger.app.core.starmap.StarVisual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StarDetailUiState(
    val loading: Boolean = true,
    val cycle: BudgetCycle? = null,
    val star: MonthlyStar? = null,
    val envelopes: List<BudgetEnvelope> = emptyList(),
    val visual: StarVisual = StarVisual(),
    val expense: Long = 0,
    val surplus: Long = 0,
)

@HiltViewModel
class StarDetailViewModel @Inject constructor(
    private val starRepository: StarRepository,
    private val allocationRepository: AllocationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StarDetailUiState())
    val uiState: StateFlow<StarDetailUiState> = _state

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
        val star = starRepository.refreshStar(cycleId)
        val envelopes = allocationRepository.getEnvelopes(cycleId)
        val computed: StarComputed = starRepository.computeStar(cycle)
        _state.update {
            it.copy(
                loading = false,
                cycle = cycle,
                star = star,
                envelopes = envelopes,
                visual = StarVisual.from(computed),
                expense = computed.totalExpense,
                surplus = computed.surplusAmount,
            )
        }
    }

    fun markUnrecorded() {
        val cycle = _state.value.cycle ?: return
        viewModelScope.launch {
            allocationRepository.updateCycle(cycle.copy(markedUnrecorded = !cycle.markedUnrecorded))
            refresh()
        }
    }

    fun completeReview() {
        val cycle = _state.value.cycle ?: return
        viewModelScope.launch {
            allocationRepository.updateCycle(cycle.copy(reviewCompleted = !cycle.reviewCompleted))
            refresh()
        }
    }
}
