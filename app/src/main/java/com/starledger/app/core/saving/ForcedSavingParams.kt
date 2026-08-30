package com.starledger.app.core.saving

import com.starledger.app.core.model.ForcedSavingType

/**
 * 强制存储参数（随主薪资下发时设置）。
 *
 * @param type 设置类型
 * @param value 参数值：固定金额时为分；百分比时为万分比（2500 = 25%）
 */
data class ForcedSavingParams(
    val type: ForcedSavingType = ForcedSavingType.NONE,
    val value: Long = 0,
) {
    /** 根据收入计算强制存储目标金额 */
    fun amount(income: Long): Long = when (type) {
        ForcedSavingType.NONE -> 0L
        ForcedSavingType.FIXED_AMOUNT -> value.coerceAtLeast(0)
        ForcedSavingType.INCOME_PERCENTAGE -> (income * value / 10_000L).coerceAtLeast(0)
    }
}
