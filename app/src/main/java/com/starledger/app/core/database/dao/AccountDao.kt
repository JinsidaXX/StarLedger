package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    suspend fun getAll(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}
