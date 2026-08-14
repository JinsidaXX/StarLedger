package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.PlannedPurchase
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Query("SELECT * FROM planned_purchases ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlannedPurchase>>

    @Query("SELECT * FROM planned_purchases ORDER BY createdAt DESC")
    suspend fun getAll(): List<PlannedPurchase>

    @Query(
        """
        SELECT * FROM planned_purchases
        WHERE status IN ('COOLING','READY','POSTPONED','DRAFT')
        ORDER BY earliestDecisionDate ASC
        """
    )
    fun observeActive(): Flow<List<PlannedPurchase>>

    @Query("SELECT * FROM planned_purchases WHERE id = :id")
    suspend fun getById(id: Long): PlannedPurchase?

    @Insert
    suspend fun insert(plan: PlannedPurchase): Long

    @Update
    suspend fun update(plan: PlannedPurchase)

    @Query("DELETE FROM planned_purchases WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM planned_purchases")
    suspend fun count(): Int
}
