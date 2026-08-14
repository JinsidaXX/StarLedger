package com.starledger.app.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.ledger.AddTransactionUseCase
import com.starledger.app.core.ledger.DeleteTransactionUseCase
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.ledger.UpdateTransactionUseCase
import com.starledger.app.core.model.Account
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.Transaction
import com.starledger.app.core.model.TxType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionFormState(
    val loading: Boolean = true,
    val type: TxType = TxType.EXPENSE,
    val amountText: String = "",
    val amountCents: Long = 0,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val merchant: String = "",
    val note: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val canSave: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TransactionEditViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionFormState())
    val uiState: StateFlow<TransactionFormState> = _state

    private var existing: Transaction? = null
    private var initialized = false

    fun load(transactionId: Long?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val accounts = ledgerRepository.getAccounts()
            val categories = ledgerRepository.getCategories()
            if (transactionId == null) {
                val settings = settingsStore.current()
                val firstAccount = accounts.firstOrNull()
                val expenseCats = categories.filter { it.isExpense }
                val accountId = settings.lastAccountId.takeIf { id -> accounts.any { it.id == id } }
                    ?: firstAccount?.id
                val categoryId = settings.lastCategoryId.takeIf { id -> expenseCats.any { it.id == id } }
                    ?: expenseCats.firstOrNull()?.id
                _state.update {
                    it.copy(
                        loading = false,
                        accounts = accounts,
                        categories = categories,
                        accountId = accountId,
                        categoryId = categoryId,
                    )
                }
            } else {
                val tx = ledgerRepository.getTransaction(transactionId)
                if (tx == null) {
                    _state.update { it.copy(loading = false, error = "账目不存在") }
                    return@launch
                }
                existing = tx
                _state.update {
                    it.copy(
                        loading = false,
                        accounts = accounts,
                        categories = categories,
                        type = tx.type,
                        amountText = Money.format(tx.amount),
                        categoryId = tx.categoryId,
                        accountId = tx.accountId,
                        toAccountId = tx.toAccountId,
                        date = tx.date,
                        merchant = tx.merchant,
                        note = tx.note,
                    )
                }
            }
        }
    }

    fun setType(type: TxType) {
        _state.update { current ->
            current.copy(
                type = type,
                categoryId = null,
                toAccountId = if (type == TxType.TRANSFER) current.toAccountId else null,
                canSave = recomputeCanSave(current.copy(type = type, categoryId = null)),
            )
        }
    }

    fun setAmountText(text: String) {
        val filtered = text.filter { it.isDigit() || it == '.' }
        val parts = filtered.split('.')
        val cleaned = if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else filtered
        val limited = if (cleaned.contains('.')) {
            val (intPart, decPart) = cleaned.split('.', limit = 2)
            "$intPart.${decPart.take(2)}"
        } else cleaned.take(9)
        _state.update {
            val cents = Money.parseYuan(limited) ?: 0
            it.copy(amountText = limited, amountCents = cents, canSave = recomputeCanSave(it.copy(amountText = limited, amountCents = cents)))
        }
    }

    fun setCategory(id: Long?) {
        _state.update { it.copy(categoryId = id, canSave = recomputeCanSave(it.copy(categoryId = id))) }
    }

    fun setAccount(id: Long?) {
        _state.update { it.copy(accountId = id, canSave = recomputeCanSave(it.copy(accountId = id))) }
    }

    fun setToAccount(id: Long?) {
        _state.update { it.copy(toAccountId = id, canSave = recomputeCanSave(it.copy(toAccountId = id))) }
    }

    fun setDate(millis: Long) {
        _state.update { it.copy(date = millis) }
    }

    fun setMerchant(text: String) {
        _state.update { it.copy(merchant = text) }
    }

    fun setNote(text: String) {
        _state.update { it.copy(note = text) }
    }

    private fun recomputeCanSave(s: TransactionFormState): Boolean = when (s.type) {
        TxType.TRANSFER -> s.amountCents > 0 && s.accountId != null && s.toAccountId != null && s.accountId != s.toAccountId
        else -> s.amountCents > 0 && s.accountId != null && s.categoryId != null
    }

    fun save() {
        val state = _state.value
        if (!state.canSave) return
        viewModelScope.launch {
            try {
                val original = existing
                val tx = (original ?: Transaction(type = state.type, amount = 0, accountId = 0))
                    .copy(
                        type = state.type,
                        amount = state.amountCents,
                        accountId = state.accountId!!,
                        toAccountId = state.toAccountId,
                        categoryId = state.categoryId,
                        date = state.date,
                        merchant = state.merchant.trim(),
                        note = state.note.trim(),
                    )
                if (original == null) {
                    addTransactionUseCase(tx)
                } else {
                    updateTransactionUseCase(tx)
                }
                settingsStore.setLastAccountId(state.accountId!!)
                state.categoryId?.let { settingsStore.setLastCategoryId(it) }
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "保存失败：${e.message}") }
            }
        }
    }

    fun delete() {
        val tx = existing ?: return
        viewModelScope.launch {
            deleteTransactionUseCase(tx)
            _state.update { it.copy(deleted = true) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
