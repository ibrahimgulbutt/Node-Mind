package com.example.node_mind.presentation.dashboard

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
import com.example.node_mind.presentation.ViewModelFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
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
                // Header with settings
                item {
                    DashboardHeader(
                        onSettingsClick = viewModel::showSettings,
                        onRefreshClick = viewModel::refreshData
                    )
                }
                
                // Stats Overview
                item {
                    StatsOverview(
                        totalStats = uiState.totalStats
                    )
                }
                
                // Weekly Progress Chart
                if (uiState.weeklyStats.isNotEmpty()) {
                    item {
                        WeeklyProgressChart(
                            weeklyStats = uiState.weeklyStats
                        )
                    }
                }
                
                // Achievements
                if (uiState.achievements.isNotEmpty()) {
                    item {
                        AchievementsSection(
                            achievements = uiState.achievements
                        )
                    }
                }
                
                // Recent Activity
                if (uiState.recentActivity.isNotEmpty()) {
                    item {
                        RecentActivitySection(
                            activities = uiState.recentActivity
                        )
                    }
                }
            }
        }
    }
    
    // Settings Dialog
    if (uiState.showSettings) {
        SettingsDialog(
            settings = uiState.settings,
            onSettingsUpdate = viewModel::updateSettings,
            onDismiss = viewModel::hideSettings
        )
    }
}

@Composable
private fun DashboardHeader(
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "Your productivity overview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsOverview(
    totalStats: TotalStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Tasks",
                    value = "${totalStats.completedTasks}/${totalStats.totalTasks}",
                    subtitle = "completed",
                    icon = "✅",
                    progress = if (totalStats.totalTasks > 0) {
                        totalStats.completedTasks.toFloat() / totalStats.totalTasks
                    } else 0f
                )
                
                StatCard(
                    title = "Focus",
                    value = "${totalStats.totalFocusHours}h",
                    subtitle = "total time",
                    icon = "🎯",
                    progress = minOf(totalStats.totalFocusHours.toFloat() / 100f, 1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Notes",
                    value = "${totalStats.totalNotes}",
                    subtitle = "created",
                    icon = "📝",
                    progress = minOf(totalStats.totalNotes.toFloat() / 50f, 1f)
                )
                
                StatCard(
                    title = "Streak",
                    value = "${totalStats.currentStreak}",
                    subtitle = "days",
                    icon = "🔥",
                    progress = minOf(totalStats.currentStreak.toFloat() / 30f, 1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    progress: Float
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = icon, fontSize = 16.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WeeklyProgressChart(
    weeklyStats: List<com.example.node_mind.data.model.DailyStats>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Weekly Progress",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxValue = weeklyStats.maxOfOrNull { it.totalFocusMinutes } ?: 1
                
                weeklyStats.forEachIndexed { index, stat ->
                    val dayName = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index % 7]
                    val height = if (maxValue > 0) {
                        (stat.totalFocusMinutes.toFloat() / maxValue * 80).coerceAtLeast(4f)
                    } else 4f
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(height.dp)
                                .background(
                                    if (stat.totalFocusMinutes > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Focus minutes per day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AchievementsSection(
    achievements: List<Achievement>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementCard(achievement = achievement)
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = achievement.icon,
                fontSize = 32.sp,
                modifier = Modifier.padding(8.dp)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (achievement.isUnlocked) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (achievement.isUnlocked) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (!achievement.isUnlocked) {
                LinearProgressIndicator(
                    progress = achievement.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentActivitySection(
    activities: List<ActivityItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            activities.take(6).forEach { activity ->
                ActivityItem(activity = activity)
                if (activity != activities.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(
    activity: ActivityItem
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = when (activity.type) {
                ActivityType.TASK_COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ActivityType.FOCUS_SESSION -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                ActivityType.NOTE_CREATED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                ActivityType.ACHIEVEMENT_UNLOCKED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = activity.icon, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = activity.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        val timeAgo = try {
            val dateTime = LocalDateTime.parse(activity.timestamp)
            "${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        } catch (e: Exception) {
            "Recent"
        }
        
        Text(
            text = timeAgo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    settings: AppSettings,
    onSettingsUpdate: (AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settings) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsSection(title = "Appearance") {
                        SettingsDropdown(
                            label = "Theme",
                            options = ThemeMode.values().toList(),
                            selectedOption = currentSettings.theme,
                            onOptionSelected = { theme ->
                                currentSettings = currentSettings.copy(theme = theme)
                            },
                            optionText = { it.displayName }
                        )
                    }
                }
                
                item {
                    SettingsSection(title = "Focus") {
                        SettingsSlider(
                            label = "Default Focus Duration",
                            value = currentSettings.defaultFocusDuration,
                            valueRange = 15f..60f,
                            onValueChange = { duration ->
                                currentSettings = currentSettings.copy(defaultFocusDuration = duration.toInt())
                            },
                            valueText = "${currentSettings.defaultFocusDuration}min"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        SettingsSwitch(
                            label = "Auto-start breaks",
                            checked = currentSettings.autoStartBreaks,
                            onCheckedChange = { autoStart ->
                                currentSettings = currentSettings.copy(autoStartBreaks = autoStart)
                            }
                        )
                    }
                }
                
                item {
                    SettingsSection(title = "Notifications") {
                        SettingsSwitch(
                            label = "Enable notifications",
                            checked = currentSettings.enableNotifications,
                            onCheckedChange = { enabled ->
                                currentSettings = currentSettings.copy(enableNotifications = enabled)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        SettingsSwitch(
                            label = "Enable sounds",
                            checked = currentSettings.enableSounds,
                            onCheckedChange = { enabled ->
                                currentSettings = currentSettings.copy(enableSounds = enabled)
                            }
                        )
                    }
                }
                
                item {
                    SettingsSection(title = "Data") {
                        SettingsSwitch(
                            label = "Weekly reports",
                            checked = currentSettings.showWeeklyReports,
                            onCheckedChange = { enabled ->
                                currentSettings = currentSettings.copy(showWeeklyReports = enabled)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        SettingsSwitch(
                            label = "Backup data",
                            checked = currentSettings.backupEnabled,
                            onCheckedChange = { enabled ->
                                currentSettings = currentSettings.copy(backupEnabled = enabled)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSettingsUpdate(currentSettings)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsDropdown(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionText: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = optionText(selectedOption),
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueText: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
