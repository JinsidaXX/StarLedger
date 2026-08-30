package com.starledger.app.core.cycle

import com.starledger.app.core.database.dao.CycleDao
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.CycleCloseReason
import com.starledger.app.core.model.CycleMode
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.IncomeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 可注入的 CycleDao 假实现 */
private class FakeCycleDao : CycleDao {
    val cycles = mutableListOf<BudgetCycle>()
    var nextId = 1L

    override fun observeAll(): Flow<List<BudgetCycle>> = flowOf(cycles.toList())
    override suspend fun getAll(): List<BudgetCycle> = cycles.toList()
    override suspend fun getById(id: Long): BudgetCycle? = cycles.firstOrNull { it.id == id }
    override suspend fun getByMonth(year: Int, month: Int): BudgetCycle? =
        cycles.firstOrNull { it.year == year && it.month == month }
    override suspend fun getByYear(year: Int): List<BudgetCycle> = cycles.filter { it.year == year }
    override suspend fun getActive(): BudgetCycle? =
        cycles.firstOrNull { it.status == CycleStatus.ACTIVE }
    override suspend fun getRunning(): BudgetCycle? =
        cycles.firstOrNull { it.status == CycleStatus.ACTIVE }
    override suspend fun getActiveAll(): List<BudgetCycle> =
        cycles.filter { it.status == CycleStatus.ACTIVE }
    override suspend fun getPast(now: Long): List<BudgetCycle> =
        cycles.filter { it.endDate < now }
    override suspend fun insert(cycle: BudgetCycle): Long {
        val id = nextId++
        cycles += cycle.copy(id = id)
        return id
    }
    override suspend fun update(cycle: BudgetCycle) {
        val idx = cycles.indexOfFirst { it.id == cycle.id }
        if (idx >= 0) cycles[idx] = cycle
    }
    override suspend fun deleteById(id: Long) {
        cycles.removeAll { it.id == id }
    }
    override suspend fun count(): Int = cycles.size
}

/** 可注入的 TransactionDao 假实现 */
private class FakeTransactionDao : TransactionDao {
    var expense = 0L

    override fun observeBetween(start: Long, end: Long): Flow<List<com.starledger.app.core.model.Transaction>> =
        flowOf(emptyList())
    override suspend fun getBetween(start: Long, end: Long): List<com.starledger.app.core.model.Transaction> = emptyList()
    override suspend fun getById(id: Long): com.starledger.app.core.model.Transaction? = null
    override suspend fun getRecent(limit: Int): List<com.starledger.app.core.model.Transaction> = emptyList()
    override suspend fun insert(transaction: com.starledger.app.core.model.Transaction): Long = 0
    override suspend fun update(transaction: com.starledger.app.core.model.Transaction) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun count(): Int = 0
    override suspend fun balanceDelta(accountId: Long): Long = 0
    override suspend fun sumExpense(start: Long, end: Long): Long = expense
    override suspend fun sumIncome(start: Long, end: Long): Long = 0
    override suspend fun sumExpenseByCategory(start: Long, end: Long, categoryId: Long): Long = 0
    override suspend fun sumExpenseByCategoryExcludingPlans(start: Long, end: Long, categoryId: Long): Long = 0
    override suspend fun countUsageByCategory(categoryId: Long): Long = 0
    override suspend fun countUsageByAccount(accountId: Long): Long = 0
    override suspend fun getByPlan(planId: Long): com.starledger.app.core.model.Transaction? = null
    override suspend fun countActiveDays(start: Long, end: Long): Int = 0
    override suspend fun getAll(): List<com.starledger.app.core.model.Transaction> = emptyList()
}

class CycleServiceTest {

    private fun cycle(
        id: Long = 0,
        status: CycleStatus = CycleStatus.ACTIVE,
        mode: CycleMode = CycleMode.ROLLING_SALARY,
        startDate: Long = 1_000_000_000_000L,
        totalIncome: Long = 0,
        maxRunDays: Int = 50,
    ) = BudgetCycle(
        id = id, name = "test", year = 2026, month = 8,
        startDate = startDate, endDate = startDate + maxRunDays * 86_400_000L,
        status = status, cycleMode = mode, totalIncome = totalIncome,
        maxRunDays = maxRunDays,
    )

    private fun service(cycleDao: CycleDao, txDao: TransactionDao) =
        CycleService(cycleDao, txDao)

    @Test
    fun `运行天数 按整天计算`() {
        val cycleDao = FakeCycleDao()
        val service = service(cycleDao, FakeTransactionDao())
        val c = cycle(startDate = 1_000_000_000_000L)
        assertEquals(0L, service.runningDays(c, now = c.startDate))
        assertEquals(1L, service.runningDays(c, now = c.startDate + 86_400_000L))
        assertEquals(0L, service.runningDays(c, now = c.startDate + 86_400_000L - 1))
    }

    @Test
    fun `超过最大运行天数 判定`() {
        val service = service(FakeCycleDao(), FakeTransactionDao())
        val c = cycle(startDate = 1_000_000_000_000L, maxRunDays = 50)
        assertFalse(service.isOverMaxRunDays(c, now = c.startDate + 49 * 86_400_000L))
        assertTrue(service.isOverMaxRunDays(c, now = c.startDate + 50 * 86_400_000L))
    }

