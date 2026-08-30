package com.starledger.app.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.cycle.CycleService
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.ledger.AddTransactionUseCase
import com.starledger.app.core.ledger.DeleteTransactionUseCase
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.ledger.NoRunningCycleException
import com.starledger.app.core.ledger.UpdateTransactionUseCase
import com.starledger.app.core.model.Account
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.IncomeType
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
    val incomeType: IncomeType? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val canSave: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
    // 主薪资确认弹窗
    val showSalaryConfirm: Boolean = false,
    val salaryRunningDays: Long = 0,
)

@HiltViewModel
class TransactionEditViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val settingsStore: SettingsStore,
    private val cycleService: CycleService,
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
                        incomeType = tx.incomeType,
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
                incomeType = if (type == TxType.INCOME) IncomeType.OTHER else null,
                canSave = recomputeCanSave(current.copy(type = type, categoryId = null)),
            )
        }
    }

    fun setIncomeType(incomeType: IncomeType) {
        _state.update { it.copy(incomeType = incomeType) }
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

    /** 保存入口：新增主薪资收入时先检查是否需确认周期切换 */
    fun save() {
        val state = _state.value
        if (!state.canSave) return
        viewModelScope.launch {
            val tx = buildTransaction(state)
            if (existing == null && tx.type == TxType.INCOME &&
                tx.incomeType == IncomeType.PRIMARY_SALARY
            ) {
                val settings = settingsStore.current()
                if (cycleService.shouldConfirmPrimarySalary(tx.incomeType, settings.cycleMode)) {
                    val running = cycleService.getRunningCycle()
                    _state.update {
                        it.copy(
                            showSalaryConfirm = true,
                            salaryRunningDays = running?.let { c -> cycleService.runningDays(c) } ?: 0,
                        )
                    }
                    return@launch
                }
            }
            doSave(tx)
        }
    }

    /** 确认主薪资：结算旧周期并开启新周期，该笔作为新周期第一笔收入 */
    fun confirmSalaryNewCycle() {
        val state = _state.value
        viewModelScope.launch {
            try {
                val settings = settingsStore.current()
                cycleService.confirmPrimarySalary(state.date, settings.maxRunDays)
                doSave(buildTransaction(state))
            } catch (e: Exception) {
                _state.update { it.copy(error = "保存失败：${e.message}", showSalaryConfirm = false) }
            }
        }
    }

    /** 主薪资作为普通收入：不打断周期 */
    fun confirmSalaryAsNormal() {
        val state = _state.value
        viewModelScope.launch {
            try {
                doSave(buildTransaction(state))
            } catch (e: Exception) {
                _state.update { it.copy(error = "保存失败：${e.message}", showSalaryConfirm = false) }
            }
        }
    }

    fun dismissSalaryConfirm() {
        _state.update { it.copy(showSalaryConfirm = false) }
    }

    private fun buildTransaction(state: TransactionFormState): Transaction {
        val original = existing
        return (original ?: Transaction(type = state.type, amount = 0, accountId = 0))
            .copy(
                type = state.type,
                amount = state.amountCents,
                accountId = state.accountId!!,
                toAccountId = state.toAccountId,
                categoryId = state.categoryId,
                date = state.date,
                merchant = state.merchant.trim(),
                note = state.note.trim(),
                incomeType = if (state.type == TxType.INCOME) state.incomeType else null,
            )
    }

    private suspend fun doSave(tx: Transaction) {
        try {
            if (existing == null) {
                addTransactionUseCase(tx)
            } else {
                updateTransactionUseCase(tx)
            }
            settingsStore.setLastAccountId(tx.accountId)
            tx.categoryId?.let { settingsStore.setLastCategoryId(it) }
            _state.update { it.copy(saved = true, showSalaryConfirm = false) }
        } catch (e: NoRunningCycleException) {
            _state.update {
                it.copy(
                    error = "当前无运行中的财务周期，请先开启周期再记录消费",
                    showSalaryConfirm = false,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = "保存失败：${e.message}", showSalaryConfirm = false) }
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
