package com.starledger.app.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.PlanStatus
import com.starledger.app.core.model.PlannedPurchase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanEditUiState(
    val loading: Boolean = true,
    val name: String = "",
    val amountText: String = "",
    val reason: String = "",
    val alternative: String = "",
    val coolingDays: Int = 7,
    val targetDateText: String = "",
    val note: String = "",
    val canSave: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class PlanEditViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanEditUiState())
    val uiState: StateFlow<PlanEditUiState> = _state

    private var existing: PlannedPurchase? = null
    private var initialized = false

    fun load(planId: Long?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val coolingDays = settingsStore.current().coolingDays
            if (planId != null) {
                val plan = planRepository.getPlan(planId)
                if (plan != null) {
                    existing = plan
                    _state.update {
                        it.copy(
                            loading = false,
                            name = plan.name,
                            amountText = Money.format(plan.estimatedAmount),
                            reason = plan.reason,
                            alternative = plan.alternative,
                            coolingDays = plan.coolingDays,
                            targetDateText = plan.targetDate?.let { d ->
                                com.starledger.app.core.model.TimeUtil.formatShort(d)
                            } ?: "",
                            note = plan.note,
                        )
                    }
                    return@launch
                }
            }
            _state.update { it.copy(loading = false, coolingDays = coolingDays) }
        }
    }

    fun setName(v: String) {
        _state.update { it.copy(name = v, canSave = v.isNotBlank() && Money.parseYuan(it.amountText) != null) }
    }

    fun setAmount(v: String) {
        val filtered = v.filter { it.isDigit() || it == '.' }
        _state.update {
            it.copy(amountText = filtered, canSave = it.name.isNotBlank() && Money.parseYuan(filtered) != null)
        }
    }

    fun setReason(v: String) = _state.update { it.copy(reason = v) }
    fun setAlternative(v: String) = _state.update { it.copy(alternative = v) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }
    fun setTargetDateText(v: String) = _state.update { it.copy(targetDateText = v) }

    fun setCoolingDays(days: Int) {
        _state.update { it.copy(coolingDays = days) }
    }

    fun save() {
        val s = _state.value
        val amount = Money.parseYuan(s.amountText) ?: return
        if (s.name.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val ex = existing
            val targetDate = runCatching {
                val parts = s.targetDateText.split(".")
                if (parts.size == 2) {
                    val m = parts[0].toInt()
                    val d = parts[1].toInt()
                    val year = java.time.YearMonth.now().let { ym ->
                        if (m < ym.monthValue) ym.year + 1 else ym.year
                    }
                    com.starledger.app.core.model.TimeUtil.toEpochMillis(java.time.LocalDate.of(year, m, d))
                } else null
            }.getOrNull()
            val plan = (ex ?: PlannedPurchase(
                name = s.name,
                estimatedAmount = amount,
                earliestDecisionDate = now + s.coolingDays * 86_400_000L,
            )).copy(
                name = s.name.trim(),
                estimatedAmount = amount,
                reason = s.reason.trim(),
                alternative = s.alternative.trim(),
                coolingDays = s.coolingDays,
                targetDate = targetDate,
                note = s.note.trim(),
                earliestDecisionDate = if (ex == null) now + s.coolingDays * 86_400_000L else ex.earliestDecisionDate,
            )
            planRepository.savePlan(plan)
            _state.update { it.copy(saved = true) }
        }
    }
}
