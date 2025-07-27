package com.example.node_mind.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.node_mind.presentation.ViewModelFactory
import com.example.node_mind.presentation.dashboard.DashboardScreen
import com.example.node_mind.presentation.focus.FocusScreen
import com.example.node_mind.presentation.mindmap.MindMapScreen
import com.example.node_mind.presentation.mindmap.MindMapViewModel
import com.example.node_mind.presentation.nodes.NodesScreen
import com.example.node_mind.presentation.nodes.NodesViewModel
import com.example.node_mind.presentation.today.TodayScreen
import com.example.node_mind.presentation.today.TodayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModelFactory: ViewModelFactory,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            NodeMindBottomNavigation(
                navController = navController
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("today") {
                TodayScreen(
                    viewModel = viewModel<TodayViewModel>(factory = viewModelFactory)
                )
            }
            
            composable("nodes") {
                NodesScreen(
                    viewModel = viewModel<NodesViewModel>(factory = viewModelFactory)
                )
            }
            
            composable("mindmap") {
                MindMapScreen(
                    viewModel = viewModel<MindMapViewModel>(factory = viewModelFactory)
                )
            }
            
            composable("focus") {
                FocusScreen(
                    viewModel = viewModel<com.example.node_mind.presentation.focus.FocusViewModel>(factory = viewModelFactory)
                )
            }
            
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel<com.example.node_mind.presentation.dashboard.DashboardViewModel>(factory = viewModelFactory)
                )
            }
        }
    }
}

@Composable
private fun NodeMindBottomNavigation(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        // Today
        NavigationBarItem(
            selected = currentDestination?.route == "today",
            onClick = {
                navController.navigate("today") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Text(text = "🗓️", fontSize = 20.sp)
            },
            label = {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (currentDestination?.route == "today") FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        // Nodes
        NavigationBarItem(
            selected = currentDestination?.route == "nodes",
            onClick = {
                navController.navigate("nodes") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Text(text = "🧠", fontSize = 20.sp)
            },
            label = {
                Text(
                    text = "Nodes",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (currentDestination?.route == "nodes") FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        // Mind Map
        NavigationBarItem(
            selected = currentDestination?.route == "mindmap",
            onClick = {
                navController.navigate("mindmap") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Text(text = "🧬", fontSize = 20.sp)
            },
            label = {
                Text(
                    text = "Mind Map",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (currentDestination?.route == "mindmap") FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        // Focus
        NavigationBarItem(
            selected = currentDestination?.route == "focus",
            onClick = {
                navController.navigate("focus") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Text(text = "⏰", fontSize = 20.sp)
            },
            label = {
                Text(
                    text = "Focus",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (currentDestination?.route == "focus") FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        // Dashboard
        NavigationBarItem(
            selected = currentDestination?.route == "dashboard",
            onClick = {
                navController.navigate("dashboard") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Text(text = "📊", fontSize = 20.sp)
            },
            label = {
                Text(
                    text = "Stats",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (currentDestination?.route == "dashboard") FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
