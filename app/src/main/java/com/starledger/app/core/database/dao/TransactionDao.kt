package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    suspend fun getBetween(start: Long, end: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<Transaction>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    /** 某账户余额变化：初始余额 + 收入 - 支出 + 转入 - 转出 */
    @Query(
        """
        SELECT COALESCE(SUM(
            CASE
                WHEN type = 'EXPENSE' AND accountId = :accountId THEN -amount
                WHEN type IN ('INCOME','REFUND','REIMBURSEMENT') AND accountId = :accountId THEN amount
                WHEN type = 'TRANSFER' AND accountId = :accountId THEN -amount
                WHEN type = 'TRANSFER' AND toAccountId = :accountId THEN amount
                ELSE 0
            END
        ), 0) FROM transactions
        """
    )
    suspend fun balanceDelta(accountId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0)
        FROM transactions WHERE date BETWEEN :start AND :end
        """
    )
    suspend fun sumExpense(start: Long, end: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type IN ('INCOME','REFUND','REIMBURSEMENT') THEN amount ELSE 0 END), 0)
        FROM transactions WHERE date BETWEEN :start AND :end
        """
    )
    suspend fun sumIncome(start: Long, end: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end AND categoryId = :categoryId
        """
    )
    suspend fun sumExpenseByCategory(start: Long, end: Long, categoryId: Long): Long

    /** 分类支出，排除大额消费计划（relatedPlanId 非空）产生的支出 */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end
        AND categoryId = :categoryId AND relatedPlanId IS NULL
        """
    )
    suspend fun sumExpenseByCategoryExcludingPlans(start: Long, end: Long, categoryId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0)
        FROM transactions WHERE categoryId = :categoryId
        """
    )
    suspend fun countUsageByCategory(categoryId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0)
        FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId
        """
    )
    suspend fun countUsageByAccount(accountId: Long): Long

    @Query("SELECT * FROM transactions WHERE relatedPlanId = :planId LIMIT 1")
    suspend fun getByPlan(planId: Long): Transaction?

    @Query(
        """
        SELECT COUNT(DISTINCT date / 86400000) FROM transactions
        WHERE date BETWEEN :start AND :end
        """
    )
    suspend fun countActiveDays(start: Long, end: Long): Int

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<Transaction>

    /** 非医疗支出合计（医疗类不占可用支出额度）。未关联分类的支出计入非医疗。 */
    @Query(
        """
        SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE' AND t.date BETWEEN :start AND :end
        AND (c.id IS NULL OR c.isMedical = 0)
        """
    )
    suspend fun sumNonMedicalExpense(start: Long, end: Long): Long
}
