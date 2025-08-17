package com.cgj.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notifications")

class NotificationPreferences(private val context: Context) {
    
    companion object {
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val CHECK_INTERVAL = intPreferencesKey("check_interval")
        private val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        private val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        private val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val AUTO_DOWNLOAD = booleanPreferencesKey("auto_download")
        private val LAST_CHECK_TIME = longPreferencesKey("last_check_time")
        private val LAST_VERTRETUNGSPLAN_HASH = stringPreferencesKey("last_vertretungsplan_hash")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
    
    val notificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: false
        }
    
    val checkInterval: Flow<Int> = context.notificationDataStore.data
        .map { preferences ->
            preferences[CHECK_INTERVAL] ?: 15 // Default 15 minutes
        }
    
    val quietHoursStart: Flow<Int> = context.notificationDataStore.data
        .map { preferences ->
            preferences[QUIET_HOURS_START] ?: 22 // Default 10 PM
        }
    
    val quietHoursEnd: Flow<Int> = context.notificationDataStore.data
        .map { preferences ->
            preferences[QUIET_HOURS_END] ?: 7 // Default 7 AM
        }
    
    val quietHoursEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[QUIET_HOURS_ENABLED] ?: true
        }
    
    val soundEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[SOUND_ENABLED] ?: true
        }
    
    val vibrationEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[VIBRATION_ENABLED] ?: true
        }
    
    val autoDownload: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[AUTO_DOWNLOAD] ?: false
        }
    
    val lastCheckTime: Flow<Long> = context.notificationDataStore.data
        .map { preferences ->
            preferences[LAST_CHECK_TIME] ?: 0L
        }
    
    val lastVertretungsplanHash: Flow<String?> = context.notificationDataStore.data
        .map { preferences ->
            preferences[LAST_VERTRETUNGSPLAN_HASH]
        }
    
    val onboardingCompleted: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setCheckInterval(minutes: Int) {
        context.notificationDataStore.edit { preferences ->
            preferences[CHECK_INTERVAL] = minutes
        }
    }
    
    suspend fun setQuietHours(start: Int, end: Int, enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[QUIET_HOURS_START] = start
            preferences[QUIET_HOURS_END] = end
            preferences[QUIET_HOURS_ENABLED] = enabled
        }
    }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }
    
    suspend fun setAutoDownload(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[AUTO_DOWNLOAD] = enabled
        }
    }
    
    suspend fun setLastCheckTime(timestamp: Long) {
        context.notificationDataStore.edit { preferences ->
            preferences[LAST_CHECK_TIME] = timestamp
        }
    }
    
    suspend fun setLastVertretungsplanHash(hash: String) {
        context.notificationDataStore.edit { preferences ->
            preferences[LAST_VERTRETUNGSPLAN_HASH] = hash
        }
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }
    
    suspend fun resetToDefaults() {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = false
            preferences[CHECK_INTERVAL] = 15
            preferences[QUIET_HOURS_START] = 22
            preferences[QUIET_HOURS_END] = 7
            preferences[QUIET_HOURS_ENABLED] = true
            preferences[SOUND_ENABLED] = true
            preferences[VIBRATION_ENABLED] = true
            preferences[AUTO_DOWNLOAD] = false
            preferences[ONBOARDING_COMPLETED] = false
        }
    }
}