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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Focus Timer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Session Type Selection
        SessionTypeSelector(
            currentType = uiState.sessionType,
            onTypeSelected = viewModel::selectSessionType
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Timer Display
        TimerDisplay(
            remainingTime = uiState.remainingSeconds * 1000L,
            totalTime = uiState.totalSeconds * 1000L,
            sessionType = uiState.sessionType,
            isRunning = uiState.isTimerRunning,
            isPaused = uiState.isPaused,
            onStart = viewModel::startTimer,
            onPause = viewModel::pauseTimer,
            onResume = viewModel::resumeTimer,
            onStop = viewModel::stopTimer,
            onTimeAdjust = { minutes -> viewModel.setCustomDuration((uiState.totalSeconds / 60) + (minutes / 60000).toInt()) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Current Task
        if (uiState.selectedTask != null) {
            CurrentTaskCard(
                task = uiState.selectedTask!!,
                onRemoveTask = { viewModel.selectTask(null) }
            )
        } else {
            SelectTaskButton(
                tasks = uiState.availableTasks,
                onTaskSelected = viewModel::selectTask
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Recent Sessions
        RecentSessionsCard(
            sessions = uiState.recentSessions,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SessionTypeSelector(
    currentType: SessionType,
    onTypeSelected: (SessionType) -> Unit
) {
    val types = listOf(
        SessionType.FOCUS to "Focus",
        SessionType.SHORT_BREAK to "Short Break", 
        SessionType.LONG_BREAK to "Long Break"
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(types) { (type, label) ->
            FilterChip(
                selected = type == currentType,
                onClick = { onTypeSelected(type) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun TimerDisplay(
    remainingTime: Long,
    totalTime: Long,
    sessionType: SessionType,
    isRunning: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onTimeAdjust: (Long) -> Unit
) {
    val progress = if (totalTime > 0) {
        1f - (remainingTime.toFloat() / totalTime.toFloat())
    } else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timer Ring
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawTimerRing(
                    progress = animatedProgress,
                    sessionType = sessionType
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatTime(remainingTime),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = getSessionColor(sessionType)
                )
                Text(
                    text = sessionType.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Control Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start/Pause/Resume Button
            Button(
                onClick = {
                    when {
                        !isRunning && !isPaused -> onStart()
                        isRunning -> onPause()
                        isPaused -> onResume()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isRunning -> MaterialTheme.colorScheme.secondary
                        isPaused -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                when {
                    isRunning -> {
                        Text("⏸️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause")
                    }
                    isPaused -> {
                        Text("▶️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume")
                    }
                    else -> {
                        Text("▶️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start")
                    }
                }
            }
            
            // Stop Button
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .weight(0.8f)
                    .height(56.dp)
            ) {
                Text("⏹️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Time Adjustment Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onTimeAdjust(-5 * 60 * 1000) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("-5min", fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = { onTimeAdjust(-1 * 60 * 1000) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("-1min", fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = { onTimeAdjust(1 * 60 * 1000) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("+1min", fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = { onTimeAdjust(5 * 60 * 1000) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("+5min", fontSize = 14.sp)
            }
        }
    }
}

private fun DrawScope.drawTimerRing(progress: Float, sessionType: SessionType) {
    val strokeWidth = 12.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    // Background ring
    drawCircle(
        color = Color.Gray.copy(alpha = 0.2f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    
    // Progress ring
    if (progress > 0) {
        drawArc(
            color = when (sessionType) {
                SessionType.FOCUS -> Color(0xFF4CAF50)
                SessionType.SHORT_BREAK -> Color(0xFF2196F3)
                SessionType.LONG_BREAK -> Color(0xFF9C27B0)
            },
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CurrentTaskCard(
    task: Task,
    onRemoveTask: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Task",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            IconButton(onClick = onRemoveTask) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove task",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SelectTaskButton(
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Link a task to this session",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    if (showDialog) {
        TaskSelectionDialog(
            tasks = tasks,
            onTaskSelected = { task ->
                onTaskSelected(task)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TaskSelectionDialog(
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Task") },
        text = {
            if (tasks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📝",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tasks available",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create some tasks first to link them to focus sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(tasks) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onTaskSelected(task) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (task.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (task.category.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = task.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RecentSessionsCard(
    sessions: List<FocusSession>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text("⏰", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (sessions.isEmpty()) {
                Text(
                    text = "No recent sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions.take(5)) { session ->
                        SessionItem(session = session)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItem(session: FocusSession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Session type indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    getSessionColor(session.sessionType),
                    CircleShape
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.sessionType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${session.durationMinutes}m • ${formatDateTime(session.startTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (session.isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun getSessionColor(sessionType: SessionType): Color {
    return when (sessionType) {
        SessionType.FOCUS -> Color(0xFF4CAF50)
        SessionType.SHORT_BREAK -> Color(0xFF2196F3)
        SessionType.LONG_BREAK -> Color(0xFF9C27B0)
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatDateTime(dateTimeString: String): String {
    return try {
        val dateTime = LocalDateTime.parse(dateTimeString)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
        dateTime.format(formatter)
    } catch (e: Exception) {
        dateTimeString.take(10) // Just show first 10 chars if parsing fails
    }
}
