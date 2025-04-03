package com.br.leo.moodsnap.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.MainActivity
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notifications),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_message)
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyNotification(hour: Int, minute: Int) {
        Log.d(TAG, "Scheduling notification for $hour:$minute")
        
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Se o horário já passou hoje, agendar para amanhã
            if (before(Calendar.getInstance())) {
                Log.d(TAG, "Time already passed today, scheduling for tomorrow")
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            // Cancelar qualquer notificação existente antes de agendar uma nova
            cancelDailyNotification()

            // Tentar usar setAlarmClock primeiro (mais confiável)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    NOTIFICATION_REQUEST_CODE,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmInfo, pendingIntent)
            } else {
                // Fallback para versões mais antigas
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }

            Log.d(TAG, "Notification successfully scheduled for ${calendar.time}")
            
            // Enviar uma notificação imediata para confirmar que foi configurado
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification", e)
        }
    }

    private fun sendConfirmationNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("Notificações Ativadas")
            .setContentText("Você receberá lembretes diários para registrar seu humor")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(CONFIRMATION_NOTIFICATION_ID, notification)
    }

    fun cancelDailyNotification() {
        Log.d(TAG, "Canceling daily notification")
        try {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Daily notification successfully canceled")
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notification", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "moodsnap_notification_channel"
        const val NOTIFICATION_REQUEST_CODE = 123
        const val CONFIRMATION_NOTIFICATION_ID = 456
        private const val TAG = "NotificationHelper"
    }
} 