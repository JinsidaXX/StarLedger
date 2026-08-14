package com.starledger.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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

enum class AccountType(val label: String) {
    CASH("现金"),
    BANK_CARD("银行卡"),
    WECHAT("微信"),
    ALIPAY("支付宝"),
    CAMPUS_CARD("校园卡"),
    CREDIT_CARD("信用卡"),
    OTHER("其他"),
}
