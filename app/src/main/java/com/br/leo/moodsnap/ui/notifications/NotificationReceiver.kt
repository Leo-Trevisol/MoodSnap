package com.br.leo.moodsnap.ui.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.br.leo.moodsnap.MainActivity
import com.br.leo.moodsnap.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notification received")
        
        try {
            val notificationIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.mipmap.icon_ofc)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notification_message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setLights(android.graphics.Color.BLUE, 3000, 3000)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NotificationHelper.NOTIFICATION_REQUEST_CODE, notification)
            Log.d(TAG, "Notification sent successfully")

            // Reagendar a próxima notificação
            val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", false)
            
            if (notificationsEnabled) {
                val hour = sharedPreferences.getInt("notification_hour", 20)
                val minute = sharedPreferences.getInt("notification_minute", 0)
                NotificationHelper(context).scheduleDailyNotification(hour, minute)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }

    companion object {
        private const val TAG = "NotificationReceiver"
    }
} 