package com.example.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NoticeBackgroundSyncScheduler {
    private const val TAG = "BackgroundScheduler"
    const val PERIODIC_WORK_TAG = "tbt_notice_background_periodic_sync"
    const val ONE_TIME_WORK_TAG = "tbt_notice_background_immediate_sync"
    const val TEST_WORK_TAG = "tbt_notice_background_test_sync"

    fun schedulePeriodicSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<NoticeSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "WorkManager periodic sync scheduled (every 15 min)")

            // Secondary AlarmManager trigger to ensure prompt wakeups across aggressive OEM OSs
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, BackgroundAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 15 * 60 * 1000L,
                15 * 60 * 1000L,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule background periodic sync", e)
        }
    }

    fun triggerImmediateSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<NoticeSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
            Log.d(TAG, "Immediate background sync triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger immediate background sync", e)
        }
    }

    fun triggerTestBackgroundNotification(context: Context, delaySeconds: Long = 5) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val testRequest = OneTimeWorkRequestBuilder<TestBackgroundNotificationWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                TEST_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                testRequest
            )
            Log.d(TAG, "Test background notification scheduled in $delaySeconds seconds")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule test background notification", e)
        }
    }
}
