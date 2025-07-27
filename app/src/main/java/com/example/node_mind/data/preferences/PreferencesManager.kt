package com.example.node_mind.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "node_mind_preferences")

class PreferencesManager(
    private val context: Context
) {
    
    companion object {
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DEFAULT_POMODORO_DURATION = intPreferencesKey("default_pomodoro_duration")
        private val ENABLE_NOTIFICATIONS = booleanPreferencesKey("enable_notifications")
        private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        private val CURRENT_STREAK = intPreferencesKey("current_streak")
    }
    
    // First launch flag
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_FIRST_LAUNCH] ?: true
        }
    
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = false
        }
    }
    
    // Theme settings
    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            when (preferences[THEME_MODE]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.DARK.name -> ThemeMode.DARK
                ThemeMode.AMOLED.name -> ThemeMode.AMOLED
                else -> ThemeMode.SYSTEM
            }
        }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }
    
    // Pomodoro settings
    val defaultPomodoroDuration: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_POMODORO_DURATION] ?: 25
        }
    
    suspend fun setDefaultPomodoroDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_POMODORO_DURATION] = minutes
        }
    }
    
    // Notifications
    val enableNotifications: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ENABLE_NOTIFICATIONS] ?: true
        }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_NOTIFICATIONS] = enabled
        }
    }
    
    // Backup
    val lastBackupTime: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_BACKUP_TIME] ?: 0L
        }
    
    suspend fun setLastBackupTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_BACKUP_TIME] = timestamp
        }
    }
    
    // Streak
    val currentStreak: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENT_STREAK] ?: 0
        }
    
    suspend fun setCurrentStreak(streak: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_STREAK] = streak
        }
    }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, AMOLED
}
