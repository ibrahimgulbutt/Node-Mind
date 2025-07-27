package com.example.node_mind.presentation.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.node_mind.data.model.FocusSession
import com.example.node_mind.data.model.SessionType
import com.example.node_mind.data.model.Task
import com.example.node_mind.presentation.ViewModelFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            viewModel.clearError()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with stats
                item {
                    FocusHeader(
                        todayStats = uiState.todayStats,
                        totalFocusTime = uiState.totalFocusTime,
                        currentStreak = uiState.currentStreak
                    )
                }
                
                // Timer Section
                item {
                    TimerSection(
                        uiState = uiState,
                        onSessionTypeSelect = viewModel::selectSessionType,
                        onCustomDurationClick = viewModel::showCustomDurationDialog,
                        onTaskSelect = viewModel::showTaskPicker,
                        onStartTimer = viewModel::startTimer,
                        onPauseTimer = viewModel::pauseTimer,
                        onResumeTimer = viewModel::resumeTimer,
                        onStopTimer = viewModel::stopTimer,
                        formatTime = viewModel::formatTime,
                        getProgress = viewModel::getProgress
                    )
                }
                
                // Recent Sessions
                if (uiState.recentSessions.isNotEmpty()) {
                    item {
                        RecentSessionsSection(
                            sessions = uiState.recentSessions
                        )
                    }
                }
            }
        }
    }
    
    // Task Picker Dialog
    if (uiState.showTaskPicker) {
        TaskPickerDialog(
            tasks = uiState.availableTasks,
            selectedTask = uiState.selectedTask,
            onTaskSelect = viewModel::selectTask,
            onDismiss = viewModel::hideTaskPicker
        )
    }
    
    // Custom Duration Dialog
    if (uiState.showCustomDurationDialog) {
        CustomDurationDialog(
            currentDuration = uiState.customDuration,
            onDurationSet = { duration ->
                viewModel.setCustomDuration(duration)
                viewModel.hideCustomDurationDialog()
            },
            onDismiss = viewModel::hideCustomDurationDialog
        )
    }
    
    // Session Complete Dialog
    if (uiState.showSessionCompleteDialog) {
        SessionCompleteDialog(
            sessionType = uiState.sessionType,
            duration = uiState.totalSeconds / 60,
            notes = uiState.sessionNotes,
            onNotesChange = viewModel::updateSessionNotes,
            onDismiss = viewModel::hideSessionCompleteDialog
        )
    }
}

