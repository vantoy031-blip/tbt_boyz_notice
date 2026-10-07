package com.example

import android.app.Application
import android.util.Log
import com.example.background.NoticeBackgroundSyncScheduler
import com.example.notification.NoticeNotificationHelper

class TbtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("TbtApplication", "TBT BOYz Notice application initialized")
        
        // Ensure system notification channel exists
        NoticeNotificationHelper.createNotificationChannel(this)

        // Start live Foreground Service so Firestore receives alerts even when app is closed/cleared
        com.example.background.NoticeBackgroundService.start(this)

        // Schedule background sync via WorkManager + AlarmManager as persistent backup
        NoticeBackgroundSyncScheduler.schedulePeriodicSync(this)
    }
}
