package com.starledger.app.core.ledger

import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.database.SeedData
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.database.dao.AccountDao
import com.starledger.app.core.database.dao.CategoryDao
import com.starledger.app.core.database.dao.CycleDao
import com.starledger.app.core.database.dao.EnvelopeDao
import com.starledger.app.core.database.dao.RuleDao
import com.starledger.app.core.database.dao.TemplateDao
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** 首次启动：写入默认账户、分类与分配模板（幂等，名称根据语言） */
@Singleton
class SeedDefaultsUseCase @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val templateDao: TemplateDao,
    private val ruleDao: RuleDao,
    private val cycleDao: CycleDao,
    private val envelopeDao: EnvelopeDao,
    private val settingsStore: SettingsStore,
    private val allocationRepository: AllocationRepository,
) {
    suspend operator fun invoke() {
        val zh = when (settingsStore.getLanguageSync()) {
            "zh" -> true
            "en" -> false
            else -> Locale.getDefault().language == "zh"
        }
        if (accountDao.count() == 0) {
            SeedData.defaultAccounts(zh).forEach { accountDao.insert(it) }
        }
        if (categoryDao.count() == 0) {
            SeedData.defaultCategories(zh).forEach { categoryDao.insert(it) }
        }
        if (templateDao.count() == 0) {
            val template = SeedData.defaultTemplate(zh)
            val templateId = templateDao.insert(template)
            val categories = categoryDao.getAll()
            SeedData.defaultRules(templateId, categories, zh).forEach { ruleDao.insert(it) }
        }
        // 语言切换后，把内置默认项名称同步为当前语言（用户自定义名称不受影响）
        syncLocalizedNames(zh)
        // 保证当前周期存在，首页直接可用
        allocationRepository.getOrCreateCurrentCycle()
    }

    private suspend fun syncLocalizedNames(zh: Boolean) {
        accountDao.getAll().forEach { acc ->
            SeedData.localizedName(acc.name, zh)?.let { accountDao.update(acc.copy(name = it)) }
        }
        categoryDao.getAll().forEach { cat ->
            SeedData.localizedName(cat.name, zh)?.let { categoryDao.update(cat.copy(name = it)) }
        }
        templateDao.getAll().forEach { tpl ->
            SeedData.localizedName(tpl.name, zh)?.let { templateDao.update(tpl.copy(name = it)) }
            ruleDao.getByTemplate(tpl.id).forEach { rule ->
                SeedData.localizedName(rule.name, zh)?.let { ruleDao.update(rule.copy(name = it)) }
            }
        }
        cycleDao.getAll().forEach { cycle ->
            val name = if (zh) "${cycle.year}年${cycle.month}月"
            else "${YearMonth.of(cycle.year, cycle.month).month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${cycle.year}"
            if (cycle.name != name) cycleDao.update(cycle.copy(name = name))
        }
        envelopeDao.getAllCycles().forEach { cycleId ->
            envelopeDao.getByCycle(cycleId).forEach { env ->
                SeedData.localizedName(env.name, zh)?.let { envelopeDao.update(env.copy(name = it)) }
            }
        }
    }
}
