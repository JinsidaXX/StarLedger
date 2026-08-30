package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.BudgetCycle
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {

    @Query("SELECT * FROM budget_cycles ORDER BY year DESC, month DESC")
    fun observeAll(): Flow<List<BudgetCycle>>

    @Query("SELECT * FROM budget_cycles ORDER BY year DESC, month DESC")
    suspend fun getAll(): List<BudgetCycle>

    @Query("SELECT * FROM budget_cycles WHERE id = :id")
    suspend fun getById(id: Long): BudgetCycle?

    @Query("SELECT * FROM budget_cycles WHERE year = :year AND month = :month")
    suspend fun getByMonth(year: Int, month: Int): BudgetCycle?

    @Query("SELECT * FROM budget_cycles WHERE year = :year ORDER BY month ASC")
    suspend fun getByYear(year: Int): List<BudgetCycle>

    @Query("SELECT * FROM budget_cycles WHERE status = 'ACTIVE' ORDER BY startDate DESC LIMIT 1")
    suspend fun getActive(): BudgetCycle?

    @Query("SELECT * FROM budget_cycles WHERE status = 'ACTIVE' ORDER BY startDate DESC LIMIT 1")
    suspend fun getRunning(): BudgetCycle?

    @Query("SELECT * FROM budget_cycles WHERE status = 'ACTIVE' ORDER BY startDate DESC")
    suspend fun getActiveAll(): List<BudgetCycle>

    @Query("SELECT * FROM budget_cycles WHERE endDate < :now ORDER BY endDate DESC")
    suspend fun getPast(now: Long): List<BudgetCycle>

    @Insert
    suspend fun insert(cycle: BudgetCycle): Long

    @Update
    suspend fun update(cycle: BudgetCycle)

    @Query("DELETE FROM budget_cycles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM budget_cycles")
    suspend fun count(): Int
}
