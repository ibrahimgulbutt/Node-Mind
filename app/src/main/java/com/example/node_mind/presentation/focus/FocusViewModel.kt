package com.example.node_mind.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.node_mind.data.model.FocusSession
import com.example.node_mind.data.model.SessionType
import com.example.node_mind.data.model.Task
import com.example.node_mind.data.repository.FocusRepository
import com.example.node_mind.data.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class FocusUiState(
    val currentSession: FocusSession? = null,
    val recentSessions: List<FocusSession> = emptyList(),
    val availableTasks: List<Task> = emptyList(),
    val selectedTask: Task? = null,
    val sessionType: SessionType = SessionType.FOCUS,
    val customDuration: Int = 25,
    val isTimerRunning: Boolean = false,
    val isPaused: Boolean = false,
    val remainingSeconds: Int = 25 * 60, // 25 minutes in seconds
    val totalSeconds: Int = 25 * 60,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTaskPicker: Boolean = false,
    val showCustomDurationDialog: Boolean = false,
    val showSessionCompleteDialog: Boolean = false,
    val sessionNotes: String = "",
    val totalFocusTime: Int = 0,
    val currentStreak: Int = 0,
    val todayStats: FocusStats = FocusStats()
)

data class FocusStats(
    val sessionsCompleted: Int = 0,
    val totalMinutes: Int = 0,
    val averageSession: Double = 0.0,
    val streak: Int = 0
)

class FocusViewModel(
    private val focusRepository: FocusRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load recent sessions
                val sessions = focusRepository.getRecentSessions(10).first()
                _uiState.value = _uiState.value.copy(recentSessions = sessions)
                
                // Load available tasks
                val allTasks = taskRepository.getAllTasks().first()
                val incompleteTasks = allTasks.filter { !it.isCompleted }
                _uiState.value = _uiState.value.copy(availableTasks = incompleteTasks)
                
                // Load today's stats
                loadTodayStats()
                
                // Load overall stats
                val totalFocusTime = focusRepository.getTotalFocusTime()
                val currentStreak = focusRepository.getCurrentStreak()
                
                _uiState.value = _uiState.value.copy(
                    totalFocusTime = totalFocusTime,
                    currentStreak = currentStreak,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load focus data: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    private suspend fun loadTodayStats() {
        val todaySessions = focusRepository.getTodaySessions().first()
        val completedSessions = todaySessions.filter { it.isCompleted }
        val totalMinutes = completedSessions.sumOf { it.durationMinutes }
        val averageSession = if (completedSessions.isNotEmpty()) {
            completedSessions.map { it.durationMinutes }.average()
        } else 0.0
        
        _uiState.value = _uiState.value.copy(
            todayStats = FocusStats(
                sessionsCompleted = completedSessions.size,
                totalMinutes = totalMinutes,
                averageSession = averageSession,
                streak = focusRepository.getCurrentStreak()
            )
        )
    }
    
    fun selectSessionType(sessionType: SessionType) {
        if (!_uiState.value.isTimerRunning) {
            val duration = when (sessionType) {
                SessionType.FOCUS -> _uiState.value.customDuration
                else -> sessionType.durationMinutes
            }
            
            _uiState.value = _uiState.value.copy(
                sessionType = sessionType,
                remainingSeconds = duration * 60,
                totalSeconds = duration * 60
            )
        }
    }
    
    fun setCustomDuration(minutes: Int) {
        if (!_uiState.value.isTimerRunning && minutes > 0) {
            _uiState.value = _uiState.value.copy(
                customDuration = minutes,
                remainingSeconds = if (_uiState.value.sessionType == SessionType.FOCUS) minutes * 60 else _uiState.value.remainingSeconds,
                totalSeconds = if (_uiState.value.sessionType == SessionType.FOCUS) minutes * 60 else _uiState.value.totalSeconds
            )
        }
    }
    
    fun selectTask(task: Task?) {
        _uiState.value = _uiState.value.copy(
            selectedTask = task,
            showTaskPicker = false
        )
    }
    
    fun startTimer() {
        val state = _uiState.value
        if (state.isTimerRunning) return
        
        viewModelScope.launch {
            // Create new session
            val session = FocusSession(
                durationMinutes = state.totalSeconds / 60,
                startTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                sessionType = state.sessionType,
                taskId = state.selectedTask?.id,
                notes = ""
            )
            
            val sessionId = focusRepository.insertSession(session)
            val sessionWithId = session.copy(id = "session_${sessionId}_${System.currentTimeMillis()}")
            
            _uiState.value = _uiState.value.copy(
                currentSession = sessionWithId,
                isTimerRunning = true,
                isPaused = false
            )
            
            startTimerCountdown()
        }
    }
    
    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isTimerRunning = false,
            isPaused = true
        )
    }
    
    fun resumeTimer() {
        if (_uiState.value.isPaused) {
            _uiState.value = _uiState.value.copy(
                isTimerRunning = true,
                isPaused = false
            )
            startTimerCountdown()
        }
    }
    
    fun stopTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isTimerRunning = false,
            isPaused = false,
            currentSession = null,
            remainingSeconds = _uiState.value.totalSeconds
        )
    }
    
    fun completeSession() {
        val state = _uiState.value
        state.currentSession?.let { session ->
            viewModelScope.launch {
                val endTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val completedSession = session.copy(
                    endTime = endTime,
                    isCompleted = true,
                    notes = state.sessionNotes
                )
                
                focusRepository.updateSession(completedSession)
                focusRepository.completeSession(session.id, endTime)
                
                // Update task if linked
                state.selectedTask?.let { task ->
                    // You might want to add focus time to task or mark as worked on
                }
                
                _uiState.value = _uiState.value.copy(
                    currentSession = null,
                    isTimerRunning = false,
                    isPaused = false,
                    remainingSeconds = _uiState.value.totalSeconds,
                    showSessionCompleteDialog = true,
                    sessionNotes = ""
                )
                
                loadTodayStats()
            }
        }
    }
    
    private fun startTimerCountdown() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && _uiState.value.isTimerRunning) {
                delay(1000)
                val newRemaining = _uiState.value.remainingSeconds - 1
                _uiState.value = _uiState.value.copy(remainingSeconds = newRemaining)
                
                if (newRemaining <= 0) {
                    // Timer completed
                    completeSession()
                }
            }
        }
    }
    
    fun updateSessionNotes(notes: String) {
        _uiState.value = _uiState.value.copy(sessionNotes = notes)
    }
    
    fun showTaskPicker() {
        _uiState.value = _uiState.value.copy(showTaskPicker = true)
    }
    
    fun hideTaskPicker() {
        _uiState.value = _uiState.value.copy(showTaskPicker = false)
    }
    
    fun showCustomDurationDialog() {
        _uiState.value = _uiState.value.copy(showCustomDurationDialog = true)
    }
    
    fun hideCustomDurationDialog() {
        _uiState.value = _uiState.value.copy(showCustomDurationDialog = false)
    }
    
    fun hideSessionCompleteDialog() {
        _uiState.value = _uiState.value.copy(showSessionCompleteDialog = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
    
    // Helper functions
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    fun getProgress(): Float {
        val state = _uiState.value
        return if (state.totalSeconds > 0) {
            (state.totalSeconds - state.remainingSeconds).toFloat() / state.totalSeconds
        } else 0f
    }
}
