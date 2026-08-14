package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.AllocationTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Query("SELECT * FROM allocation_templates ORDER BY id")
    fun observeAll(): Flow<List<AllocationTemplate>>

    @Query("SELECT * FROM allocation_templates ORDER BY id")
    suspend fun getAll(): List<AllocationTemplate>

    @Query("SELECT * FROM allocation_templates WHERE id = :id")
    suspend fun getById(id: Long): AllocationTemplate?

    @Query("SELECT * FROM allocation_templates WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): AllocationTemplate?

    @Insert
    suspend fun insert(template: AllocationTemplate): Long

    @Update
    suspend fun update(template: AllocationTemplate)

    @Delete
    suspend fun delete(template: AllocationTemplate)

    @Query("SELECT COUNT(*) FROM allocation_templates")
    suspend fun count(): Int

    @Query("UPDATE allocation_templates SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE allocation_templates SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
}
