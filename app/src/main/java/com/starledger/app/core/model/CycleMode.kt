package com.starledger.app.core.model

import com.starledger.app.R

/** 财务周期模式 */
enum class CycleMode(@androidx.annotation.StringRes val labelResId: Int) {
    /** 日历月：传统自然月周期 */
    CALENDAR_MONTH(R.string.cycle_mode_calendar),

    /** 滚动薪资周期：由薪资事件驱动，不随日期自动闭合 */
    ROLLING_SALARY(R.string.cycle_mode_rolling),
}

/** 周期关闭类型：记录周期如何结束，用于审计排查 */
enum class CycleCloseReason(@androidx.annotation.StringRes val labelResId: Int) {
    /** 用户确认主薪资后结算旧周期、开启新周期 */
    CONFIRMED_SALARY(R.string.cycle_close_confirmed_salary),

    /** 用户在周期详情页手动结束并结算 */
    MANUAL(R.string.cycle_close_manual),

    /** 日历月自然到期自动闭合 */
    CALENDAR_AUTO(R.string.cycle_close_calendar_auto),
}
