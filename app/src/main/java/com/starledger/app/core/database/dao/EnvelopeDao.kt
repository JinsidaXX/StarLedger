package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.BudgetEnvelope
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao {

    @Query("SELECT * FROM budget_envelopes WHERE cycleId = :cycleId ORDER BY sortOrder, id")
    fun observeByCycle(cycleId: Long): Flow<List<BudgetEnvelope>>

    @Query("SELECT * FROM budget_envelopes WHERE cycleId = :cycleId ORDER BY sortOrder, id")
    suspend fun getByCycle(cycleId: Long): List<BudgetEnvelope>

    @Query("SELECT * FROM budget_envelopes WHERE id = :id")
    suspend fun getById(id: Long): BudgetEnvelope?

    @Insert
    suspend fun insert(envelope: BudgetEnvelope): Long

    @Insert
    suspend fun insertAll(envelopes: List<BudgetEnvelope>)

    @Update
    suspend fun update(envelope: BudgetEnvelope)

    @Delete
    suspend fun delete(envelope: BudgetEnvelope)

    @Query("DELETE FROM budget_envelopes WHERE cycleId = :cycleId")
    suspend fun deleteByCycle(cycleId: Long)

    @Query("SELECT DISTINCT cycleId FROM budget_envelopes")
    suspend fun getAllCycles(): List<Long>

    @Query("SELECT COUNT(*) FROM budget_envelopes")
    suspend fun count(): Int
}
