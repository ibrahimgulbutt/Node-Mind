package com.example.node_mind.di

import android.content.Context
import com.example.node_mind.data.database.NodeMindDatabase
import com.example.node_mind.data.preferences.PreferencesManager
import com.example.node_mind.data.repository.NodeRepository
import com.example.node_mind.data.repository.TaskRepository

// Simple dependency container - in a real app you'd use Hilt/Dagger
class AppContainer(private val context: Context) {
    
    // Database
    private val database by lazy {
        NodeMindDatabase.getDatabase(context)
    }
    
    // Preferences
    val preferencesManager by lazy {
        PreferencesManager(context)
    }
    
    // Repositories
    val taskRepository by lazy {
        TaskRepository(database.taskDao())
    }
    
    val nodeRepository by lazy {
        NodeRepository(database.nodeDao())
    }
    
    // ViewModels would be created here in a real app
    // For now we'll use simple constructor injection
}
