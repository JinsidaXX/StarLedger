package com.starledger.app.feature.allocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.AllocationTemplate
import com.starledger.app.core.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateEditUiState(
    val loading: Boolean = true,
    val template: AllocationTemplate? = null,
    val rules: List<AllocationRule> = emptyList(),
    val categories: List<Category> = emptyList(),
)

@HiltViewModel
class TemplateEditViewModel @Inject constructor(
    private val allocationRepository: AllocationRepository,
    private val ledgerRepository: LedgerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TemplateEditUiState())
    val uiState: StateFlow<TemplateEditUiState> = _state

    private var templateId: Long = 0

    fun load(id: Long) {
        if (templateId == id && !_state.value.loading) return
        templateId = id
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        val template = allocationRepository.getTemplates().firstOrNull { it.id == templateId }
        val rules = template?.let { allocationRepository.getRules(it.id) } ?: emptyList()
        val categories = ledgerRepository.getCategories().filter { it.isExpense }
        _state.update {
            it.copy(
                loading = false,
                template = template,
                rules = rules,
                categories = categories,
            )
        }
    }

    suspend fun saveRule(rule: AllocationRule) {
        allocationRepository.upsertRule(rule)
        refresh()
    }

    suspend fun deleteRule(rule: AllocationRule) {
        allocationRepository.deleteRule(rule)
        refresh()
    }

    suspend fun renameTemplate(newName: String) {
        val template = _state.value.template ?: return
        allocationRepository.renameTemplate(template, newName.trim())
        refresh()
    }

    suspend fun deleteTemplate(): Boolean {
        val template = _state.value.template ?: return false
        return allocationRepository.deleteTemplate(template)
    }
}
