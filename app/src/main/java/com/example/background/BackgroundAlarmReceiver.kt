package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BackgroundAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("BackgroundAlarmReceiver", "AlarmManager wakeup triggered; kicking off background sync")
        NoticeBackgroundSyncScheduler.triggerImmediateSync(context)
    }
}
