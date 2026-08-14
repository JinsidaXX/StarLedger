package com.starledger.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.starledger.app.core.model.MonthlyStar
import kotlinx.coroutines.flow.Flow

@Dao
interface StarDao {

    @Query("SELECT * FROM monthly_stars ORDER BY year DESC, month DESC")
    fun observeAll(): Flow<List<MonthlyStar>>

    @Query("SELECT * FROM monthly_stars ORDER BY year DESC, month DESC")
    suspend fun getAll(): List<MonthlyStar>

    @Query("SELECT * FROM monthly_stars WHERE cycleId = :cycleId LIMIT 1")
    suspend fun getByCycle(cycleId: Long): MonthlyStar?

    @Query("SELECT * FROM monthly_stars WHERE year = :year ORDER BY month ASC")
    fun observeByYear(year: Int): Flow<List<MonthlyStar>>

    @Query("SELECT * FROM monthly_stars WHERE year = :year ORDER BY month ASC")
    suspend fun getByYearSync(year: Int): List<MonthlyStar>

    @Insert
    suspend fun insert(star: MonthlyStar): Long

    @Update
    suspend fun update(star: MonthlyStar)

    @Query("DELETE FROM monthly_stars WHERE cycleId = :cycleId")
    suspend fun deleteByCycle(cycleId: Long)

    @Query("SELECT COUNT(*) FROM monthly_stars")
    suspend fun count(): Int
}
