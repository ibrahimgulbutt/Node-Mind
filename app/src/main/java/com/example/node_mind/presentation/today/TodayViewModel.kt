package com.example.node_mind.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.node_mind.data.model.Task
import com.example.node_mind.data.model.TaskPriority
import com.example.node_mind.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class TodayUiState(
    val tasks: List<Task> = emptyList(),
    val completedCount: Int = 0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingTask: Task? = null,
    val taskTitle: String = "",
    val taskDescription: String = "",
    val taskPriority: TaskPriority = TaskPriority.MEDIUM,
    val error: String? = null,
    val successMessage: String? = null
)

class TodayViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    
    init {
        loadTodayTasks()
    }
    
    private fun loadTodayTasks() {
        viewModelScope.launch {
            taskRepository.getTodayTasks()
                .catch { e -> 
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load tasks: ${e.message}"
                    )
                }
                .collect { tasks ->
                    val completedCount = tasks.count { it.isCompleted }
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks.sortedWith(
                            compareBy<Task> { it.isCompleted }
                                .thenByDescending { it.priority.ordinal }
                                .thenBy { it.createdAt }
                        ),
                        completedCount = completedCount,
                        isLoading = false
                    )
                }
        }
    }
    
    fun toggleTaskComplete(taskId: String) {
        viewModelScope.launch {
            try {
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    if (task.isCompleted) {
                        taskRepository.uncompleteTask(taskId)
                    } else {
                        taskRepository.completeTask(taskId)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update task: ${e.message}"
                )
            }
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Task deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete task: ${e.message}"
                )
            }
        }
    }
    
    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingTask = null,
            taskTitle = "",
            taskDescription = "",
            taskPriority = TaskPriority.MEDIUM
        )
    }
    
    fun showEditDialog(task: Task) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingTask = task,
            taskTitle = task.title,
            taskDescription = task.description,
            taskPriority = task.priority
        )
    }
    
    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            editingTask = null,
            taskTitle = "",
            taskDescription = "",
            taskPriority = TaskPriority.MEDIUM
        )
    }
    
    fun updateTaskTitle(title: String) {
        _uiState.value = _uiState.value.copy(taskTitle = title)
    }
    
    fun updateTaskDescription(description: String) {
        _uiState.value = _uiState.value.copy(taskDescription = description)
    }
    
    fun updateTaskPriority(priority: TaskPriority) {
        _uiState.value = _uiState.value.copy(taskPriority = priority)
    }
    
    fun saveTask() {
        val state = _uiState.value
        if (state.taskTitle.isBlank()) return
        
        viewModelScope.launch {
            try {
                if (state.editingTask != null) {
                    // Update existing task
                    val updatedTask = state.editingTask.copy(
                        title = state.taskTitle,
                        description = state.taskDescription,
                        priority = state.taskPriority
                    )
                    taskRepository.updateTask(updatedTask)
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Task updated"
                    )
                } else {
                    // Create new task
                    val newTask = Task(
                        title = state.taskTitle,
                        description = state.taskDescription,
                        priority = state.taskPriority,
                        createdAt = LocalDateTime.now().toString()
                    )
                    taskRepository.insertTask(newTask)
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Task created"
                    )
                }
                hideDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save task: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
