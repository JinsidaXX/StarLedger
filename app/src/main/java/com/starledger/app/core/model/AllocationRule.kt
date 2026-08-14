package com.starledger.app.core.model

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

enum class RuleType(val label: String) {
    FIXED_AMOUNT("固定金额"),
    INCOME_PERCENTAGE("总收入比例"),
    REMAINING_PERCENTAGE("剩余金额比例"),
    REMAINDER("全部剩余"),
}
