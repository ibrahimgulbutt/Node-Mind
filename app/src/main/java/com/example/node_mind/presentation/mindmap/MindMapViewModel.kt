package com.example.node_mind.presentation.mindmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.node_mind.data.model.Node
import com.example.node_mind.data.repository.NodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.*

data class MindMapUiState(
    val nodePositions: List<NodePosition> = emptyList(),
    val selectedNode: NodePosition? = null,
    val selectedNodeIds: Set<String> = emptySet(),
    val connections: List<Connection> = emptyList(),
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingNode: Node? = null,
    val nodeTitle: String = "",
    val nodeContent: String = "",
    val nodeTags: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDragging: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class NodePosition(
    val node: Node,
    val x: Float,
    val y: Float,
    val isSelected: Boolean = false
)

data class Connection(
    val fromNodeId: String,
    val toNodeId: String,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float
)

class MindMapViewModel(
    private val nodeRepository: NodeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MindMapUiState())
    val uiState: StateFlow<MindMapUiState> = _uiState.asStateFlow()
    
    init {
        loadMindMap()
    }
    
    private fun loadMindMap() {
        viewModelScope.launch {
            nodeRepository.getAllNodes()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load mind map: ${e.message}"
                    )
                }
                .collect { nodes ->
                    val nodePositions = nodes.mapIndexed { index, node ->
                        // Create circular layout if node doesn't have position
                        val angle = (2 * PI * index) / nodes.size
                        val radius = 200f + (nodes.size * 10f)
                        val x = if (node.positionX != 0f) node.positionX else (cos(angle) * radius).toFloat() + 400f
                        val y = if (node.positionY != 0f) node.positionY else (sin(angle) * radius).toFloat() + 400f
                        
                        NodePosition(node, x, y)
                    }
                    
                    val connections = buildConnections(nodePositions)
                    
                    _uiState.value = _uiState.value.copy(
                        nodePositions = nodePositions,
                        connections = connections,
                        isLoading = false
                    )
                }
        }
    }
    
    private fun buildConnections(nodePositions: List<NodePosition>): List<Connection> {
        val connections = mutableListOf<Connection>()
        
        nodePositions.forEach { fromNode ->
            fromNode.node.connectedNodeIds.forEach { toNodeId ->
                val toNode = nodePositions.find { it.node.id == toNodeId }
                if (toNode != null) {
                    connections.add(
                        Connection(
                            fromNodeId = fromNode.node.id,
                            toNodeId = toNode.node.id,
                            fromX = fromNode.x,
                            fromY = fromNode.y,
                            toX = toNode.x,
                            toY = toNode.y
                        )
                    )
                }
            }
        }
        
        return connections
    }
    
    fun selectNode(nodePosition: NodePosition) {
        val updatedNodes = _uiState.value.nodePositions.map { node ->
            node.copy(isSelected = node.node.id == nodePosition.node.id)
        }
        _uiState.value = _uiState.value.copy(
            nodePositions = updatedNodes,
            selectedNode = if (_uiState.value.selectedNode?.node?.id == nodePosition.node.id) {
                null
            } else {
                nodePosition.copy(isSelected = true)
            }
        )
    }
    
    fun moveNode(nodeId: String, newX: Float, newY: Float) {
        viewModelScope.launch {
            try {
                // Update in-memory state immediately for responsive UI
                val updatedNodes = _uiState.value.nodePositions.map { nodePos ->
                    if (nodePos.node.id == nodeId) {
                        nodePos.copy(x = newX, y = newY)
                    } else {
                        nodePos
                    }
                }
                
                val updatedConnections = buildConnections(updatedNodes)
                
                _uiState.value = _uiState.value.copy(
                    nodePositions = updatedNodes,
                    connections = updatedConnections
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to move node: ${e.message}"
                )
            }
        }
    }
    
    fun connectNodes(fromNodeId: String, toNodeId: String) {
        if (fromNodeId == toNodeId) return
        
        viewModelScope.launch {
            try {
                nodeRepository.connectNodes(fromNodeId, toNodeId)
                loadMindMap() // Reload to get updated connections
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to connect nodes: ${e.message}"
                )
            }
        }
    }
    
    fun disconnectNodes(fromNodeId: String, toNodeId: String) {
        viewModelScope.launch {
            try {
                nodeRepository.disconnectNodes(fromNodeId, toNodeId)
                loadMindMap() // Reload to get updated connections
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to disconnect nodes: ${e.message}"
                )
            }
        }
    }
    
    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingNode = null,
            nodeTitle = "",
            nodeContent = "",
            nodeTags = ""
        )
    }
    
    fun showEditDialog(node: Node) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            editingNode = node,
            nodeTitle = node.title,
            nodeContent = node.content,
            nodeTags = node.tags.joinToString(", ")
        )
    }
    
    fun hideDialogs() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            showEditDialog = false,
            editingNode = null,
            nodeTitle = "",
            nodeContent = "",
            nodeTags = ""
        )
    }
    
    fun updateNodeTitle(title: String) {
        _uiState.value = _uiState.value.copy(nodeTitle = title)
    }
    
    fun updateNodeContent(content: String) {
        _uiState.value = _uiState.value.copy(nodeContent = content)
    }
    
    fun updateNodeTags(tags: String) {
        _uiState.value = _uiState.value.copy(nodeTags = tags)
    }
    
    fun saveNode() {
        val state = _uiState.value
        if (state.nodeTitle.isBlank()) return
        
        viewModelScope.launch {
            try {
                val tags = state.nodeTags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                
                if (state.editingNode != null) {
                    // Update existing node
                    val updatedNode = state.editingNode.copy(
                        title = state.nodeTitle,
                        content = state.nodeContent,
                        tags = tags,
                        updatedAt = LocalDateTime.now().toString()
                    )
                    nodeRepository.updateNode(updatedNode)
                } else {
                    // Create new node
                    val newNode = Node(
                        title = state.nodeTitle,
                        content = state.nodeContent,
                        tags = tags,
                        positionX = 400f + (0..200).random(), // Random position near center
                        positionY = 400f + (0..200).random(),
                        createdAt = LocalDateTime.now().toString(),
                        updatedAt = LocalDateTime.now().toString()
                    )
                    nodeRepository.insertNode(newNode)
                }
                
                hideDialogs()
                loadMindMap() // Reload the mind map
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save node: ${e.message}"
                )
            }
        }
    }
    
    fun deleteNode(node: Node) {
        viewModelScope.launch {
            try {
                nodeRepository.deleteNode(node)
                loadMindMap() // Reload the mind map
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete node: ${e.message}"
                )
            }
        }
    }
    
    fun updateScale(newScale: Float) {
        _uiState.value = _uiState.value.copy(
            scale = newScale.coerceIn(0.5f, 3f)
        )
    }
    
    fun updateOffset(deltaX: Float, deltaY: Float) {
        _uiState.value = _uiState.value.copy(
            offsetX = _uiState.value.offsetX + deltaX,
            offsetY = _uiState.value.offsetY + deltaY
        )
    }
    
    fun centerMap() {
        _uiState.value = _uiState.value.copy(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f
        )
    }
    
    fun refreshData() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadMindMap()
    }
    
    fun saveMindMap() {
        viewModelScope.launch {
            try {
                // Update all nodes with their current positions
                _uiState.value.nodePositions.forEach { nodePosition ->
                    val updatedNode = nodePosition.node.copy(
                        positionX = nodePosition.x,
                        positionY = nodePosition.y,
                        updatedAt = LocalDateTime.now().toString()
                    )
                    nodeRepository.updateNode(updatedNode)
                }
                
                // Show success feedback
                _uiState.value = _uiState.value.copy(
                    error = "Mind map saved successfully!"
                )
                // Clear success message after delay
                kotlinx.coroutines.delay(2000)
                clearError()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save mind map: ${e.message}"
                )
            }
        }
    }
    
    fun deleteSelectedNodes() {
        viewModelScope.launch {
            try {
                val selectedNodes = _uiState.value.nodePositions.filter { it.isSelected }
                selectedNodes.forEach { nodePosition ->
                    nodeRepository.deleteNode(nodePosition.node)
                }
                
                // Refresh data to update UI
                loadMindMap()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete nodes: ${e.message}"
                )
            }
        }
    }
    
    fun resetView() {
        _uiState.value = _uiState.value.copy(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            selectedNode = null,
            nodePositions = _uiState.value.nodePositions.map { it.copy(isSelected = false) }
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
