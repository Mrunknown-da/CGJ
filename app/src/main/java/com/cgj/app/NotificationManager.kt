package com.cgj.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

class NotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationManager"
        private const val CHANNEL_ID_VERTRETUNGSPLAN = "vertretungsplan_updates"
        private const val CHANNEL_ID_GENERAL = "general_notifications"
        private const val NOTIFICATION_ID_VERTRETUNGSPLAN = 1001
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Vertretungsplan updates channel
            val vertretungsplanChannel = NotificationChannel(
                CHANNEL_ID_VERTRETUNGSPLAN,
                "Vertretungsplan Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen über neue Vertretungspläne"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "Allgemeine Benachrichtigungen",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Allgemeine App-Benachrichtigungen"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannels(listOf(vertretungsplanChannel, generalChannel))
        }
    }
    
    fun showVertretungsplanUpdate(
        title: String = "Neuer Vertretungsplan verfügbar",
        content: String = "Es gibt Änderungen im Vertretungsplan",
        pdfUrl: String? = null,
        imageUrl: String? = null
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "OPEN_VERTRETUNGSPLAN"
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_VERTRETUNGSPLAN)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            
            // Add actions if URLs are available
            if (pdfUrl != null) {
                val downloadIntent = Intent(context, MainActivity::class.java).apply {
                    action = "DOWNLOAD_VERTRETUNGSPLAN"
                    putExtra("url", pdfUrl)
                    putExtra("type", "pdf")
                }
                val downloadPendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    downloadIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                notificationBuilder.addAction(
                    android.R.drawable.ic_menu_download,
                    "Herunterladen",
                    downloadPendingIntent
                )
            }
            
            if (imageUrl != null) {
                val viewIntent = Intent(context, MainActivity::class.java).apply {
                    action = "VIEW_VERTRETUNGSPLAN"
                    putExtra("url", imageUrl)
                    putExtra("type", "image")
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    context,
                    2,
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                notificationBuilder.addAction(
                    android.R.drawable.ic_menu_view,
                    "Anzeigen",
                    viewPendingIntent
                )
            }
            
            // Add snooze action
            val snoozeIntent = Intent(context, MainActivity::class.java).apply {
                action = "SNOOZE_NOTIFICATION"
            }
            val snoozePendingIntent = PendingIntent.getActivity(
                context,
                3,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            notificationBuilder.addAction(
                android.R.drawable.ic_menu_recent_history,
                "Später",
                snoozePendingIntent
            )
            
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_VERTRETUNGSPLAN,
                    notificationBuilder.build()
                )
                Log.d(TAG, "Vertretungsplan notification shown successfully")
            } else {
                Log.w(TAG, "Notifications are disabled")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }
    
    fun showGeneralNotification(
        title: String,
        content: String,
        channelId: String = CHANNEL_ID_GENERAL,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
            
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
                Log.d(TAG, "General notification shown successfully")
            } else {
                Log.w(TAG, "Notifications are disabled")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing general notification: ${e.message}")
        }
    }
    
    fun cancelVertretungsplanNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_VERTRETUNGSPLAN)
        Log.d(TAG, "Vertretungsplan notification cancelled")
    }
    
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
        Log.d(TAG, "All notifications cancelled")
    }
    
    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}