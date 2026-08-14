package com.starledger.app.core.model

import com.starledger.app.R

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

enum class PlanStatus(@androidx.annotation.StringRes val labelResId: Int) {
    DRAFT(R.string.plan_status_draft),
    COOLING(R.string.plan_status_cooling),
    READY(R.string.plan_status_ready),
    POSTPONED(R.string.plan_status_postponed),
    REPLACED(R.string.plan_status_replaced),
    CANCELED(R.string.plan_status_canceled),
    PURCHASED(R.string.plan_status_purchased),
}
