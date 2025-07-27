package com.example.node_mind.data.database

import androidx.room.*
import com.example.node_mind.data.model.FocusSession
import com.example.node_mind.data.model.DailyStats
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions WHERE DATE(startTime) = DATE('now', 'localtime')")
    fun getTodaySessions(): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): FocusSession?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)
    
    @Update
    suspend fun updateSession(session: FocusSession)
    
    @Delete
    suspend fun deleteSession(session: FocusSession)
    
    // Daily Stats
    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<DailyStats>>
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsByDate(date: String): DailyStats?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: DailyStats)
    
    @Query("SELECT * FROM daily_stats WHERE date >= date('now', '-7 days') ORDER BY date DESC")
    fun getWeeklyStats(): Flow<List<DailyStats>>
    
    @Query("SELECT MAX(streak) FROM daily_stats")
    suspend fun getMaxStreak(): Int?
}
