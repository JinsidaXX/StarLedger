package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE isExpense = 1 ORDER BY sortOrder, id")
    fun observeExpense(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isExpense = 0 ORDER BY sortOrder, id")
    fun observeIncome(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
