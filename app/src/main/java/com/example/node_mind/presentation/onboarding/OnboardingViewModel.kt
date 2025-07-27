package com.example.node_mind.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.node_mind.data.preferences.PreferencesManager
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setFirstLaunchCompleted()
        }
    }
}
