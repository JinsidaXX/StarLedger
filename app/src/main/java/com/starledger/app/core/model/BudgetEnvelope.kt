package com.starledger.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 预算信封：周期内一个分配项的预算 */
@Entity(tableName = "budget_envelopes")
data class BudgetEnvelope(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val name: String,
    val plannedAmount: Long,
    val actualAmount: Long = 0,
    val remainingAmount: Long = 0,
    val type: EnvelopeType = EnvelopeType.NECESSARY,
    val categoryId: Long? = null,
    val color: Long = 0xFF86A8FF,
    val carryOverEnabled: Boolean = true,
    val sortOrder: Int = 0,
)

enum class EnvelopeType(val label: String) {
    NECESSARY("必要支出"),
    FLEXIBLE("弹性支出"),
    SAVING("储蓄"),
    BUFFER("缓冲"),
}
