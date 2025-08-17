package com.cgj.app

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "NotificationWorker"
        private const val WORK_NAME = "vertretungsplan_check"
        
        fun schedulePeriodicWork(context: Context, intervalMinutes: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .addTag(WORK_NAME)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            
            Log.d(TAG, "Scheduled periodic work every $intervalMinutes minutes")
        }
        
        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled periodic work")
        }
        
        fun scheduleOneTimeWork(context: Context, delayMinutes: Long = 0) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()
            
            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "Scheduled one-time work with ${delayMinutes}min delay")
        }
    }
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting Vertretungsplan check")
                
                val preferences = NotificationPreferences(applicationContext)
                val repository = VertretungsplanRepository(applicationContext)
                val notificationManager = NotificationManager(applicationContext)
                
                // Check if notifications are enabled
                val notificationsEnabled = preferences.notificationsEnabled.first()
                if (!notificationsEnabled) {
                    Log.d(TAG, "Notifications disabled, skipping check")
                    return@withContext Result.success()
                }
                
                // Check if it's school hours
                if (!repository.isSchoolHours()) {
                    Log.d(TAG, "Outside school hours, skipping check")
                    return@withContext Result.success()
                }
                
                // Check quiet hours
                val quietHoursEnabled = preferences.quietHoursEnabled.first()
                if (quietHoursEnabled) {
                    val quietStart = preferences.quietHoursStart.first()
                    val quietEnd = preferences.quietHoursEnd.first()
                    if (repository.isQuietHours(quietStart, quietEnd)) {
                        Log.d(TAG, "In quiet hours, skipping check")
                        return@withContext Result.success()
                    }
                }
                
                // Get last known hash
                val lastHash = preferences.lastVertretungsplanHash.first()
                
                // Check for changes
                val hasChanges = repository.checkForChanges(lastHash)
                if (hasChanges) {
                    Log.d(TAG, "Changes detected in Vertretungsplan")
                    
                    // Fetch current data
                    val currentData = repository.fetchCurrentVertretungsplan()
                    if (currentData != null) {
                        // Update last known hash
                        preferences.setLastVertretungsplanHash(currentData.contentHash)
                        
                        // Show notification
                        notificationManager.showVertretungsplanUpdate(
                            title = "Neuer Vertretungsplan verfügbar",
                            content = "Es gibt Änderungen im Vertretungsplan",
                            pdfUrl = currentData.pdfUrl,
                            imageUrl = currentData.imageUrl
                        )
                        
                        // Auto-download if enabled
                        val autoDownload = preferences.autoDownload.first()
                        if (autoDownload) {
                            val urlToDownload = currentData.pdfUrl ?: currentData.imageUrl
                            if (urlToDownload != null) {
                                val downloaded = repository.downloadVertretungsplan(urlToDownload)
                                if (downloaded != null) {
                                    Log.d(TAG, "Auto-download completed successfully")
                                }
                            }
                        }
                        
                        // Update last check time
                        preferences.setLastCheckTime(System.currentTimeMillis())
                        
                        Log.d(TAG, "Notification sent successfully")
                    } else {
                        Log.w(TAG, "Failed to fetch current Vertretungsplan data")
                    }
                } else {
                    Log.d(TAG, "No changes detected in Vertretungsplan")
                    // Update last check time even if no changes
                    preferences.setLastCheckTime(System.currentTimeMillis())
                }
                
                Result.success()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in NotificationWorker: ${e.message}")
                Result.retry()
            }
        }
    }
}