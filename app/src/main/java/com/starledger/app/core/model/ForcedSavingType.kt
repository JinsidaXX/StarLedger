package com.starledger.app.core.model

import com.starledger.app.R

/** 强制存储设置类型：随主薪资下发时确定 */
enum class ForcedSavingType(@androidx.annotation.StringRes val labelResId: Int) {
    /** 不设置强制存储 */
    NONE(R.string.forced_saving_none),

    /** 固定金额 */
    FIXED_AMOUNT(R.string.forced_saving_fixed),

    /** 收入百分比 */
    INCOME_PERCENTAGE(R.string.forced_saving_percent),
}
