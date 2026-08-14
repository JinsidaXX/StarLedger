package com.starledger.app.core.allocation

import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.RuleType

data class AllocationItem(
    val rule: AllocationRule,
    val amount: Long,
)

data class AllocationPreview(
    val items: List<AllocationItem>,
    val totalAllocated: Long,
    val unallocated: Long,
)

/**
 * 分配引擎：按模板规则把本期收入分配到各信封。
 * 顺序：固定金额 → 总收入比例 → 剩余金额比例 → 全部剩余。
 * 剩余不足时按顺序截断，不会出现负数。
 */
object AllocationEngine {

    fun allocate(income: Long, rules: List<AllocationRule>): AllocationPreview {
        val enabled = rules.filter { it.enabled }.sortedBy { it.sortOrder }
        val items = mutableListOf<AllocationItem>()
        var remaining = income.coerceAtLeast(0)

        fun take(rule: AllocationRule, amount: Long) {
            val a = amount.coerceAtMost(remaining).coerceAtLeast(0)
            items += AllocationItem(rule, a)
            remaining -= a
        }

        for (rule in enabled.filter { it.ruleType == RuleType.FIXED_AMOUNT }) {
            var amount = rule.value
            rule.minAmount?.let { amount = maxOf(amount, it) }
            rule.maxAmount?.let { amount = minOf(amount, it) }
            take(rule, amount)
        }

        for (rule in enabled.filter { it.ruleType == RuleType.INCOME_PERCENTAGE }) {
            var amount = income * rule.percent / 10_000
            rule.minAmount?.let { amount = maxOf(amount, it) }
            rule.maxAmount?.let { amount = minOf(amount, it) }
            take(rule, amount)
        }

        for (rule in enabled.filter { it.ruleType == RuleType.REMAINING_PERCENTAGE }) {
            var amount = remaining * rule.percent / 10_000
            rule.maxAmount?.let { amount = minOf(amount, it) }
            take(rule, amount)
        }

        val remainderRules = enabled.filter { it.ruleType == RuleType.REMAINDER }
        if (remainderRules.isNotEmpty() && remaining > 0) {
            remainderRules.forEachIndexed { index, rule ->
                val amount = if (index == remainderRules.lastIndex) {
                    remaining
                } else {
                    remaining / (remainderRules.size - index)
                }.let { base -> rule.maxAmount?.let { minOf(base, it) } ?: base }
                take(rule, amount)
            }
        }

        return AllocationPreview(
            items = items,
            totalAllocated = items.sumOf { it.amount },
            unallocated = remaining,
        )
    }

    /**
     * 收入不足时的调整预览：固定项不变，弹性项按比例缩减，
     * 最后削减储蓄/大额储备。返回建议金额表（ruleId -> amount）。
     */
    fun suggestAdjustment(
        income: Long,
        rules: List<AllocationRule>,
        original: List<AllocationItem>,
    ): Map<Long, Long> {
        val originalTotal = original.sumOf { it.amount }
        val shortage = originalTotal - income
        if (shortage <= 0) return original.associate { it.rule.id to it.amount }

        val suggested = original.associate { it.rule.id to it.amount }.toMutableMap()
        var toCut = shortage
        // 削减顺序：储蓄 → 缓冲 → 弹性 → 必要（保持固定）
        val cutOrder = original
            .sortedBy { item ->
                when (item.rule.envelopeType) {
                    com.starledger.app.core.model.EnvelopeType.SAVING -> 0
                    com.starledger.app.core.model.EnvelopeType.BUFFER -> 1
                    com.starledger.app.core.model.EnvelopeType.FLEXIBLE -> 2
                    com.starledger.app.core.model.EnvelopeType.NECESSARY -> 3
                }
            }
        for (item in cutOrder) {
            if (toCut <= 0) break
            val current = suggested[item.rule.id] ?: continue
            val cut = minOf(current, toCut)
            suggested[item.rule.id] = current - cut
            toCut -= cut
        }
        return suggested
    }
}
