package com.example.node_mind.data.dao

import androidx.room.*
import com.example.node_mind.data.model.Node
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
    
    @Query("SELECT * FROM nodes ORDER BY updatedAt DESC")
    fun getAllNodes(): Flow<List<Node>>
    
    @Query("SELECT * FROM nodes WHERE id = :nodeId")
    suspend fun getNodeById(nodeId: String): Node?
    
    @Query("SELECT * FROM nodes WHERE tags LIKE '%' || :tag || '%'")
    fun getNodesByTag(tag: String): Flow<List<Node>>
    
    @Query("SELECT * FROM nodes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNodes(query: String): Flow<List<Node>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: Node)
    
    @Update
    suspend fun updateNode(node: Node)
    
    @Delete
    suspend fun deleteNode(node: Node)
    
    @Query("UPDATE nodes SET connectedNodeIds = :connectedIds WHERE id = :nodeId")
    suspend fun updateNodeConnections(nodeId: String, connectedIds: List<String>)
    
    @Query("UPDATE nodes SET positionX = :x, positionY = :y WHERE id = :nodeId")
    suspend fun updateNodePosition(nodeId: String, x: Float, y: Float)
    
    @Query("SELECT tags FROM nodes")
    suspend fun getAllTagsRaw(): List<String>
}