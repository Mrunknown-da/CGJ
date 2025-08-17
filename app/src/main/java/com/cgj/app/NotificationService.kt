package com.cgj.app

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.*

class NotificationService : LifecycleService() {
    
    companion object {
        private const val TAG = "NotificationService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "notification_service"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "NotificationService started")
        
        when (intent?.action) {
            "START_FOREGROUND" -> startForeground()
            "STOP_FOREGROUND" -> stopForeground()
            else -> startForeground()
        }
        
        return START_STICKY
    }
    
    private fun startForeground() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Service started in foreground")
        
        // Start the notification worker
        serviceScope.launch {
            try {
                val preferences = NotificationPreferences(this@NotificationService)
                val checkInterval = preferences.checkInterval.first()
                
                if (preferences.notificationsEnabled.first()) {
                    NotificationWorker.schedulePeriodicWork(this@NotificationService, checkInterval)
                    Log.d(TAG, "Scheduled periodic work with $checkInterval minute interval")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting notification worker: ${e.message}")
            }
        }
    }
    
    private fun stopForeground() {
        stopForeground(true)
        Log.d(TAG, "Service stopped foreground")
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CGJ App läuft im Hintergrund")
            .setContentText("Überwacht Vertretungsplan-Updates")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notification Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hintergrund-Service für Vertretungsplan-Benachrichtigungen"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NotificationService destroyed")
        
        // Cancel the notification worker
        NotificationWorker.cancelPeriodicWork(this)
        
        // Cancel the service scope
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}