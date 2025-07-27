package com.example.node_mind.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "focus_sessions")
@Serializable
data class FocusSession(
    @PrimaryKey
    val id: String = generateId(),
    val durationMinutes: Int = 25, // Pomodoro default
    val startTime: String, // ISO string format
    val endTime: String? = null,
    val isCompleted: Boolean = false,
    val sessionType: SessionType = SessionType.FOCUS,
    val taskId: String? = null, // Optional linked task
    val notes: String = ""
) {
    companion object {
        fun generateId(): String = "session_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

enum class SessionType(val displayName: String, val durationMinutes: Int) {
    FOCUS("Focus", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

@Entity(tableName = "daily_stats")
@Serializable
data class DailyStats(
    @PrimaryKey
    val date: String, // yyyy-MM-dd format
    val tasksCompleted: Int = 0,
    val focusSessionsCompleted: Int = 0,
    val totalFocusMinutes: Int = 0,
    val streak: Int = 0
)
