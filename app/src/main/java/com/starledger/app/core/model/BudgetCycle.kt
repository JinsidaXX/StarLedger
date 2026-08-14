package com.starledger.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 预算周期：一个财务月 */
@Entity(tableName = "budget_cycles")
data class BudgetCycle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val year: Int,
    val month: Int,
    val startDate: Long,
    val endDate: Long,
    val status: CycleStatus = CycleStatus.ACTIVE,
    /** 本期收入（用户记录） */
    val totalIncome: Long = 0,
    /** 已分配金额 */
    val totalAllocated: Long = 0,
    /** 记录完整度 0..1 */
    val observationCompleteness: Float = 0f,
    val markedUnrecorded: Boolean = false,
    val reviewCompleted: Boolean = false,
    val surplusHandled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class CycleStatus { ACTIVE, CLOSED }
