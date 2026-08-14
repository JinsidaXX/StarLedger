package com.starledger.app.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.ledger.AccountWithBalance
import com.starledger.app.core.ledger.LedgerRepository
import com.starledger.app.core.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalAssets: Long = 0,
    val totalLiabilities: Long = 0,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _state

    init {
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        val accounts = ledgerRepository.getAccounts()
        _state.value = AccountsUiState(
            accounts = accounts.map { AccountWithBalance(it, ledgerRepository.accountBalance(it)) },
            totalAssets = ledgerRepository.totalAssets(accounts),
            totalLiabilities = ledgerRepository.totalLiabilities(accounts),
        )
    }

    suspend fun save(account: Account) {
        ledgerRepository.upsertAccount(account)
        refresh()
    }

    /** 删除账户；已被账目使用则失败 */
    suspend fun delete(account: Account): Boolean {
        val ok = ledgerRepository.deleteAccount(account)
        refresh()
        return ok
    }

    /** 核对余额：把账户当前余额调整为实际值，自动反算初始余额 */
    suspend fun reconcileBalance(account: Account, targetBalance: Long) {
        val delta = ledgerRepository.balanceDelta(account.id)
        ledgerRepository.upsertAccount(account.copy(initialBalance = targetBalance - delta))
        refresh()
    }
}
