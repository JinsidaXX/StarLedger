package com.starledger.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 大额消费计划 */
@Entity(tableName = "planned_purchases")
data class PlannedPurchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val estimatedAmount: Long,
    val reason: String = "",
    val alternative: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** 冷静期结束、可以决定的时间 */
    val earliestDecisionDate: Long,
    val targetDate: Long? = null,
    val coolingDays: Int = 7,
    val sourceEnvelopeId: Long? = null,
    val status: PlanStatus = PlanStatus.COOLING,
    val note: String = "",
    val purchasedTransactionId: Long? = null,
)

enum class PlanStatus(val label: String) {
    DRAFT("草稿"),
    COOLING("冷静期"),
    READY("可决定"),
    POSTPONED("已延期"),
    REPLACED("已更换方案"),
    CANCELED("已放弃"),
    PURCHASED("已购买"),
}
