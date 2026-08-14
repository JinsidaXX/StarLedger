package com.starledger.app.core.model

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.starledger.app.R

/** 账户：钱实际存放的位置 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Long = 0,
    val currency: String = "CNY",
    val color: Long = 0xFF86A8FF,
    val isCredit: Boolean = false,
    val isHidden: Boolean = false,
    val includeInTotal: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class AccountType(@StringRes val labelResId: Int) {
    CASH(R.string.account_type_cash),
    BANK_CARD(R.string.account_type_bank_card),
    WECHAT(R.string.account_type_wechat),
    ALIPAY(R.string.account_type_alipay),
    CAMPUS_CARD(R.string.account_type_campus_card),
    CREDIT_CARD(R.string.account_type_credit_card),
    OTHER(R.string.account_type_other),
}