@Composable
private fun FocusHeader(
    todayStats: FocusStats,
    totalFocusTime: Int,
    currentStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Focus Session",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "Stay productive with Pomodoro",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎯",
                            fontSize = 24.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Today",
                    value = "${todayStats.sessionsCompleted}",
                    subtitle = "sessions"
                )
                
                StatItem(
                    label = "Minutes",
                    value = "${todayStats.totalMinutes}",
                    subtitle = "focused"
                )
                
                StatItem(
                    label = "Streak",
                    value = "$currentStreak",
                    subtitle = "days"
                )
                
                StatItem(
                    label = "Total",
                    value = "${totalFocusTime / 60}h",
                    subtitle = "all time"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun TimerSection(
    uiState: FocusUiState,
    onSessionTypeSelect: (SessionType) -> Unit,
    onCustomDurationClick: () -> Unit,
    onTaskSelect: () -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    formatTime: (Int) -> String,
    getProgress: () -> Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Session Type Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SessionType.values()) { sessionType ->
                    SessionTypeChip(
                        sessionType = sessionType,
                        isSelected = uiState.sessionType == sessionType,
                        isEnabled = !uiState.isTimerRunning,
                        onClick = { onSessionTypeSelect(sessionType) }
                    )
                }
                
                item {
                    OutlinedButton(
                        onClick = onCustomDurationClick,
                        enabled = !uiState.isTimerRunning && uiState.sessionType == SessionType.FOCUS,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "${uiState.customDuration}m",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Timer Display
            TimerDisplay(
                timeText = formatTime(uiState.remainingSeconds),
                progress = getProgress(),
                isRunning = uiState.isTimerRunning
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Selected Task
            uiState.selectedTask?.let { task ->
                TaskCard(
                    task = task,
                    onClick = onTaskSelect,
                    enabled = !uiState.isTimerRunning
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            } ?: run {
                OutlinedButton(
                    onClick = onTaskSelect,
                    enabled = !uiState.isTimerRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link to Task (Optional)")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Timer Controls
            TimerControls(
                isRunning = uiState.isTimerRunning,
                isPaused = uiState.isPaused,
                onStart = onStartTimer,
                onPause = onPauseTimer,
                onResume = onResumeTimer,
                onStop = onStopTimer
            )
        }
    }
}

@Composable
private fun SessionTypeChip(
    sessionType: SessionType,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        enabled = isEnabled,
        label = {
            Text(
                text = sessionType.displayName,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            val icon = when (sessionType) {
                SessionType.FOCUS -> "🎯"
                SessionType.SHORT_BREAK -> "☕"
                SessionType.LONG_BREAK -> "🏖️"
            }
            Text(text = icon, fontSize = 14.sp)
        }
    )
}

@Composable
private fun TimerDisplay(
    timeText: String,
    progress: Float,
    isRunning: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300), label = ""
    )
    
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTimerRing(
                progress = animatedProgress,
                isRunning = isRunning
            )
        }
        
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun DrawScope.drawTimerRing(
    progress: Float,
    isRunning: Boolean
) {
    val strokeWidth = 8.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    // Background ring
    drawCircle(
        color = Color.Gray.copy(alpha = 0.2f),
        radius = radius,
        center = center,
        style = Stroke(strokeWidth)
    )
    
    // Progress ring
    if (progress > 0) {
        val sweepAngle = 360f * progress
        drawArc(
            color = if (isRunning) Color(0xFF4CAF50) else Color(0xFF2196F3),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = Color(android.graphics.Color.parseColor(task.priority.colorHex))
            ) {}
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Change task",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            !isRunning && !isPaused -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Focus")
                }
            }
            
            isRunning -> {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause")
                }
                
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }
            
            isPaused -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resume")
                }
                
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
private fun RecentSessionsSection(
    sessions: List<FocusSession>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            sessions.take(5).forEach { session ->
                SessionItem(session = session)
                if (session != sessions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionItem(
    session: FocusSession
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = when (session.sessionType) {
                SessionType.FOCUS -> MaterialTheme.colorScheme.primary
                SessionType.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
                SessionType.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
            }.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val icon = when (session.sessionType) {
                    SessionType.FOCUS -> "🎯"
                    SessionType.SHORT_BREAK -> "☕"
                    SessionType.LONG_BREAK -> "🏖️"
                }
                Text(text = icon, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.sessionType.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            
            val startTime = try {
                LocalDateTime.parse(session.startTime).format(
                    DateTimeFormatter.ofPattern("HH:mm")
                )
            } catch (e: Exception) {
                "Unknown time"
            }
            
            Text(
                text = "$startTime • ${session.durationMinutes}min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (session.isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "In progress",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskPickerDialog(
    tasks: List<Task>,
    selectedTask: Task?,
    onTaskSelect: (Task?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Task")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTaskSelect(null) },
                        colors = if (selectedTask == null) {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            CardDefaults.cardColors()
                        }
                    ) {
                        Text(
                            text = "No task (Free focus)",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                items(tasks) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTaskSelect(task) },
                        colors = if (selectedTask?.id == task.id) {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            CardDefaults.cardColors()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = CircleShape,
                                color = Color(android.graphics.Color.parseColor(task.priority.colorHex))
                            ) {}
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                
                                if (task.description.isNotBlank()) {
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDurationDialog(
    currentDuration: Int,
    onDurationSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var duration by remember { mutableStateOf(currentDuration.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Custom Duration")
        },
        text = {
            OutlinedTextField(
                value = duration,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } && value.length <= 3) {
                        duration = value
                    }
                },
                label = { Text("Minutes") },
                suffix = { Text("min") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = duration.toIntOrNull()
                    if (minutes != null && minutes > 0) {
                        onDurationSet(minutes)
                    }
                },
                enabled = duration.toIntOrNull()?.let { it > 0 } == true
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCompleteDialog(
    sessionType: SessionType,
    duration: Int,
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Session Complete! 🎉")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Great job! You completed a ${duration}min ${sessionType.displayName.lowercase()} session.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Session notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
