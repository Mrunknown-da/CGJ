package com.cgj.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Boot completed or package replaced, checking notification settings")
                
                // Use coroutine to handle async operations
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val preferences = NotificationPreferences(context)
                        val notificationsEnabled = preferences.notificationsEnabled.first()
                        
                        if (notificationsEnabled) {
                            Log.d(TAG, "Notifications enabled, starting service")
                            
                            // Start the notification service
                            val serviceIntent = Intent(context, NotificationService::class.java).apply {
                                action = "START_FOREGROUND"
                            }
                            
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                            
                            // Schedule the notification worker
                            val checkInterval = preferences.checkInterval.first()
                            NotificationWorker.schedulePeriodicWork(context, checkInterval)
                            
                            Log.d(TAG, "Service and worker started successfully after boot")
                        } else {
                            Log.d(TAG, "Notifications disabled, not starting service")
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting service after boot: ${e.message}")
                    }
                }
            }
        }
    }
}