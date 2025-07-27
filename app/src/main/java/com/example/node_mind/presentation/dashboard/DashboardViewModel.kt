package com.example.node_mind.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.node_mind.data.model.DailyStats
import com.example.node_mind.data.model.FocusSession
import com.example.node_mind.data.model.Task
import com.example.node_mind.data.repository.FocusRepository
import com.example.node_mind.data.repository.TaskRepository
import com.example.node_mind.data.repository.NodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val weeklyStats: List<DailyStats> = emptyList(),
    val totalStats: TotalStats = TotalStats(),
    val recentActivity: List<ActivityItem> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val showSettings: Boolean = false,
    val settings: AppSettings = AppSettings()
)

data class TotalStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val totalNotes: Int = 0,
    val totalFocusHours: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

data class ActivityItem(
    val id: String,
    val type: ActivityType,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val icon: String
)

enum class ActivityType {
    TASK_COMPLETED,
    FOCUS_SESSION,
    NOTE_CREATED,
    ACHIEVEMENT_UNLOCKED
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f,
    val target: Int = 1
)

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val enableNotifications: Boolean = true,
    val defaultFocusDuration: Int = 25,
    val enableSounds: Boolean = true,
    val autoStartBreaks: Boolean = false,
    val showWeeklyReports: Boolean = true,
    val backupEnabled: Boolean = false
)

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

class DashboardViewModel(
    private val focusRepository: FocusRepository,
    private val taskRepository: TaskRepository,
    private val nodeRepository: NodeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load weekly stats
                val startOfWeek = LocalDate.now().minusDays(6)
                val endOfWeek = LocalDate.now()
                
                val weeklyStats = focusRepository.getWeeklyStats(
                    startOfWeek.toString(),
                    endOfWeek.toString()
                ).first() // Get first emission instead of collecting
                
                _uiState.value = _uiState.value.copy(weeklyStats = weeklyStats)
                
                // Load total stats
                loadTotalStats()
                
                // Load recent activity
                loadRecentActivity()
                
                // Load achievements
                loadAchievements()
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load dashboard: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    private suspend fun loadTotalStats() {
        val allTasks = taskRepository.getAllTasks().first()
        val completedTasks = allTasks.count { it.isCompleted }
        val allNotes = nodeRepository.getAllNodes().first()
        val totalFocusMinutes = focusRepository.getTotalFocusTime()
        val currentStreak = focusRepository.getCurrentStreak()
        
        _uiState.value = _uiState.value.copy(
            totalStats = TotalStats(
                totalTasks = allTasks.size,
                completedTasks = completedTasks,
                totalNotes = allNotes.size,
                totalFocusHours = totalFocusMinutes / 60,
                currentStreak = currentStreak,
                longestStreak = currentStreak // For now, could be calculated properly
            )
        )
    }
    
    private suspend fun loadRecentActivity() {
        val activities = mutableListOf<ActivityItem>()
        
        // Recent focus sessions
        val recentSessions = focusRepository.getRecentSessions(5).first()
        recentSessions.forEach { session ->
            if (session.isCompleted) {
                activities.add(
                    ActivityItem(
                        id = session.id,
                        type = ActivityType.FOCUS_SESSION,
                        title = "Focus Session Completed",
                        subtitle = "${session.durationMinutes}min ${session.sessionType.displayName}",
                        timestamp = session.endTime ?: session.startTime,
                        icon = "🎯"
                    )
                )
            }
        }
        
        // Recent completed tasks
        val recentTasks = taskRepository.getAllTasks().first()
            .filter { it.isCompleted }
            .sortedByDescending { it.completedAt }
            .take(5)
        
        recentTasks.forEach { task ->
            activities.add(
                ActivityItem(
                    id = task.id,
                    type = ActivityType.TASK_COMPLETED,
                    title = "Task Completed",
                    subtitle = task.title,
                    timestamp = task.completedAt ?: task.createdAt,
                    icon = "✅"
                )
            )
        }
        
        // Recent notes
        val recentNotes = nodeRepository.getAllNodes().first()
            .sortedByDescending { it.createdAt }
            .take(3)
        
        recentNotes.forEach { note ->
            activities.add(
                ActivityItem(
                    id = note.id,
                    type = ActivityType.NOTE_CREATED,
                    title = "Note Created",
                    subtitle = note.title,
                    timestamp = note.createdAt,
                    icon = "📝"
                )
            )
        }
        
        // Sort by timestamp and update state
        val sortedActivities = activities.sortedByDescending { it.timestamp }.take(10)
        _uiState.value = _uiState.value.copy(recentActivity = sortedActivities)
    }
    
    private fun loadAchievements() {
        val stats = _uiState.value.totalStats
        val achievements = listOf(
            Achievement(
                id = "first_task",
                title = "First Steps",
                description = "Complete your first task",
                icon = "🎯",
                isUnlocked = stats.completedTasks >= 1,
                progress = minOf(stats.completedTasks.toFloat(), 1f),
                target = 1
            ),
            Achievement(
                id = "task_master",
                title = "Task Master",
                description = "Complete 10 tasks",
                icon = "🏆",
                isUnlocked = stats.completedTasks >= 10,
                progress = minOf(stats.completedTasks.toFloat() / 10f, 1f),
                target = 10
            ),
            Achievement(
                id = "focus_warrior",
                title = "Focus Warrior",
                description = "Complete 25 hours of focus time",
                icon = "⚡",
                isUnlocked = stats.totalFocusHours >= 25,
                progress = minOf(stats.totalFocusHours.toFloat() / 25f, 1f),
                target = 25
            ),
            Achievement(
                id = "streak_keeper",
                title = "Streak Keeper",
                description = "Maintain a 7-day streak",
                icon = "🔥",
                isUnlocked = stats.currentStreak >= 7,
                progress = minOf(stats.currentStreak.toFloat() / 7f, 1f),
                target = 7
            ),
            Achievement(
                id = "note_taker",
                title = "Note Taker",
                description = "Create 20 notes",
                icon = "📚",
                isUnlocked = stats.totalNotes >= 20,
                progress = minOf(stats.totalNotes.toFloat() / 20f, 1f),
                target = 20
            )
        )
        
        _uiState.value = _uiState.value.copy(achievements = achievements)
    }
    
    fun showSettings() {
        _uiState.value = _uiState.value.copy(showSettings = true)
    }
    
    fun hideSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }
    
    fun updateSettings(settings: AppSettings) {
        _uiState.value = _uiState.value.copy(settings = settings)
        // Here you would save to preferences/datastore
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun refreshData() {
        loadDashboardData()
    }
}
