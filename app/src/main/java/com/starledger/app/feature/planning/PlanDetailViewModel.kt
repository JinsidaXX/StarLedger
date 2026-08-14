package com.starledger.app.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.model.Account
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.PlanStatus
import com.starledger.app.core.model.PlannedPurchase
import com.starledger.app.core.model.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanDetailUiState(
    val loading: Boolean = true,
    val plan: PlannedPurchase? = null,
    val accounts: List<Account> = emptyList(),
    val envelopes: List<BudgetEnvelope> = emptyList(),
    val daysLeft: Long = 0,
    val purchased: Boolean = false,
    val closed: Boolean = false,
)

@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val ledgerRepository: LedgerRepository,
    private val allocationRepository: AllocationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanDetailUiState())
    val uiState: StateFlow<PlanDetailUiState> = _state

    private var planId: Long = 0

    fun load(id: Long) {
        if (planId == id && !_state.value.loading) return
        planId = id
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        var plan = planRepository.getPlan(planId)
        val accounts = ledgerRepository.getAccounts()
        val cycle = allocationRepository.getCurrentCycle()
        val envelopes = cycle?.let { allocationRepository.getEnvelopes(it.id) } ?: emptyList()
        val daysLeft = plan?.let { p -> TimeUtil.daysFromToday(p.earliestDecisionDate) } ?: 0
        // 冷静期结束自动进入可决定状态
        if (plan != null && plan.status == PlanStatus.COOLING && daysLeft <= 0) {
            plan = plan.copy(status = PlanStatus.READY)
            planRepository.savePlan(plan)
        }
        _state.update {
            it.copy(
                loading = false,
                plan = plan,
                accounts = accounts,
                envelopes = envelopes,
                daysLeft = daysLeft,
            )
        }
    }

    /** 放弃：删除计划并关闭页面 */
    fun cancel() {
        val plan = _state.value.plan ?: return
        viewModelScope.launch {
            planRepository.cancel(plan)
            _state.update { it.copy(closed = true) }
        }
    }

    fun buy(accountId: Long, categoryId: Long?, actualAmount: Long?) {
        val plan = _state.value.plan ?: return
        viewModelScope.launch {
            planRepository.markPurchased(plan, accountId, categoryId, actualAmount)
            // 购买后直接返回规划界面，让用户看到结果，避免重复操作
            _state.update { it.copy(purchased = true, closed = true) }
        }
    }

    /** 从计划中删除（账目保留），并关闭页面 */
    fun deletePlan() {
        val plan = _state.value.plan ?: return
        viewModelScope.launch {
            planRepository.deletePlan(plan)
            _state.update { it.copy(closed = true) }
        }
    }

    /** 转为已购物品长期管理，并关闭页面 */
    fun convertToItem() {
        val plan = _state.value.plan ?: return
        viewModelScope.launch {
            planRepository.convertToItem(plan)
            _state.update { it.copy(closed = true) }
        }
    }

    fun close() {
        _state.update { it.copy(closed = true) }
    }
}
