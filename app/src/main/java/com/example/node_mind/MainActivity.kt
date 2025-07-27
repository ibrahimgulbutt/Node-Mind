package com.example.node_mind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.node_mind.data.preferences.ThemeMode
import com.example.node_mind.presentation.ViewModelFactory
import com.example.node_mind.presentation.home.HomeScreen
import com.example.node_mind.presentation.onboarding.OnboardingScreen
import com.example.node_mind.presentation.splash.SplashScreen
import com.example.node_mind.ui.theme.NodeMindTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    
    private val app by lazy { application as NodeMindApplication }
    private val viewModelFactory by lazy { ViewModelFactory(app.container) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val themeMode by app.container.preferencesManager.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            
            NodeMindTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NodeMindApp(viewModelFactory)
                }
            }
        }
    }
}

@Composable
private fun NodeMindApp(
    viewModelFactory: ViewModelFactory
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate("onboarding") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable("home") {
            HomeScreen(viewModelFactory)
        }
    }
}