package com.starledger.app.core.cycle

import com.starledger.app.core.database.dao.CycleDao
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.CycleCloseReason
import com.starledger.app.core.model.CycleMode
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.IncomeType
import com.starledger.app.core.model.TimeUtil
import com.starledger.app.core.saving.ForcedSavingParams
import javax.inject.Inject
import javax.inject.Singleton

/** 周期结算结果 */
data class CycleSettlement(
    val cycleId: Long,
    val surplus: Long,
    val closeReason: CycleCloseReason,
)

/**
 * 财务周期服务：管理滚动薪资周期与日历月周期。
 *
 * 滚动薪资周期业务规则：
 * - 生命周期由用户行为驱动，不由日期自动闭合；
 * - 同一时间仅允许一个 running 周期；
 * - 仅录入【主薪资】触发新周期判定；红包转账/兼职/其他收入不打断周期；
 * - 手动结束结算后进入无运行周期状态，消费需先开启周期。
 *
 * 纯依赖 DAO 接口（可单测），周期模式/最大运行天数由调用方传入。
 */
@Singleton
class CycleService @Inject constructor(
    private val cycleDao: CycleDao,
    private val transactionDao: TransactionDao,
) {

    /** 当前唯一运行中的周期；不存在返回 null */
    suspend fun getRunningCycle(): BudgetCycle? = cycleDao.getRunning()

    /** 已运行天数（不足一天按 0 计） */
    fun runningDays(cycle: BudgetCycle, now: Long = System.currentTimeMillis()): Long {
        val start = cycle.startDate
        if (now <= start) return 0
        return (now - start) / 86_400_000L
    }

    /** 是否超过配置的最大运行天数（仅告警，不自动关闭） */
    fun isOverMaxRunDays(cycle: BudgetCycle, now: Long = System.currentTimeMillis()): Boolean =
        runningDays(cycle, now) >= cycle.maxRunDays

    /** 结算结余：本期收入 - 本期支出，非负 */
    fun surplusOf(cycle: BudgetCycle, expense: Long): Long =
        (cycle.totalIncome - expense).coerceAtLeast(0)

    /**
     * 结算周期：关闭运行中的周期并记录关闭类型，返回结算信息。
     * 结余后续流向（心愿存钱罐）在资金池版本接入。
     */
    suspend fun settle(cycle: BudgetCycle, reason: CycleCloseReason): CycleSettlement {
        val expense = transactionDao.sumExpense(cycle.startDate, cycle.endDate)
        val surplus = surplusOf(cycle, expense)
        val now = System.currentTimeMillis()
        cycleDao.update(
            cycle.copy(
                status = CycleStatus.CLOSED,
                closeReason = reason,
                surplusHandled = true,
                endDate = now,
                updatedAt = now,
            )
        )
        return CycleSettlement(cycleId = cycle.id, surplus = surplus, closeReason = reason)
    }

    /**
     * 开启新的滚动薪资周期。
     * 该笔主薪资作为新周期第一笔收入，新周期 startDate 取薪资交易日期。
     * 默认周期结束日期为「下个月同一天」（月末自动回退，如 1/31 → 2/28）。
     * 周期不随日期自动闭合，运行超过 maxRunDays 仅告警，不强制关闭。
     * 预发工资场景：规划消费预算可延迟到 [effectStartTime] 解锁，null 表示立即生效。
     * [forcedSaving] 随主薪资下发设置的强制存储参数。
     */
    suspend fun startRollingCycle(
        salaryDate: Long,
        maxRunDays: Int,
        effectStartTime: Long? = null,
        forcedSaving: ForcedSavingParams = ForcedSavingParams(),
    ): BudgetCycle {
        // 下个月同一天的结束时刻（含当天）
        val endDate = TimeUtil.toEpochMillis(
            TimeUtil.toLocalDate(salaryDate).plusMonths(1).plusDays(1)
        ) - 1
        val cycle = BudgetCycle(
            name = TimeUtil.formatMonth(salaryDate),
            year = TimeUtil.yearMonthOf(salaryDate).year,
            month = TimeUtil.yearMonthOf(salaryDate).monthValue,
            startDate = salaryDate,
            endDate = endDate,
            status = CycleStatus.ACTIVE,
            cycleMode = CycleMode.ROLLING_SALARY,
            maxRunDays = maxRunDays,
            effectStartTime = effectStartTime,
            forcedSavingType = forcedSaving.type,
            forcedSavingValue = forcedSaving.value,
            forcedSavingAmount = forcedSaving.amount(salaryDate),
        )
        val id = cycleDao.insert(cycle)
        return cycle.copy(id = id)
    }

    /** 手动结束并结算当前运行周期；无运行周期返回 null */
    suspend fun manuallySettleCurrent(): CycleSettlement? {
        val running = getRunningCycle() ?: return null
        return settle(running, CycleCloseReason.MANUAL)
    }

    /**
     * 判定某笔收入是否应触发「结束旧周期、开启新周期」的确认：
     * 仅滚动薪资模式下，且类型为主薪资，且当前存在运行周期时触发。
     */
    suspend fun shouldConfirmPrimarySalary(
        incomeType: IncomeType?,
        mode: CycleMode,
    ): Boolean {
        if (incomeType != IncomeType.PRIMARY_SALARY) return false
        if (mode != CycleMode.ROLLING_SALARY) return false
        return getRunningCycle() != null
    }

    /** 滚动薪资模式下，消费是否允许录入（必须存在运行周期） */
    suspend fun canRecordExpense(mode: CycleMode): Boolean {
        if (mode == CycleMode.CALENDAR_MONTH) return true
        return getRunningCycle() != null
    }

    /** 确认主薪资：结算旧周期并开启新周期，返回新周期 */
    suspend fun confirmPrimarySalary(
        salaryDate: Long,
        maxRunDays: Int,
        effectStartTime: Long? = null,
        forcedSaving: ForcedSavingParams = ForcedSavingParams(),
    ): CycleSettlement? {
        val old = getRunningCycle()
        val settlement = old?.let { settle(it, CycleCloseReason.CONFIRMED_SALARY) }
        startRollingCycle(salaryDate, maxRunDays, effectStartTime, forcedSaving)
        return settlement
    }
}
