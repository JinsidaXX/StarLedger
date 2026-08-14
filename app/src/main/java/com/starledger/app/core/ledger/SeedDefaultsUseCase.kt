package com.starledger.app.core.ledger

import com.starledger.app.core.allocation.AllocationRepository
import com.starledger.app.core.database.SeedData
import com.starledger.app.core.database.dao.AccountDao
import com.starledger.app.core.database.dao.CategoryDao
import com.starledger.app.core.database.dao.RuleDao
import com.starledger.app.core.database.dao.TemplateDao
import javax.inject.Inject
import javax.inject.Singleton

/** 首次启动：写入默认账户、分类与分配模板（幂等） */
@Singleton
class SeedDefaultsUseCase @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val templateDao: TemplateDao,
    private val ruleDao: RuleDao,
    private val allocationRepository: AllocationRepository,
) {
    suspend operator fun invoke() {
        if (accountDao.count() == 0) {
            SeedData.defaultAccounts().forEach { accountDao.insert(it) }
        }
        if (categoryDao.count() == 0) {
            SeedData.defaultCategories().forEach { categoryDao.insert(it) }
        }
        if (templateDao.count() == 0) {
            val template = SeedData.defaultTemplate()
            val templateId = templateDao.insert(template)
            val categories = categoryDao.getAll()
            SeedData.defaultRules(templateId, categories).forEach { ruleDao.insert(it) }
        }
        // 保证当前周期存在，首页直接可用
        allocationRepository.getOrCreateCurrentCycle()
    }
}
