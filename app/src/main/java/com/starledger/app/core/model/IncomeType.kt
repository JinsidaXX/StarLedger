package com.starledger.app.core.model

import com.starledger.app.R

/** 收入类型：仅对 INCOME 类交易有意义，用于滚动薪资周期的事件驱动判定 */
enum class IncomeType(@androidx.annotation.StringRes val labelResId: Int) {
    PRIMARY_SALARY(R.string.income_type_primary_salary),
    BONUS(R.string.income_type_bonus),
    PART_TIME(R.string.income_type_part_time),
    OTHER(R.string.income_type_other),
}
