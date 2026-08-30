package com.starledger.app.core.model

import com.starledger.app.R

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 交易记录。金额单位统一为分。 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val merchant: String = "",
    val note: String = "",
    val tags: List<String> = emptyList(),
    val relatedPlanId: Long? = null,
    val cycleId: Long? = null,
    /** 收入类型：仅对 INCOME 类交易有意义，用于滚动薪资周期判定 */
    val incomeType: IncomeType? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class TxType(@androidx.annotation.StringRes val labelResId: Int) {
    EXPENSE(R.string.tx_type_expense),
    INCOME(R.string.tx_type_income),
    TRANSFER(R.string.tx_type_transfer),
    REFUND(R.string.tx_type_refund),
    REIMBURSEMENT(R.string.tx_type_reimbursement),
}
