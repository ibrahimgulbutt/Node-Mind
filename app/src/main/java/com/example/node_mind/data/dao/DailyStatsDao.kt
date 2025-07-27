package com.example.node_mind.data.dao

import androidx.room.*
import com.example.node_mind.data.model.DailyStats
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun getDailyStats(date: String): Flow<DailyStats?>
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getDailyStatsSync(date: String): DailyStats?
    
    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getStatsInRange(startDate: String, endDate: String): Flow<List<DailyStats>>
    
    @Query("SELECT * FROM daily_stats WHERE date LIKE :month || '%' ORDER BY date DESC")
    fun getMonthlyStats(month: String): Flow<List<DailyStats>>
    
    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    suspend fun getAllStatsOrderedByDate(): List<DailyStats>
    
    @Query("SELECT COALESCE(SUM(totalFocusMinutes), 0) FROM daily_stats")
    suspend fun getTotalFocusMinutes(): Int
    
    @Query("SELECT COALESCE(SUM(tasksCompleted), 0) FROM daily_stats")
    suspend fun getTotalTasksCompleted(): Int
    
    @Query("SELECT COALESCE(SUM(focusSessionsCompleted), 0) FROM daily_stats")
    suspend fun getTotalSessionsCompleted(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: DailyStats)
    
    @Update
    suspend fun updateStats(stats: DailyStats)
    
    @Delete
    suspend fun deleteStats(stats: DailyStats)
}
