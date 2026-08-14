package com.starledger.app.core.ledger

import com.starledger.app.core.database.dao.AccountDao
import com.starledger.app.core.database.dao.CategoryDao
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.Account
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class AccountWithBalance(
    val account: Account,
    val balance: Long,
)

data class TransactionWithDetails(
    val transaction: Transaction,
    val category: Category?,
    val account: Account?,
    val toAccount: Account?,
)

@Singleton
class LedgerRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) {

    // ---------- 账户 ----------

    fun observeAccounts(): Flow<List<Account>> = accountDao.observeAll()

    suspend fun getAccounts(): List<Account> = accountDao.getAll()

    fun observeAccountsWithBalance(): Flow<List<AccountWithBalance>> = kotlinx.coroutines.flow.flow {
        accountDao.observeAll().collect { accounts ->
            emit(accounts.map { AccountWithBalance(it, accountBalance(it)) })
        }
    }

    suspend fun accountBalance(account: Account): Long =
        account.initialBalance + transactionDao.balanceDelta(account.id)

    suspend fun balanceDelta(accountId: Long): Long =
        transactionDao.balanceDelta(accountId)

    suspend fun totalAssets(accounts: List<Account>): Long = accounts
        .filter { it.includeInTotal && !it.isCredit }
        .sumOf { accountBalance(it) }

    suspend fun totalLiabilities(accounts: List<Account>): Long = accounts
        .filter { it.includeInTotal && it.isCredit }
        .sumOf { accountBalance(it) }

    suspend fun upsertAccount(account: Account): Long {
        val now = System.currentTimeMillis()
        return if (account.id == 0L) {
            accountDao.insert(account.copy(createdAt = now, updatedAt = now))
        } else {
            accountDao.update(account.copy(updatedAt = now))
            account.id
        }
    }

    suspend fun deleteAccount(account: Account): Boolean {
        val usage = transactionDao.countUsageByAccount(account.id)
        if (usage > 0) return false
        accountDao.delete(account)
        return true
    }

    // ---------- 分类 ----------

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    fun observeExpenseCategories(): Flow<List<Category>> = categoryDao.observeExpense()

    fun observeIncomeCategories(): Flow<List<Category>> = categoryDao.observeIncome()

    suspend fun getCategories(): List<Category> = categoryDao.getAll()

    suspend fun upsertCategory(category: Category): Long {
        return if (category.id == 0L) {
            categoryDao.insert(category)
        } else {
            categoryDao.update(category)
            category.id
        }
    }

    suspend fun deleteCategory(category: Category): Boolean {
        val usage = transactionDao.countUsageByCategory(category.id)
        if (usage > 0) return false
        categoryDao.delete(category)
        return true
    }

    // ---------- 交易 ----------

    fun observeTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(start, end)

    suspend fun getTransactionsBetween(start: Long, end: Long): List<Transaction> =
        transactionDao.getBetween(start, end)

    suspend fun getTransaction(id: Long): Transaction? = transactionDao.getById(id)

    suspend fun getRecent(limit: Int): List<Transaction> = transactionDao.getRecent(limit)

    suspend fun withDetails(
        transactions: List<Transaction>,
        accounts: List<Account>,
        categories: List<Category>,
    ): List<TransactionWithDetails> {
        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }
        return transactions.map { tx ->
            TransactionWithDetails(
                transaction = tx,
                category = tx.categoryId?.let { categoryMap[it] },
                account = accountMap[tx.accountId],
                toAccount = tx.toAccountId?.let { accountMap[it] },
            )
        }
    }

    suspend fun getRecentWithDetails(limit: Int): List<TransactionWithDetails> {
        val txs = transactionDao.getRecent(limit)
        val accounts = accountDao.getAll()
        val categories = categoryDao.getAll()
        return withDetails(txs, accounts, categories)
    }

    suspend fun sumExpense(start: Long, end: Long): Long =
        transactionDao.sumExpense(start, end)

    suspend fun sumIncome(start: Long, end: Long): Long =
        transactionDao.sumIncome(start, end)

    suspend fun getAllTransactions(): List<Transaction> = transactionDao.getAll()
}
