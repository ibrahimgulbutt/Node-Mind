package com.example.node_mind.presentation.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.node_mind.data.model.Node
import com.example.node_mind.data.repository.NodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class NodesViewModel(
    private val nodeRepository: NodeRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()
    
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()
    
    private val _uiState = MutableStateFlow(NodesUiState())
    val uiState = _uiState.asStateFlow()
    
    // Get all nodes based on search and tag filters
    val nodes = combine(
        nodeRepository.getAllNodes(),
        searchQuery,
        selectedTag
    ) { allNodes, query, tag ->
        var filteredNodes = allNodes
        
        // Filter by tag if selected
        if (tag != null) {
            filteredNodes = filteredNodes.filter { node ->
                node.tags.contains(tag)
            }
        }
        
        // Filter by search query
        if (query.isNotBlank()) {
            filteredNodes = filteredNodes.filter { node ->
                node.title.contains(query, ignoreCase = true) ||
                node.content.contains(query, ignoreCase = true) ||
                node.tags.any { it.contains(query, ignoreCase = true) }
            }
        }
        
        filteredNodes.sortedByDescending { it.updatedAt }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Get all available tags
    val availableTags = flow {
        emit(nodeRepository.getAllTags())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }
    
    fun showAddDialog() {
        _showAddDialog.value = true
    }
    
    fun hideAddDialog() {
        _showAddDialog.value = false
        _uiState.value = NodesUiState() // Reset form state
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
    
    fun createNode() {
        val state = _uiState.value
        if (state.nodeTitle.isBlank()) return
        
        viewModelScope.launch {
            try {
                val tags = state.nodeTags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                
                val node = Node(
                    title = state.nodeTitle,
                    content = state.nodeContent,
                    tags = tags,
                    createdAt = LocalDateTime.now().toString(),
                    updatedAt = LocalDateTime.now().toString()
                )
                
                nodeRepository.insertNode(node)
                hideAddDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create node: ${e.message}"
                )
            }
        }
    }
    
    fun deleteNode(node: Node) {
        viewModelScope.launch {
            try {
                nodeRepository.deleteNode(node)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete node: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class NodesUiState(
    val nodeTitle: String = "",
    val nodeContent: String = "",
    val nodeTags: String = "",
    val error: String? = null
)
