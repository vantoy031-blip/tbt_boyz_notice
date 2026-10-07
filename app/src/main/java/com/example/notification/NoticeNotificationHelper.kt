package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Notice

object NoticeNotificationHelper {
    private const val TAG = "NoticeNotification"
    const val CHANNEL_ID = "tbt_boyz_notices_channel"
    const val CHANNEL_NAME = "TBT BOYz Notices"
    const val CHANNEL_DESC = "Notifications for new notices, events, and important alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = 0xFFE11D48.toInt() // Crimson
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showNoticeNotification(context: Context, notice: Notice) {
        showNoticeNotification(
            context = context,
            noticeId = notice.id,
            title = notice.title,
            description = notice.description,
            category = notice.category,
            isImportant = notice.isImportant
        )
    }

    fun showNoticeNotification(
        context: Context,
        noticeId: String,
        title: String,
        description: String,
        category: String,
        isImportant: Boolean
    ) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Notification permission not granted; skipping system notification")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notice_id", noticeId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                noticeId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val displayTitle = when {
                isImportant -> "🚨 [IMPORTANT] $title"
                category.equals("event", ignoreCase = true) -> "🎉 [EVENT] $title"
                category.equals("announcement", ignoreCase = true) -> "📢 [ANNOUNCEMENT] $title"
                else -> "ℹ️ $title"
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(displayTitle)
                .setContentText(description.lines().firstOrNull() ?: description)
                .setStyle(NotificationCompat.BigTextStyle().bigText(description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(if (isImportant) 0xFFE11D48.toInt() else 0xFFF59E0B.toInt())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = noticeId.hashCode()
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "System notification dispatched for notice: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notice notification", e)
        }
    }

    fun sendTestNotification(context: Context) {
        showNoticeNotification(
            context = context,
            noticeId = "test_${System.currentTimeMillis()}",
            title = "Welcome to TBT BOYz Notice!",
            description = "Mobile notifications are active. You will receive updates directly on your device whenever new announcements or alerts are published.",
            category = "announcement",
            isImportant = true
        )
    }
}
