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

        // Schedule background sync via WorkManager + AlarmManager
        NoticeBackgroundSyncScheduler.schedulePeriodicSync(this)
    }
}
