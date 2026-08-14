package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.OwnedItem
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnedItemDao {

    @Query("SELECT * FROM owned_items ORDER BY purchaseDate DESC")
    fun observeAll(): Flow<List<OwnedItem>>

    @Query("SELECT * FROM owned_items ORDER BY purchaseDate DESC")
    suspend fun getAll(): List<OwnedItem>

    @Insert
    suspend fun insert(item: OwnedItem): Long

    @Update
    suspend fun update(item: OwnedItem)

    @Query("DELETE FROM owned_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM owned_items")
    suspend fun count(): Int
}
