package com.starledger.app.core.ledger

import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.cycle.CycleService
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.CycleMode
import com.starledger.app.core.model.Transaction
import com.starledger.app.core.model.TxType
import javax.inject.Inject
import javax.inject.Singleton

/** 记账失败：滚动薪资模式下无运行周期 */
class NoRunningCycleException : IllegalStateException("No running cycle")

/**
 * 记账：新增交易并绑定所属周期，然后刷新该周期信封实际支出。
 * 日历月模式：按日期 getOrCreate 周期；
 * 滚动薪资模式：消费必须归属运行中的周期，收入归属运行周期（主薪资周期切换由调用方处理）。
 */
@Singleton
class AddTransactionUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val allocationRepository: AllocationRepository,
    private val cycleService: CycleService,
    private val settingsStore: SettingsStore,
) {
    suspend operator fun invoke(
        transaction: Transaction,
        targetCycleId: Long? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val cycle = resolveCycle(transaction, targetCycleId)
        val id = transactionDao.insert(
            transaction.copy(cycleId = cycle.id, createdAt = now, updatedAt = now)
        )
        allocationRepository.refreshEnvelopeActuals(cycle.id)
        allocationRepository.refreshCycleIncome(cycle.id)
        return id
    }

    private suspend fun resolveCycle(
        transaction: Transaction,
        targetCycleId: Long?,
    ): BudgetCycle {
        targetCycleId?.let { return allocationRepository.getCycle(it) ?: fallback(transaction) }
        val settings = settingsStore.current()
        if (settings.cycleMode == CycleMode.CALENDAR_MONTH) {
            return allocationRepository.getOrCreateCycleForDate(transaction.date)
        }
        // 滚动薪资模式
        val running = cycleService.getRunningCycle()
        if (running == null) {
            // 收入可自动开启周期；消费必须要求先开启周期
            if (transaction.type == TxType.INCOME) {
                return cycleService.startRollingCycle(transaction.date, settings.maxRunDays)
            }
            throw NoRunningCycleException()
        }
        return running
    }

    private suspend fun fallback(transaction: Transaction): BudgetCycle =
        allocationRepository.getOrCreateCycleForDate(transaction.date)
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
