package com.starledger.app.core.model

import com.starledger.app.R

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

enum class EnvelopeType(@androidx.annotation.StringRes val labelResId: Int) {
    NECESSARY(R.string.envelope_necessary),
    FLEXIBLE(R.string.envelope_flexible),
    SAVING(R.string.envelope_saving),
    BUFFER(R.string.envelope_buffer),
}
