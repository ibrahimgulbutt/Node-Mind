package com.example.node_mind.data.database

import androidx.room.*
import com.example.node_mind.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE DATE(createdAt) = DATE('now', 'localtime') ORDER BY priority DESC")
    fun getTodayTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, createdAt ASC")
    fun getPendingTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?
    
    @Query("SELECT * FROM tasks WHERE tags LIKE '%' || :tag || '%'")
    fun getTasksByTag(tag: String): Flow<List<Task>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete  
    suspend fun deleteTask(task: Task)
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean, completedAt: String?)
    
    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()
    
    @Query("SELECT COUNT(*) FROM tasks WHERE DATE(createdAt) = DATE('now', 'localtime') AND isCompleted = 1")
    suspend fun getTodayCompletedCount(): Int
}
