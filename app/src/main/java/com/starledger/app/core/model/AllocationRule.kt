package com.starledger.app.core.model

import com.starledger.app.R

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 分配规则：模板中的一条分配项 */
@Entity(tableName = "allocation_rules")
data class AllocationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val name: String,
    val categoryId: Long? = null,
    val ruleType: RuleType,
    /** 固定金额（分） */
    val value: Long = 0,
    /** 百分比，万分比（2500 = 25%） */
    val percent: Int = 0,
    val priority: Int = 0,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val carryOver: Boolean = true,
    val enabled: Boolean = true,
    val envelopeType: EnvelopeType = EnvelopeType.NECESSARY,
    val color: Long = 0xFF86A8FF,
    val sortOrder: Int = 0,
)

enum class RuleType(@androidx.annotation.StringRes val labelResId: Int) {
    FIXED_AMOUNT(R.string.rule_fixed_amount),
    INCOME_PERCENTAGE(R.string.rule_income_percentage),
    REMAINING_PERCENTAGE(R.string.rule_remaining_percentage),
    REMAINDER(R.string.rule_remainder),
}
