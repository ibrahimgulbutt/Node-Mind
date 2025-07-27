package com.example.node_mind.data.repository

import com.example.node_mind.data.database.NodeDao
import com.example.node_mind.data.model.Node
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

class NodeRepository(
    private val nodeDao: NodeDao
) {
    
    fun getAllNodes(): Flow<List<Node>> = nodeDao.getAllNodes()
    
    suspend fun getNodeById(nodeId: String): Node? = nodeDao.getNodeById(nodeId)
    
    fun getNodesByTag(tag: String): Flow<List<Node>> = nodeDao.getNodesByTag(tag)
    
    fun searchNodes(query: String): Flow<List<Node>> = nodeDao.searchNodes(query)
    
    suspend fun insertNode(node: Node) {
        try {
            nodeDao.insertNode(node)
        } catch (e: Exception) {
            throw NodeRepositoryException("Failed to save node: ${e.message}")
        }
    }
    
    suspend fun updateNode(node: Node) {
        try {
            val updatedNode = node.copy(updatedAt = LocalDateTime.now().toString())
            nodeDao.updateNode(updatedNode)
        } catch (e: Exception) {
            throw NodeRepositoryException("Failed to update node: ${e.message}")
        }
    }
    
    suspend fun deleteNode(node: Node) {
        try {
            // Remove this node from all other nodes' connections
            val allNodes = nodeDao.getAllNodes()
            // TODO: Implement proper connection cleanup
            nodeDao.deleteNode(node)
        } catch (e: Exception) {
            throw NodeRepositoryException("Failed to delete node: ${e.message}")
        }
    }
    
    suspend fun connectNodes(fromNodeId: String, toNodeId: String) {
        try {
            val fromNode = nodeDao.getNodeById(fromNodeId) ?: return
            val toNode = nodeDao.getNodeById(toNodeId) ?: return
            
            // Add bidirectional connection
            val fromConnections = fromNode.connectedNodeIds.toMutableList()
            val toConnections = toNode.connectedNodeIds.toMutableList()
            
            if (!fromConnections.contains(toNodeId)) {
                fromConnections.add(toNodeId)
                nodeDao.updateNodeConnections(fromNodeId, fromConnections)
            }
            
            if (!toConnections.contains(fromNodeId)) {
                toConnections.add(fromNodeId)  
                nodeDao.updateNodeConnections(toNodeId, toConnections)
            }
        } catch (e: Exception) {
            throw NodeRepositoryException("Failed to connect nodes: ${e.message}")
        }
    }
    
    suspend fun disconnectNodes(fromNodeId: String, toNodeId: String) {
        try {
            val fromNode = nodeDao.getNodeById(fromNodeId) ?: return
            val toNode = nodeDao.getNodeById(toNodeId) ?: return
            
            // Remove bidirectional connection
            val fromConnections = fromNode.connectedNodeIds.toMutableList()
            val toConnections = toNode.connectedNodeIds.toMutableList()
            
            fromConnections.remove(toNodeId)
            toConnections.remove(fromNodeId)
            
            nodeDao.updateNodeConnections(fromNodeId, fromConnections)
            nodeDao.updateNodeConnections(toNodeId, toConnections)
        } catch (e: Exception) {
            throw NodeRepositoryException("Failed to disconnect nodes: ${e.message}")
        }
    }
    
    suspend fun updateNodePosition(nodeId: String, x: Float, y: Float) {
        try {
            nodeDao.updateNodePosition(nodeId, x, y)
        } catch (e: Exception) {
            throw NodeRepositoryException("Failed to update node position: ${e.message}")
        }
    }
    
    suspend fun getAllTags(): List<String> {
        return try {
            val tagLists = nodeDao.getAllTagsRaw()
            val allTags = mutableSetOf<String>()
            tagLists.forEach { tagListJson ->
                try {
                    val tags = Json.decodeFromString<List<String>>(tagListJson)
                    allTags.addAll(tags)
                } catch (e: Exception) {
                    // Skip malformed tag lists
                }
            }
            allTags.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class NodeRepositoryException(message: String) : Exception(message)
