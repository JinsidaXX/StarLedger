package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.AllocationRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM allocation_rules WHERE templateId = :templateId ORDER BY sortOrder, id")
    fun observeByTemplate(templateId: Long): Flow<List<AllocationRule>>

    @Query("SELECT * FROM allocation_rules WHERE templateId = :templateId ORDER BY sortOrder, id")
    suspend fun getByTemplate(templateId: Long): List<AllocationRule>

    @Query("SELECT * FROM allocation_rules WHERE id = :id")
    suspend fun getById(id: Long): AllocationRule?

    @Insert
    suspend fun insert(rule: AllocationRule): Long

    @Update
    suspend fun update(rule: AllocationRule)

    @Delete
    suspend fun delete(rule: AllocationRule)

    @Query("DELETE FROM allocation_rules WHERE templateId = :templateId")
    suspend fun deleteByTemplate(templateId: Long)

    @Query("SELECT COUNT(*) FROM allocation_rules")
    suspend fun count(): Int
}
