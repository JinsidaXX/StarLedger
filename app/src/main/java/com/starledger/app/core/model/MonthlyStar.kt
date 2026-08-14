package com.starledger.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 月度恒星：一个财务月生成的星 */
@Entity(tableName = "monthly_stars")
data class MonthlyStar(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val year: Int,
    val month: Int,
    /** 星辉亮度 0..1 */
    val brightness: Float = 0f,
    val colorState: StarColorState = StarColorState.BLUE,
    /** 记录完整度 0..1 */
    val observationCompleteness: Float = 0f,
    /** 分配完成度 0..1 */
    val allocationCompletion: Float = 0f,
    /** 总体预算状态：实际/计划 */
    val budgetStatus: Float = 0f,
    val reviewCompleted: Boolean = false,
    val surplusAmount: Long = 0,
    /** JSON 快照：各星芒数据 */
    val snapshotData: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class StarColorState(val label: String) {
    BLUE("计划内"),
    WARM("轻微偏差"),
    ORANGE("明显偏差"),
    RED("严重超支"),
    FOG("未记录"),
}
