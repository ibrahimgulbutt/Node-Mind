package com.example.node_mind.data.repository

import com.example.node_mind.data.dao.NodeDao
import com.example.node_mind.data.model.Node
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class NodeRepository(
    private val nodeDao: NodeDao
) {
    
    fun getAllNodes(): Flow<List<Node>> = nodeDao.getAllNodes()
    
    suspend fun getNodeById(nodeId: String): Node? = nodeDao.getNodeById(nodeId)
    
    fun getNodesByTag(tag: String): Flow<List<Node>> = nodeDao.getNodesByTag(tag)
    
    fun searchNodes(query: String): Flow<List<Node>> = nodeDao.searchNodes(query)
    
    // Create operations
    suspend fun insertNode(node: Node): Long {
        return nodeDao.insertNode(node)
    }
    
    suspend fun createNode(title: String, content: String = "", tags: List<String> = emptyList()): Node {
        val node = Node(
            title = title,
            content = content,
            tags = tags,
            createdAt = LocalDateTime.now().toString()
        )
        nodeDao.insertNode(node)
        return node
    }
    
    // Update operations
    suspend fun updateNode(node: Node) {
        nodeDao.updateNode(node)
    }
    
    // Delete operations
    suspend fun deleteNode(node: Node) {
        nodeDao.deleteNode(node)
    }
    
    suspend fun deleteNodeById(nodeId: String) {
        val node = nodeDao.getNodeById(nodeId)
        if (node != null) {
            nodeDao.deleteNode(node)
        }
    }
    
    // Connection operations (simplified for now)
    suspend fun connectNodes(fromNodeId: String, toNodeId: String) {
        val fromNode = nodeDao.getNodeById(fromNodeId)
        val toNode = nodeDao.getNodeById(toNodeId)
        
        if (fromNode != null && toNode != null) {
            val updatedFromNode = fromNode.copy(
                connectedNodeIds = (fromNode.connectedNodeIds + toNodeId).distinct()
            )
            val updatedToNode = toNode.copy(
                connectedNodeIds = (toNode.connectedNodeIds + fromNodeId).distinct()
            )
            
            nodeDao.updateNode(updatedFromNode)
            nodeDao.updateNode(updatedToNode)
        }
    }
    
    suspend fun disconnectNodes(fromNodeId: String, toNodeId: String) {
        val fromNode = nodeDao.getNodeById(fromNodeId)
        val toNode = nodeDao.getNodeById(toNodeId)
        
        if (fromNode != null && toNode != null) {
            val updatedFromNode = fromNode.copy(
                connectedNodeIds = fromNode.connectedNodeIds.filter { it != toNodeId }
            )
            val updatedToNode = toNode.copy(
                connectedNodeIds = toNode.connectedNodeIds.filter { it != fromNodeId }
            )
            
            nodeDao.updateNode(updatedFromNode)
            nodeDao.updateNode(updatedToNode)
        }
    }
    
    // Get all unique tags
    fun getAllTags(): Flow<List<String>> {
        return nodeDao.getAllNodes().map { nodes ->
            nodes.flatMap { it.tags }.distinct().sorted()
        }
    }
}
