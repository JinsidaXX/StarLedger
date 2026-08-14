package com.starledger.app.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val expense: List<Category> = emptyList(),
    val income: List<Category> = emptyList(),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _state

    init {
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        val all = ledgerRepository.getCategories()
        _state.update {
            it.copy(
                expense = all.filter { c -> c.isExpense },
                income = all.filter { c -> !c.isExpense },
            )
        }
    }

    suspend fun save(category: Category) {
        ledgerRepository.upsertCategory(category)
        refresh()
    }

    suspend fun delete(category: Category): Boolean {
        val ok = ledgerRepository.deleteCategory(category)
        refresh()
        return ok
    }
}
