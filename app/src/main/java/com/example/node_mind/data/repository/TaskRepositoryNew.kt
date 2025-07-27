package com.example.node_mind.data.repository

import com.example.node_mind.data.dao.TaskDao
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
    
    // Create operations
    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }
    
    suspend fun createTask(title: String, description: String = ""): Task {
        val task = Task(
            title = title,
            description = description,
            createdAt = LocalDateTime.now().toString()
        )
        taskDao.insertTask(task)
        return task
    }
    
    // Update operations
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
    
    // Delete operations
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
    
    // Task completion
    suspend fun completeTask(taskId: String) {
        val task = taskDao.getTaskById(taskId)
        if (task != null && !task.isCompleted) {
            taskDao.updateTaskCompletion(taskId, true, LocalDateTime.now().toString())
        }
    }
    
    suspend fun uncompleteTask(taskId: String) {
        taskDao.updateTaskCompletion(taskId, false, null)
    }
    
    // Batch operations
    suspend fun deleteCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }
    
    // Statistics
    suspend fun getTodayCompletedCount(): Int {
        return taskDao.getTodayCompletedCount()
    }
}