    @Test
    fun `结算结余 非负`() {
        val service = service(FakeCycleDao(), FakeTransactionDao())
        assertEquals(500L, service.surplusOf(cycle(totalIncome = 1000L), expense = 500L))
        assertEquals(0L, service.surplusOf(cycle(totalIncome = 500L), expense = 1000L))
    }

    @Test
    fun `手动结算 关闭周期并记录关闭类型`() = runTest {
        val cycleDao = FakeCycleDao()
        val txDao = FakeTransactionDao().apply { expense = 200L }
        val service = service(cycleDao, txDao)
        cycleDao.insert(cycle(totalIncome = 1000L, status = CycleStatus.ACTIVE))

        val settlement = service.manuallySettleCurrent()

        assertNotNull(settlement)
        assertEquals(800L, settlement?.surplus)
        assertEquals(CycleCloseReason.MANUAL, settlement?.closeReason)
        val updated = cycleDao.cycles.firstOrNull { it.id == settlement?.cycleId }
        assertEquals(CycleStatus.CLOSED, updated?.status)
        assertEquals(CycleCloseReason.MANUAL, updated?.closeReason)
        assertTrue(updated?.surplusHandled ?: false)
    }

    @Test
    fun `主薪资确认判定 仅滚动模式且存在运行周期`() = runTest {
        val cycleDao = FakeCycleDao()
        val service = service(cycleDao, FakeTransactionDao())

        // 无运行周期
        assertFalse(
            service.shouldConfirmPrimarySalary(IncomeType.PRIMARY_SALARY, CycleMode.ROLLING_SALARY)
        )
        // 有运行周期
        cycleDao.insert(cycle(totalIncome = 1000L))
        assertTrue(
            service.shouldConfirmPrimarySalary(IncomeType.PRIMARY_SALARY, CycleMode.ROLLING_SALARY)
        )
        // 日历月模式不触发
        assertFalse(
            service.shouldConfirmPrimarySalary(IncomeType.PRIMARY_SALARY, CycleMode.CALENDAR_MONTH)
        )
        // 非主薪资不触发
        assertFalse(
            service.shouldConfirmPrimarySalary(IncomeType.RED_PACKET, CycleMode.ROLLING_SALARY)
        )
    }

    @Test
    fun `确认主薪资 结算旧周期并开启新周期`() = runTest {
        val cycleDao = FakeCycleDao()
        val service = service(cycleDao, FakeTransactionDao())
        val old = cycleDao.insert(cycle(totalIncome = 1000L, startDate = 1_000_000_000_000L))
        val newStart = 1_100_000_000_000L

        val settlement = service.confirmPrimarySalary(newStart, maxRunDays = 50)

        assertNotNull(settlement)
        assertEquals(CycleCloseReason.CONFIRMED_SALARY, settlement?.closeReason)
        val oldUpdated = cycleDao.cycles.firstOrNull { it.id == old }
        assertEquals(CycleStatus.CLOSED, oldUpdated?.status)
        val running = cycleDao.getRunning()
        assertNotNull(running)
        assertEquals(newStart, running?.startDate)
        assertEquals(CycleMode.ROLLING_SALARY, running?.cycleMode)
    }

    @Test
    fun `滚动模式消费需运行周期`() = runTest {
        val cycleDao = FakeCycleDao()
        val service = service(cycleDao, FakeTransactionDao())
        assertFalse(service.canRecordExpense(CycleMode.ROLLING_SALARY))
        cycleDao.insert(cycle())
        assertTrue(service.canRecordExpense(CycleMode.ROLLING_SALARY))
        assertTrue(service.canRecordExpense(CycleMode.CALENDAR_MONTH))
    }

    @Test
    fun `新周期结束日期为下个月同一天`() = runTest {
        val cycleDao = FakeCycleDao()
        val service = service(cycleDao, FakeTransactionDao())
        val salaryDate = com.starledger.app.core.model.TimeUtil.toEpochMillis(
            java.time.LocalDate.of(2026, 8, 30)
        )

        val cycle = service.startRollingCycle(salaryDate, maxRunDays = 50)

        // 结束日期 = 下个月同一天（9/30）当天结束时刻
        val expectedEnd = com.starledger.app.core.model.TimeUtil.toEpochMillis(
            java.time.LocalDate.of(2026, 9, 30).plusDays(1)
        ) - 1
        assertEquals(expectedEnd, cycle.endDate)
        assertEquals(salaryDate, cycle.startDate)
        assertEquals(50, cycle.maxRunDays)
    }

    @Test
    fun `月末发薪结束日期自动回退到小月最后一天`() = runTest {
        val cycleDao = FakeCycleDao()
        val service = service(cycleDao, FakeTransactionDao())
        // 1/31 发薪 → 下个月同一天应回退到 2/28（2026 非闰年）
        val salaryDate = com.starledger.app.core.model.TimeUtil.toEpochMillis(
            java.time.LocalDate.of(2026, 1, 31)
        )

        val cycle = service.startRollingCycle(salaryDate, maxRunDays = 50)

        val expectedEnd = com.starledger.app.core.model.TimeUtil.toEpochMillis(
            java.time.LocalDate.of(2026, 2, 28).plusDays(1)
        ) - 1
        assertEquals(expectedEnd, cycle.endDate)
    }
}
