package com.starledger.app.core.starmap

import com.starledger.app.core.database.dao.CycleDao
import com.starledger.app.core.database.dao.EnvelopeDao
import com.starledger.app.core.database.dao.PurchaseDao
import com.starledger.app.core.database.dao.StarDao
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.MonthlyStar
import com.starledger.app.core.model.PlanStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 星图仓库：由本地数据实时计算并持久化恒星快照 */
@Singleton
class StarRepository @Inject constructor(
    private val cycleDao: CycleDao,
    private val envelopeDao: EnvelopeDao,
    private val transactionDao: TransactionDao,
    private val purchaseDao: PurchaseDao,
    private val starDao: StarDao,
) {

    fun observeStarsByYear(year: Int): Flow<List<MonthlyStar>> = starDao.observeByYear(year)

    suspend fun getStarsByYear(year: Int): List<MonthlyStar> = starDao.getByYearSync(year)

    suspend fun getStarForCycle(cycleId: Long): MonthlyStar? = starDao.getByCycle(cycleId)

    /** 计算并写入某周期的恒星（存在则更新） */
    suspend fun refreshStar(cycleId: Long): MonthlyStar? {
        val cycle = cycleDao.getById(cycleId) ?: return null
        val envelopes = envelopeDao.getByCycle(cycleId)
        val expense = transactionDao.sumExpense(cycle.startDate, cycle.endDate)
        val activeDays = transactionDao.countActiveDays(cycle.startDate, cycle.endDate)
        val plans = purchaseDao.getAll()
        val activePlanCount = plans.count {
            it.createdAt in cycle.startDate..cycle.endDate &&
                it.status in listOf(PlanStatus.COOLING, PlanStatus.READY, PlanStatus.POSTPONED, PlanStatus.PURCHASED)
        }
        val computed = StarEngine.compute(
            cycle = cycle,
            envelopes = envelopes,
            expense = expense,
            activePlanCount = activePlanCount,
            activeDays = activeDays,
            hasTransactions = activeDays > 0,
        )
        val entity = StarEngine.toEntity(computed)
        val existing = starDao.getByCycle(cycleId)
        return if (existing == null) {
            starDao.insert(entity)
            entity
        } else {
            val updated = entity.copy(id = existing.id, createdAt = existing.createdAt)
            starDao.update(updated)
            updated
        }
    }

    /** 刷新所有周期的恒星 */
    suspend fun refreshAllStars() {
        cycleDao.getAll().forEach { refreshStar(it.id) }
    }

    /** 计算但不持久化（用于预览） */
    suspend fun computeStar(cycle: BudgetCycle): StarComputed {
        val envelopes = envelopeDao.getByCycle(cycle.id)
        val expense = transactionDao.sumExpense(cycle.startDate, cycle.endDate)
        val activeDays = transactionDao.countActiveDays(cycle.startDate, cycle.endDate)
        val plans = purchaseDao.getAll()
        val activePlanCount = plans.count {
            it.createdAt in cycle.startDate..cycle.endDate &&
                it.status in listOf(PlanStatus.COOLING, PlanStatus.READY, PlanStatus.POSTPONED, PlanStatus.PURCHASED)
        }
        return StarEngine.compute(
            cycle = cycle,
            envelopes = envelopes,
            expense = expense,
            activePlanCount = activePlanCount,
            activeDays = activeDays,
            hasTransactions = activeDays > 0,
        )
    }

    suspend fun getCyclesWithStars(year: Int): List<Pair<BudgetCycle?, MonthlyStar?>> {
        val cycles = cycleDao.getByYear(year)
        val stars = starDao.getByYearSync(year).associateBy { it.month }
        return (1..12).map { month ->
            val cycle = cycles.firstOrNull { it.month == month }
            val star = stars[month]
            cycle to star
        }
    }
}
