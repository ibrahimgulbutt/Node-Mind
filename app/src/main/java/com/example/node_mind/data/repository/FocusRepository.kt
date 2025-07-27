package com.example.node_mind.data.repository

import com.example.node_mind.data.dao.FocusSessionDao
import com.example.node_mind.data.dao.DailyStatsDao
import com.example.node_mind.data.model.FocusSession
import com.example.node_mind.data.model.DailyStats
import com.example.node_mind.data.model.SessionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

class FocusRepository(
    private val focusSessionDao: FocusSessionDao,
    private val dailyStatsDao: DailyStatsDao
) {
    
    fun getAllSessions(): Flow<List<FocusSession>> = focusSessionDao.getAllSessions()
    
    fun getSessionsByDate(date: String): Flow<List<FocusSession>> = 
        focusSessionDao.getSessionsByDate(date)
    
    fun getTodaySessions(): Flow<List<FocusSession>> = 
        focusSessionDao.getSessionsByDate(LocalDate.now().toString())
    
    fun getRecentSessions(limit: Int = 10): Flow<List<FocusSession>> = 
        focusSessionDao.getRecentSessions(limit)
    
    fun getSessionsByTask(taskId: String): Flow<List<FocusSession>> = 
        focusSessionDao.getSessionsByTask(taskId)
    
    suspend fun insertSession(session: FocusSession): Long = 
        focusSessionDao.insertSession(session)
    
    suspend fun updateSession(session: FocusSession) = 
        focusSessionDao.updateSession(session)
    
    suspend fun deleteSession(session: FocusSession) = 
        focusSessionDao.deleteSession(session)
    
    suspend fun getSessionById(id: String): FocusSession? = 
        focusSessionDao.getSessionById(id)
    
    suspend fun completeSession(sessionId: String, endTime: String) {
        focusSessionDao.completeSession(sessionId, endTime)
        updateDailyStats()
    }
    
    // Daily Stats methods
    fun getDailyStats(date: String): Flow<DailyStats?> = 
        dailyStatsDao.getDailyStats(date)
    
    fun getTodayStats(): Flow<DailyStats?> = 
        dailyStatsDao.getDailyStats(LocalDate.now().toString())
    
    fun getWeeklyStats(startDate: String, endDate: String): Flow<List<DailyStats>> = 
        dailyStatsDao.getStatsInRange(startDate, endDate)
    
    fun getMonthlyStats(month: String): Flow<List<DailyStats>> = 
        dailyStatsDao.getMonthlyStats(month)
    
    suspend fun insertOrUpdateDailyStats(stats: DailyStats) = 
        dailyStatsDao.insertOrUpdateStats(stats)
    
    private suspend fun updateDailyStats() {
        val today = LocalDate.now().toString()
        val currentStats = dailyStatsDao.getDailyStatsSync(today)
        
        val todaySessions = focusSessionDao.getCompletedSessionsByDate(today)
        val focusMinutes = todaySessions
            .filter { it.sessionType == SessionType.FOCUS }
            .sumOf { it.durationMinutes }
        
        val newStats = currentStats?.copy(
            focusSessionsCompleted = todaySessions.size,
            totalFocusMinutes = focusMinutes
        ) ?: DailyStats(
            date = today,
            focusSessionsCompleted = todaySessions.size,
            totalFocusMinutes = focusMinutes
        )
        
        dailyStatsDao.insertOrUpdateStats(newStats)
    }
    
    suspend fun getTotalFocusTime(): Int {
        return focusSessionDao.getTotalFocusMinutes()
    }
    
    suspend fun getCurrentStreak(): Int {
        val stats = dailyStatsDao.getAllStatsOrderedByDate()
        var streak = 0
        var currentDate = LocalDate.now()
        
        for (stat in stats) {
            if (stat.date == currentDate.toString() && stat.focusSessionsCompleted > 0) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }
        
        return streak
    }
    
    suspend fun getSessionsThisWeek(): List<FocusSession> {
        val startOfWeek = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value - 1L)
        val endOfWeek = startOfWeek.plusDays(6)
        return focusSessionDao.getSessionsInRange(startOfWeek.toString(), endOfWeek.toString())
    }
    
    suspend fun getAverageSessionDuration(): Double {
        return focusSessionDao.getAverageSessionDuration()
    }
}
