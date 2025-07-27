package com.example.node_mind.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "tasks")
@Serializable
data class Task(
    @PrimaryKey
    val id: String = generateId(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "",
    val reminderDateTime: String? = null, // ISO string format
    val repeatType: RepeatType = RepeatType.NONE,
    val createdAt: String = LocalDateTime.now().toString(),
    val completedAt: String? = null,
    val tags: List<String> = emptyList()
) {
    companion object {
        fun generateId(): String = "task_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

enum class TaskPriority(val displayName: String, val colorHex: String) {
    LOW("Low", "#4CAF50"),      // Green
    MEDIUM("Medium", "#FF9800"), // Orange  
    HIGH("High", "#F44336")     // Red
}

enum class RepeatType(val displayName: String) {
    NONE("Never"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}
