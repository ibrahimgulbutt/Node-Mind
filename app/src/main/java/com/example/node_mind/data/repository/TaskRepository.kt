package com.example.node_mind.data.repository

import com.example.node_mind.data.database.TaskDao
import com.example.node_mind.data.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class TaskRepository(
    private val taskDao: TaskDao
) {
    
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    
    fun getTodayTasks(): Flow<List<Task>> = taskDao.getTodayTasks()
    
    fun getPendingTasks(): Flow<List<Task>> = taskDao.getPendingTasks()
    
    suspend fun getTaskById(taskId: String): Task? = taskDao.getTaskById(taskId)
    
    fun getTasksByTag(tag: String): Flow<List<Task>> = taskDao.getTasksByTag(tag)
    
    suspend fun insertTask(task: Task) {
        try {
            taskDao.insertTask(task)
        } catch (e: Exception) {
            throw TaskRepositoryException("Failed to save task: ${e.message}")
        }
    }
    
    suspend fun updateTask(task: Task) {
        try {
            taskDao.updateTask(task)
        } catch (e: Exception) {
            throw TaskRepositoryException("Failed to update task: ${e.message}")
        }
    }
    
    suspend fun deleteTask(task: Task) {
        try {
            taskDao.deleteTask(task)
        } catch (e: Exception) {
            throw TaskRepositoryException("Failed to delete task: ${e.message}")
        }
    }
    
    suspend fun toggleTaskCompletion(taskId: String) {
        try {
            val task = taskDao.getTaskById(taskId) ?: return
            val isCompleted = !task.isCompleted
            val completedAt = if (isCompleted) LocalDateTime.now().toString() else null
            taskDao.updateTaskCompletion(taskId, isCompleted, completedAt)
        } catch (e: Exception) {
            throw TaskRepositoryException("Failed to toggle task completion: ${e.message}")
        }
    }
    
    suspend fun deleteCompletedTasks() {
        try {
            taskDao.deleteCompletedTasks()
        } catch (e: Exception) {
            throw TaskRepositoryException("Failed to delete completed tasks: ${e.message}")
        }
    }
    
    suspend fun getTodayCompletedCount(): Int {
        return try {
            taskDao.getTodayCompletedCount()
        } catch (e: Exception) {
            0
        }
    }
}

class TaskRepositoryException(message: String) : Exception(message)
