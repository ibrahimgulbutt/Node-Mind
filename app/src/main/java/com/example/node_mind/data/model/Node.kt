package com.example.node_mind.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "nodes")
@Serializable
data class Node(
    @PrimaryKey
    val id: String = generateId(),
    val title: String,
    val content: String = "",
    val emoji: String = "💡",
    val tags: List<String> = emptyList(),
    val connectedNodeIds: List<String> = emptyList(),
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString(),
    val isMarkdown: Boolean = true
) {
    companion object {
        fun generateId(): String = "node_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

@Serializable
data class NodeConnection(
    val fromNodeId: String,
    val toNodeId: String,
    val connectionType: ConnectionType = ConnectionType.RELATED
)

enum class ConnectionType(val displayName: String) {
    RELATED("Related"),
    CAUSES("Causes"),
    DEPENDS_ON("Depends On"),
    SIMILAR("Similar")
}
