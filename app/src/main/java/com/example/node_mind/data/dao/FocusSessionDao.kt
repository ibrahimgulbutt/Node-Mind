package com.example.node_mind.data.dao

import androidx.room.*
import com.example.node_mind.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions WHERE DATE(startTime) = :date ORDER BY startTime DESC")
    fun getSessionsByDate(date: String): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getSessionsByTask(taskId: String): Flow<List<FocusSession>>
    
    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): FocusSession?
    
    @Query("SELECT * FROM focus_sessions WHERE DATE(startTime) = :date AND isCompleted = 1")
    suspend fun getCompletedSessionsByDate(date: String): List<FocusSession>
    
    @Query("SELECT * FROM focus_sessions WHERE DATE(startTime) BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    suspend fun getSessionsInRange(startDate: String, endDate: String): List<FocusSession>
    
    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM focus_sessions WHERE isCompleted = 1 AND sessionType = 'FOCUS'")
    suspend fun getTotalFocusMinutes(): Int
    
    @Query("SELECT COALESCE(AVG(durationMinutes), 0.0) FROM focus_sessions WHERE isCompleted = 1")
    suspend fun getAverageSessionDuration(): Double
    
    @Insert
    suspend fun insertSession(session: FocusSession): Long
    
    @Update
    suspend fun updateSession(session: FocusSession)
    
    @Delete
    suspend fun deleteSession(session: FocusSession)
    
    @Query("UPDATE focus_sessions SET isCompleted = 1, endTime = :endTime WHERE id = :sessionId")
    suspend fun completeSession(sessionId: String, endTime: String)
}
