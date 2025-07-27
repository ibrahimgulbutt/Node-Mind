package com.example.node_mind.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.node_mind.di.AppContainer
import com.example.node_mind.presentation.mindmap.MindMapViewModel
import com.example.node_mind.presentation.nodes.NodesViewModel
import com.example.node_mind.presentation.onboarding.OnboardingViewModel
import com.example.node_mind.presentation.splash.SplashViewModel
import com.example.node_mind.presentation.today.TodayViewModel

class ViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            SplashViewModel::class.java -> {
                SplashViewModel(appContainer.preferencesManager) as T
            }
            OnboardingViewModel::class.java -> {
                OnboardingViewModel(appContainer.preferencesManager) as T
            }
            TodayViewModel::class.java -> {
                TodayViewModel(appContainer.taskRepository) as T
            }
            NodesViewModel::class.java -> {
                NodesViewModel(appContainer.nodeRepository) as T
            }
            MindMapViewModel::class.java -> {
                MindMapViewModel(appContainer.nodeRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
