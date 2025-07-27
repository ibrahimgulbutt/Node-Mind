package com.example.node_mind.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.node_mind.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SplashUiState(
    val isFirstLaunch: Boolean = true,
    val isLoading: Boolean = true
)

class SplashViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    
    init {
        checkFirstLaunch()
    }
    
    private fun checkFirstLaunch() {
        viewModelScope.launch {
            preferencesManager.isFirstLaunch
                .collect { isFirst ->
                    _uiState.value = _uiState.value.copy(
                        isFirstLaunch = isFirst,
                        isLoading = false
                    )
                }
        }
    }
}
