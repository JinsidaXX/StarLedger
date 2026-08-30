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
    /** 周期模式：日历月 / 滚动薪资 */
    val cycleMode: CycleMode = CycleMode.CALENDAR_MONTH,
    /** 周期关闭类型（审计排查用），运行中为 null */
    val closeReason: CycleCloseReason? = null,
    /** 滚动薪资模式的最大运行天数（仅告警，不强制关闭），默认 50 天 */
    val maxRunDays: Int = 50,
    /** 预算延迟生效时间：预发工资时，规划消费预算延迟到该时间解锁；null 表示立即生效 */
    val effectStartTime: Long? = null,
    /** 强制存储类型（随主薪资下发设置） */
    val forcedSavingType: ForcedSavingType = ForcedSavingType.NONE,
    /** 强制存储参数：固定金额时为分；百分比时为万分比（2500 = 25%） */
    val forcedSavingValue: Long = 0,
    /** 本周期强制存储的目标金额（分），由收入 × 参数计算得出 */
    val forcedSavingAmount: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class CycleStatus { ACTIVE, CLOSED }
