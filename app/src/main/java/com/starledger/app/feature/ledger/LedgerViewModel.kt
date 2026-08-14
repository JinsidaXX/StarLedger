package com.starledger.app.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.ledger.AccountWithBalance
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.ledger.TransactionWithDetails
import com.starledger.app.core.model.TimeUtil
import com.starledger.app.core.model.TxType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class LedgerUiState(
    val month: YearMonth = YearMonth.now(),
    val transactions: List<TransactionWithDetails> = emptyList(),
    val income: Long = 0,
    val expense: Long = 0,
    val accounts: List<AccountWithBalance> = emptyList(),
    val filterType: TxType? = null,
    val searchText: String = "",
)

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
) : ViewModel() {

    private val monthFlow = MutableStateFlow(YearMonth.now())
    private val filterFlow = MutableStateFlow<TxType?>(null)
    private val searchFlow = MutableStateFlow("")

    val uiState: StateFlow<LedgerUiState> = combine(monthFlow, filterFlow, searchFlow) { month, filter, search ->
        Triple(month, filter, search)
    }.flatMapLatest { (month, filter, search) ->
        val start = TimeUtil.monthStart(month.year, month.monthValue)
        val end = TimeUtil.monthEnd(month.year, month.monthValue)
        combine(
            ledgerRepository.observeTransactionsBetween(start, end),
            ledgerRepository.observeAccounts(),
            ledgerRepository.observeCategories(),
        ) { txs, accounts, categories ->
            val filtered = txs.filter { tx ->
                (filter == null || tx.type == filter) &&
                    (search.isBlank() ||
                        (tx.note.contains(search, ignoreCase = true) ||
                            tx.merchant.contains(search, ignoreCase = true)))
            }
            LedgerUiState(
                month = month,
                transactions = ledgerRepository.withDetails(filtered, accounts, categories),
                income = txs.filter { it.type != TxType.EXPENSE && it.type != TxType.TRANSFER }
                    .sumOf { it.amount },
                expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
                accounts = accounts.map { AccountWithBalance(it, ledgerRepository.accountBalance(it)) },
                filterType = filter,
                searchText = search,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerUiState())

    fun previousMonth() {
        monthFlow.value = monthFlow.value.minusMonths(1)
    }

    fun nextMonth() {
        monthFlow.value = monthFlow.value.plusMonths(1)
    }

    fun setFilter(type: TxType?) {
        filterFlow.value = type
    }

    fun setSearch(text: String) {
        searchFlow.value = text
    }
}
