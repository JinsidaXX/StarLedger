package com.starledger.app.feature.planning

import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.database.dao.OwnedItemDao
import com.starledger.app.core.database.dao.PurchaseDao
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.OwnedItem
import com.starledger.app.core.model.PlannedPurchase
import com.starledger.app.core.model.PlanStatus
import com.starledger.app.core.model.Transaction
import com.starledger.app.core.model.TxType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val purchaseDao: PurchaseDao,
    private val transactionDao: TransactionDao,
    private val ownedItemDao: OwnedItemDao,
    private val allocationRepository: AllocationRepository,
) {

    fun observePlans(): Flow<List<PlannedPurchase>> = purchaseDao.observeAll()

    fun observeActivePlans(): Flow<List<PlannedPurchase>> = purchaseDao.observeActive()

    suspend fun getPlans(): List<PlannedPurchase> = purchaseDao.getAll()

    suspend fun getPlan(id: Long): PlannedPurchase? = purchaseDao.getById(id)

    suspend fun savePlan(plan: PlannedPurchase): Long {
        return if (plan.id == 0L) purchaseDao.insert(plan) else {
            purchaseDao.update(plan)
            plan.id
        }
    }

    suspend fun deletePlan(plan: PlannedPurchase) = purchaseDao.deleteById(plan.id)

    /** 放弃：直接删除计划，不再显示 */
    suspend fun cancel(plan: PlannedPurchase) {
        purchaseDao.deleteById(plan.id)
    }

    /**
     * 购买：创建一笔关联支出，计划状态变为已购买。返回新交易的 id。
     * 实际金额可不同于预估金额。
     */
    suspend fun markPurchased(
        plan: PlannedPurchase,
        accountId: Long,
        categoryId: Long?,
        actualAmount: Long? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val cycle = allocationRepository.getOrCreateCycleForDate(now)
        val amount = actualAmount ?: plan.estimatedAmount
        val tx = Transaction(
            type = TxType.EXPENSE,
            amount = amount,
            accountId = accountId,
            categoryId = categoryId,
            date = now,
            merchant = plan.name,
            note = "大额消费计划",
            relatedPlanId = plan.id,
            cycleId = cycle.id,
        )
        val txId = transactionDao.insert(tx)
        purchaseDao.update(
            plan.copy(
                status = PlanStatus.PURCHASED,
                purchasedTransactionId = txId,
                estimatedAmount = amount,
            )
        )
        allocationRepository.refreshEnvelopeActuals(cycle.id)
        return txId
    }

    /** 转为已购物品（长期管理），并删除原计划。返回新物品 id。 */
    suspend fun convertToItem(plan: PlannedPurchase): Long {
        val itemId = ownedItemDao.insert(
            OwnedItem(
                name = plan.name,
                purchasePrice = plan.estimatedAmount,
                purchaseDate = System.currentTimeMillis(),
                note = plan.note,
                planId = plan.id,
                transactionId = plan.purchasedTransactionId,
            )
        )
        purchaseDao.deleteById(plan.id)
        return itemId
    }
}
