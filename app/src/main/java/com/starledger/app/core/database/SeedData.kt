package com.starledger.app.core.database

import com.starledger.app.core.model.Account
import com.starledger.app.core.model.AccountType
import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.AllocationTemplate
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.EnvelopeType
import com.starledger.app.core.model.RuleType

/** 首次启动的默认数据：账户、分类、默认分配模板。名称根据语言中英文。 */
object SeedData {

    private val catZh = listOf(
        "饮食", "交通", "学习", "宿舍与生活", "通讯", "社交", "娱乐", "购物", "医疗与应急", "其他",
    )
    private val catEn = listOf(
        "Food", "Transport", "Study", "Living", "Communication", "Social", "Entertainment", "Shopping", "Medical", "Other",
    )
    private val catIcons = listOf(
        "🍜", "🚌", "📚", "🏠", "📱", "🎉", "🎮", "🛍️", "🏥", "📦",
    )
    private val catColors = listOf(
        0xFFF3B95F, 0xFF5BC8E8, 0xFF86A8FF, 0xFF8BC98A, 0xFFA78BFA,
        0xFFF08CB4, 0xFFD9A066, 0xFFFF6B7A, 0xFF58D6A9, 0xFFA7B0C3,
    )

    private val accountZh = listOf("现金", "银行卡", "微信", "支付宝", "校园卡", "信用卡")
    private val accountEn = listOf("Cash", "Bank Card", "WeChat", "Alipay", "Campus Card", "Credit Card")
    private val incomeZh = listOf("工资", "兼职收入", "红包", "其他收入")
    private val incomeEn = listOf("Salary", "Part-time", "Red Packet", "Other Income")
    private const val templateZh = "默认分配模板"
    private const val templateEn = "Default Template"
    private const val bufferZh = "缓冲"
    private const val bufferEn = "Buffer"

    fun defaultAccounts(zh: Boolean): List<Account> {
        val names = if (zh) accountZh else accountEn
        return listOf(
            Account(name = names[0], type = AccountType.CASH, color = 0xFFF3B95F, sortOrder = 0),
            Account(name = names[1], type = AccountType.BANK_CARD, color = 0xFF86A8FF, sortOrder = 1),
            Account(name = names[2], type = AccountType.WECHAT, color = 0xFF58D6A9, sortOrder = 2),
            Account(name = names[3], type = AccountType.ALIPAY, color = 0xFF5BC8E8, sortOrder = 3),
            Account(name = names[4], type = AccountType.CAMPUS_CARD, color = 0xFFA78BFA, sortOrder = 4),
            Account(name = names[5], type = AccountType.CREDIT_CARD, color = 0xFFFF6B7A, isCredit = true, sortOrder = 5),
        )
    }

    fun defaultCategories(zh: Boolean): List<Category> {
        val names = if (zh) catZh else catEn
        val incNames = if (zh) incomeZh else incomeEn
        val expense = names.mapIndexed { i, name ->
            // 「医疗与应急」分类默认标记为医疗豁免，不占可用支出额度、不触发超支提示
            Category(
                name = name, icon = catIcons[i], color = catColors[i].toLong(), sortOrder = i,
                isMedical = i == 8,
            )
        }
        val income = listOf(
            Category(name = incNames[0], icon = "💰", color = 0xFF58D6A9, isExpense = false, sortOrder = 0),
            Category(name = incNames[1], icon = "💼", color = 0xFF86A8FF, isExpense = false, sortOrder = 1),
            Category(name = incNames[2], icon = "🧧", color = 0xFFFF6B7A, isExpense = false, sortOrder = 2),
            Category(name = incNames[3], icon = "📦", color = 0xFFA7B0C3, isExpense = false, sortOrder = 3),
        )
        return expense + income
    }

    fun defaultTemplate(zh: Boolean): AllocationTemplate =
        AllocationTemplate(name = if (zh) templateZh else templateEn, isDefault = true)

    fun defaultRules(templateId: Long, categories: List<Category>, zh: Boolean): List<AllocationRule> {
        val names = if (zh) catZh else catEn
        fun catId(name: String): Long? = categories.firstOrNull { it.name == name }?.id
        return listOf(
            rule(templateId, names[0], catId(names[0]), 800_00L, 0, EnvelopeType.NECESSARY, catColors[0], 0),
            rule(templateId, names[1], catId(names[1]), 150_00L, 1, EnvelopeType.NECESSARY, catColors[1], 1),
            rule(templateId, names[2], catId(names[2]), 150_00L, 2, EnvelopeType.NECESSARY, catColors[2], 2),
            rule(templateId, names[3], catId(names[3]), 200_00L, 3, EnvelopeType.NECESSARY, catColors[3], 3),
            rule(templateId, names[4], catId(names[4]), 100_00L, 4, EnvelopeType.NECESSARY, catColors[4], 4),
            rule(templateId, names[5], catId(names[5]), 100_00L, 5, EnvelopeType.FLEXIBLE, catColors[5], 5),
            rule(templateId, names[6], catId(names[6]), 150_00L, 6, EnvelopeType.FLEXIBLE, catColors[6], 6),
            rule(templateId, names[7], catId(names[7]), 150_00L, 7, EnvelopeType.FLEXIBLE, catColors[7], 7),
            rule(templateId, names[8], catId(names[8]), 100_00L, 8, EnvelopeType.NECESSARY, catColors[8], 8),
            AllocationRule(
                templateId = templateId, name = names[9], categoryId = catId(names[9]),
                ruleType = RuleType.REMAINDER, priority = 9,
                envelopeType = EnvelopeType.FLEXIBLE, color = catColors[9].toLong(), sortOrder = 9,
            ),
        )
    }

    private val zhToEn: Map<String, String> = buildMap {
        catZh.zip(catEn).forEach { (z, e) -> put(z, e) }
        incomeZh.zip(incomeEn).forEach { (z, e) -> put(z, e) }
        accountZh.zip(accountEn).forEach { (z, e) -> put(z, e) }
        put(templateZh, templateEn)
        put(bufferZh, bufferEn)
    }
    private val enToZh: Map<String, String> = zhToEn.entries.associate { (z, e) -> e to z }

    /** 内置默认项名称（任意语言）→ 目标语言名称；非默认名返回 null（用户自定义，不动）。 */
    fun localizedName(name: String, zh: Boolean): String? =
        if (zh) enToZh[name] else zhToEn[name]

    private fun rule(
        templateId: Long,
        name: String,
        categoryId: Long?,
        value: Long,
        priority: Int,
        envelopeType: EnvelopeType,
        color: Long,
        sortOrder: Int,
    ) = AllocationRule(
        templateId = templateId, name = name, categoryId = categoryId,
        ruleType = RuleType.FIXED_AMOUNT, value = value, priority = priority,
        envelopeType = envelopeType, color = color, sortOrder = sortOrder,
    )
}
