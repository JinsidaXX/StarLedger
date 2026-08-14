package com.starledger.app.core.ledger

import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.Transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 记账：新增交易并绑定所属周期，然后刷新该周期信封实际支出。
 * 转账不重复计算：余额变化由 DAO 的 balanceDelta 统一处理。
 */
@Singleton
class AddTransactionUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val allocationRepository: AllocationRepository,
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        val now = System.currentTimeMillis()
        val cycle = allocationRepository.getOrCreateCycleForDate(transaction.date)
        val id = transactionDao.insert(
            transaction.copy(cycleId = cycle.id, createdAt = now, updatedAt = now)
        )
        allocationRepository.refreshEnvelopeActuals(cycle.id)
        allocationRepository.refreshCycleIncome(cycle.id)
        return id
    }
}

@Singleton
class UpdateTransactionUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val allocationRepository: AllocationRepository,
) {
    suspend operator fun invoke(transaction: Transaction) {
        transactionDao.update(transaction.copy(updatedAt = System.currentTimeMillis()))
        val cycle = transaction.cycleId?.let {
            allocationRepository.getCycle(it)
        } ?: allocationRepository.getOrCreateCycleForDate(transaction.date)
        allocationRepository.refreshEnvelopeActuals(cycle.id)
        allocationRepository.refreshCycleIncome(cycle.id)
    }
}

@Singleton
class DeleteTransactionUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val allocationRepository: AllocationRepository,
) {
    /** 删除交易，返回其原所属周期（用于刷新） */
    suspend operator fun invoke(transaction: Transaction) {
        transactionDao.deleteById(transaction.id)
        transaction.cycleId?.let {
            allocationRepository.refreshEnvelopeActuals(it)
            allocationRepository.refreshCycleIncome(it)
        }
    }
}
