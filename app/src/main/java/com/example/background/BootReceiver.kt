package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("BootReceiver", "Boot broadcast received ($action). Initializing background sync...")
        NoticeBackgroundSyncScheduler.schedulePeriodicSync(context)
    }
}
