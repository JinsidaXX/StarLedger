package com.starledger.app.core.allocation

import com.starledger.app.core.database.dao.CycleDao
import com.starledger.app.core.database.dao.EnvelopeDao
import com.starledger.app.core.database.dao.RuleDao
import com.starledger.app.core.database.dao.TemplateDao
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.AllocationTemplate
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.EnvelopeType
import com.starledger.app.core.model.TimeUtil
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

enum class SurplusMode(val label: String) {
    CARRY_TO_NEXT("结转下期"),
    TO_BUFFER("归入缓冲"),
    KEEP("保留原处"),
}

@Singleton
class AllocationRepository @Inject constructor(
    private val cycleDao: CycleDao,
    private val templateDao: TemplateDao,
    private val ruleDao: RuleDao,
    private val envelopeDao: EnvelopeDao,
    private val transactionDao: TransactionDao,
) {

    // ---------- 周期 ----------

    fun observeCycles(): Flow<List<BudgetCycle>> = cycleDao.observeAll()

    suspend fun getCycles(): List<BudgetCycle> = cycleDao.getAll()

    suspend fun getCycle(id: Long): BudgetCycle? = cycleDao.getById(id)

    suspend fun getCycleForMonth(year: Int, month: Int): BudgetCycle? =
        cycleDao.getByMonth(year, month)

    suspend fun getOrCreateCycleForDate(dateMillis: Long): BudgetCycle {
        val ym = TimeUtil.yearMonthOf(dateMillis)
        cycleDao.getByMonth(ym.year, ym.monthValue)?.let { return it }
        val cycle = BudgetCycle(
            name = TimeUtil.monthName(ym.year, ym.monthValue),
            year = ym.year,
            month = ym.monthValue,
            startDate = TimeUtil.monthStart(ym.year, ym.monthValue),
            endDate = TimeUtil.monthEnd(ym.year, ym.monthValue),
        )
        val id = cycleDao.insert(cycle)
        return cycle.copy(id = id)
    }

    suspend fun getOrCreateCurrentCycle(): BudgetCycle =
        getOrCreateCycleForDate(System.currentTimeMillis())

    suspend fun getCurrentCycle(): BudgetCycle? {
        val now = System.currentTimeMillis()
        val active = cycleDao.getActive()
        return when {
            active != null && active.endDate >= now -> active
            active != null && active.endDate < now -> {
                cycleDao.update(active.copy(status = CycleStatus.CLOSED, updatedAt = now))
                cycleDao.getActive()
            }
            else -> null
        }
    }

    suspend fun updateCycle(cycle: BudgetCycle) {
        cycleDao.update(cycle.copy(updatedAt = System.currentTimeMillis()))
    }

    // ---------- 模板与规则 ----------

    fun observeTemplates(): Flow<List<AllocationTemplate>> = templateDao.observeAll()

    suspend fun getTemplates(): List<AllocationTemplate> = templateDao.getAll()

    suspend fun getDefaultTemplate(): AllocationTemplate? = templateDao.getDefault()

    suspend fun getRules(templateId: Long): List<AllocationRule> =
        ruleDao.getByTemplate(templateId)

    fun observeRules(templateId: Long): Flow<List<AllocationRule>> =
        ruleDao.observeByTemplate(templateId)

    suspend fun upsertTemplate(template: AllocationTemplate): Long {
        return if (template.id == 0L) templateDao.insert(template) else {
            templateDao.update(template)
            template.id
        }
    }

    suspend fun upsertRule(rule: AllocationRule): Long {
        return if (rule.id == 0L) ruleDao.insert(rule) else {
            ruleDao.update(rule)
            rule.id
        }
    }

    suspend fun deleteRule(rule: AllocationRule) = ruleDao.delete(rule)

    suspend fun createTemplate(name: String): Long =
        templateDao.insert(AllocationTemplate(name = name))

    suspend fun renameTemplate(template: AllocationTemplate, newName: String) {
        templateDao.update(template.copy(name = newName))
    }

    suspend fun setDefaultTemplate(template: AllocationTemplate) {
        templateDao.clearDefault()
        templateDao.update(template.copy(isDefault = true))
    }

    /** 删除模板及其规则。只剩一个模板时不允许删除。删除默认模板后自动把剩余第一个设为默认。 */
    suspend fun deleteTemplate(template: AllocationTemplate): Boolean {
        val templates = templateDao.getAll()
        if (templates.size <= 1) return false
        ruleDao.deleteByTemplate(template.id)
        templateDao.delete(template)
        if (template.isDefault) {
            templateDao.getAll().firstOrNull()?.let {
                templateDao.update(it.copy(isDefault = true))
            }
        }
        return true
    }

    // ---------- 信封 ----------

    fun observeEnvelopes(cycleId: Long): Flow<List<BudgetEnvelope>> =
        envelopeDao.observeByCycle(cycleId)

    suspend fun getEnvelopes(cycleId: Long): List<BudgetEnvelope> =
        envelopeDao.getByCycle(cycleId)

    suspend fun updateEnvelope(envelope: BudgetEnvelope) = envelopeDao.update(envelope)

    /**
     * 按模板把本期收入分配到信封。
     * 重新分配会替换该周期已有信封。
     */
    suspend fun applyAllocation(
        cycle: BudgetCycle,
        template: AllocationTemplate,
        income: Long,
    ): AllocationPreview {
        val rules = ruleDao.getByTemplate(template.id)
        val preview = AllocationEngine.allocate(income, rules)
        envelopeDao.deleteByCycle(cycle.id)
        val envelopes = preview.items.filter { it.amount > 0 }.mapIndexed { index, item ->
            BudgetEnvelope(
                cycleId = cycle.id,
                name = item.rule.name,
                plannedAmount = item.amount,
                actualAmount = 0,
                remainingAmount = item.amount,
                type = item.rule.envelopeType,
                categoryId = item.rule.categoryId,
                color = item.rule.color,
                carryOverEnabled = item.rule.carryOver,
                sortOrder = index,
            )
        }
        envelopeDao.insertAll(envelopes)
        cycleDao.update(
            cycle.copy(
                totalIncome = income,
                totalAllocated = preview.totalAllocated,
                updatedAt = System.currentTimeMillis(),
            )
        )
        refreshEnvelopeActuals(cycle.id)
        return preview
    }

    /** 从交易统计本期收入，自动更新 cycle.totalIncome（与记账收入一致） */
    suspend fun refreshCycleIncome(cycleId: Long) {
        val cycle = cycleDao.getById(cycleId) ?: return
        val income = transactionDao.sumIncome(cycle.startDate, cycle.endDate)
        if (income != cycle.totalIncome) {
            cycleDao.update(
                cycle.copy(totalIncome = income, updatedAt = System.currentTimeMillis())
            )
        }
    }

    /** 从交易重新计算各信封实际支出与剩余。大额消费计划支出不计入预算评定。 */
    suspend fun refreshEnvelopeActuals(cycleId: Long) {
        val cycle = cycleDao.getById(cycleId) ?: return
        val envelopes = envelopeDao.getByCycle(cycleId)
        if (envelopes.isEmpty()) return
        envelopes.forEach { env ->
            val actual = env.categoryId?.let {
                transactionDao.sumExpenseByCategoryExcludingPlans(cycle.startDate, cycle.endDate, it)
            } ?: 0L
            if (actual != env.actualAmount || env.remainingAmount != env.plannedAmount - actual) {
                envelopeDao.update(
                    env.copy(actualAmount = actual, remainingAmount = env.plannedAmount - actual)
                )
            }
        }
    }

    // ---------- 结余处理 ----------

    /** 处理周期结余：结转下期 / 归入缓冲 / 保留 */
    suspend fun handleSurplus(cycle: BudgetCycle, mode: SurplusMode) {
        val expense = transactionDao.sumExpense(cycle.startDate, cycle.endDate)
        val surplus = (cycle.totalIncome - expense).coerceAtLeast(0)
        when (mode) {
            SurplusMode.CARRY_TO_NEXT -> {
                if (surplus > 0) {
                    val ym = TimeUtil.yearMonthOf(cycle.startDate).plusMonths(1)
                    val next = getOrCreateCycleForDate(TimeUtil.monthStart(ym.year, ym.monthValue))
                    val buffer = envelopeDao.getByCycle(next.id)
                        .firstOrNull { it.type == EnvelopeType.BUFFER }
                    if (buffer == null) {
                        envelopeDao.insert(
                            BudgetEnvelope(
                                cycleId = next.id,
                                name = "缓冲",
                                plannedAmount = surplus,
                                remainingAmount = surplus,
                                type = EnvelopeType.BUFFER,
                                color = 0xFF86A8FF,
                                sortOrder = 99,
                            )
                        )
                    } else {
                        envelopeDao.update(
                            buffer.copy(
                                plannedAmount = buffer.plannedAmount + surplus,
                                remainingAmount = buffer.remainingAmount + surplus,
                            )
                        )
                    }
                    val nextCycle = cycleDao.getById(next.id) ?: next
                    cycleDao.update(
                        nextCycle.copy(
                            totalAllocated = nextCycle.totalAllocated + surplus,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                }
            }
            SurplusMode.TO_BUFFER -> {
                if (surplus > 0) {
                    val buffer = envelopeDao.getByCycle(cycle.id)
                        .firstOrNull { it.type == EnvelopeType.BUFFER }
                    if (buffer == null) {
                        envelopeDao.insert(
                            BudgetEnvelope(
                                cycleId = cycle.id,
                                name = "缓冲",
                                plannedAmount = surplus,
                                remainingAmount = surplus,
                                type = EnvelopeType.BUFFER,
                                color = 0xFF86A8FF,
                                sortOrder = 99,
                            )
                        )
                    } else {
                        envelopeDao.update(
                            buffer.copy(
                                plannedAmount = buffer.plannedAmount + surplus,
                                remainingAmount = buffer.remainingAmount + surplus,
                            )
                        )
                    }
                    cycleDao.update(
                        cycle.copy(
                            totalAllocated = cycle.totalAllocated + surplus,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                }
            }
            SurplusMode.KEEP -> Unit
        }
        cycleDao.update(
            cycle.copy(surplusHandled = true, updatedAt = System.currentTimeMillis())
        )
    }
}
