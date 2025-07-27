package com.example.node_mind.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    
    @Serializable
    data object Splash : Destination
    
    @Serializable
    data object Onboarding : Destination
    
    @Serializable
    data object Home : Destination
    
    // Main tabs in Home
    @Serializable
    data object Today : Destination
    
    @Serializable
    data object Nodes : Destination
    
    @Serializable
    data object MindMap : Destination
    
    @Serializable
    data object Focus : Destination
    
    @Serializable
    data object Dashboard : Destination
    
    // Detail screens
    @Serializable
    data class AddEditTask(val taskId: String? = null) : Destination
    
    @Serializable
    data class AddEditNode(val nodeId: String? = null) : Destination
    
    @Serializable
    data class NodeDetail(val nodeId: String) : Destination
    
    @Serializable
    data object Settings : Destination
}

// Bottom navigation items
enum class BottomNavItem(
    val route: Destination,
    val title: String,
    val icon: String // We'll use emoji for now, can be replaced with vector icons
) {
    TODAY(Destination.Today, "Today", "🗓️"),
    NODES(Destination.Nodes, "Nodes", "🧠"),
    MIND_MAP(Destination.MindMap, "Mind Map", "🧬"),
    FOCUS(Destination.Focus, "Focus", "⏰"),
    DASHBOARD(Destination.Dashboard, "Stats", "📊")
}
