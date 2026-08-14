package com.starledger.app.core.database

import com.starledger.app.core.model.Account
import com.starledger.app.core.model.AccountType
import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.AllocationTemplate
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.EnvelopeType
import com.starledger.app.core.model.RuleType

/** 首次启动的默认数据：账户、分类、默认分配模板 */
object SeedData {

    fun defaultAccounts(): List<Account> = listOf(
        Account(name = "现金", type = AccountType.CASH, color = 0xFFF3B95F, sortOrder = 0),
        Account(name = "银行卡", type = AccountType.BANK_CARD, color = 0xFF86A8FF, sortOrder = 1),
        Account(name = "微信", type = AccountType.WECHAT, color = 0xFF58D6A9, sortOrder = 2),
        Account(name = "支付宝", type = AccountType.ALIPAY, color = 0xFF5BC8E8, sortOrder = 3),
        Account(name = "校园卡", type = AccountType.CAMPUS_CARD, color = 0xFFA78BFA, sortOrder = 4),
        Account(name = "信用卡", type = AccountType.CREDIT_CARD, color = 0xFFFF6B7A, isCredit = true, sortOrder = 5),
    )

    fun defaultCategories(): List<Category> = listOf(
        Category(name = "饮食", icon = "🍜", color = 0xFFF3B95F, sortOrder = 0),
        Category(name = "交通", icon = "🚌", color = 0xFF5BC8E8, sortOrder = 1),
        Category(name = "学习", icon = "📚", color = 0xFF86A8FF, sortOrder = 2),
        Category(name = "宿舍与生活", icon = "🏠", color = 0xFF8BC98A, sortOrder = 3),
        Category(name = "通讯", icon = "📱", color = 0xFFA78BFA, sortOrder = 4),
        Category(name = "社交", icon = "🎉", color = 0xFFF08CB4, sortOrder = 5),
        Category(name = "娱乐", icon = "🎮", color = 0xFFD9A066, sortOrder = 6),
        Category(name = "购物", icon = "🛍️", color = 0xFFFF6B7A, sortOrder = 7),
        Category(name = "医疗与应急", icon = "🏥", color = 0xFF58D6A9, sortOrder = 8),
        Category(name = "其他", icon = "📦", color = 0xFFA7B0C3, sortOrder = 9),
        // 收入分类
        Category(name = "工资", icon = "💰", color = 0xFF58D6A9, isExpense = false, sortOrder = 0),
        Category(name = "兼职收入", icon = "💼", color = 0xFF86A8FF, isExpense = false, sortOrder = 1),
        Category(name = "红包", icon = "🧧", color = 0xFFFF6B7A, isExpense = false, sortOrder = 2),
        Category(name = "其他收入", icon = "📦", color = 0xFFA7B0C3, isExpense = false, sortOrder = 3),
    )

    /** 默认模板：月收入 2,000 元示例 */
    fun defaultTemplate(name: String = "默认分配模板"): AllocationTemplate =
        AllocationTemplate(name = name, isDefault = true)

    fun defaultRules(templateId: Long, categories: List<Category>): List<AllocationRule> {
        fun catId(name: String): Long? = categories.firstOrNull { it.name == name }?.id
        return listOf(
            AllocationRule(
                templateId = templateId, name = "饮食", categoryId = catId("饮食"),
                ruleType = RuleType.FIXED_AMOUNT, value = 800_00L, priority = 0,
                envelopeType = EnvelopeType.NECESSARY, color = 0xFFF3B95F, sortOrder = 0,
            ),
            AllocationRule(
                templateId = templateId, name = "交通", categoryId = catId("交通"),
                ruleType = RuleType.FIXED_AMOUNT, value = 150_00L, priority = 1,
                envelopeType = EnvelopeType.NECESSARY, color = 0xFF5BC8E8, sortOrder = 1,
            ),
            AllocationRule(
                templateId = templateId, name = "学习", categoryId = catId("学习"),
                ruleType = RuleType.FIXED_AMOUNT, value = 150_00L, priority = 2,
                envelopeType = EnvelopeType.NECESSARY, color = 0xFF86A8FF, sortOrder = 2,
            ),
            AllocationRule(
                templateId = templateId, name = "宿舍与生活", categoryId = catId("宿舍与生活"),
                ruleType = RuleType.FIXED_AMOUNT, value = 200_00L, priority = 3,
                envelopeType = EnvelopeType.NECESSARY, color = 0xFF8BC98A, sortOrder = 3,
            ),
            AllocationRule(
                templateId = templateId, name = "通讯", categoryId = catId("通讯"),
                ruleType = RuleType.FIXED_AMOUNT, value = 100_00L, priority = 4,
                envelopeType = EnvelopeType.NECESSARY, color = 0xFFA78BFA, sortOrder = 4,
            ),
            AllocationRule(
                templateId = templateId, name = "社交", categoryId = catId("社交"),
                ruleType = RuleType.FIXED_AMOUNT, value = 100_00L, priority = 5,
                envelopeType = EnvelopeType.FLEXIBLE, color = 0xFFF08CB4, sortOrder = 5,
            ),
            AllocationRule(
                templateId = templateId, name = "娱乐", categoryId = catId("娱乐"),
                ruleType = RuleType.FIXED_AMOUNT, value = 150_00L, priority = 6,
                envelopeType = EnvelopeType.FLEXIBLE, color = 0xFFD9A066, sortOrder = 6,
            ),
            AllocationRule(
                templateId = templateId, name = "购物", categoryId = catId("购物"),
                ruleType = RuleType.FIXED_AMOUNT, value = 150_00L, priority = 7,
                envelopeType = EnvelopeType.FLEXIBLE, color = 0xFFFF6B7A, sortOrder = 7,
            ),
            AllocationRule(
                templateId = templateId, name = "医疗与应急", categoryId = catId("医疗与应急"),
                ruleType = RuleType.FIXED_AMOUNT, value = 100_00L, priority = 8,
                envelopeType = EnvelopeType.NECESSARY, color = 0xFF58D6A9, sortOrder = 8,
            ),
            AllocationRule(
                templateId = templateId, name = "其他", categoryId = catId("其他"),
                ruleType = RuleType.REMAINDER, priority = 9,
                envelopeType = EnvelopeType.FLEXIBLE, color = 0xFFA7B0C3, sortOrder = 9,
            ),
        )
    }
}
