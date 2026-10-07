package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkChangeReceiver"
        private var lastTriggerTime = 0L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val isConnected = isInternetAvailable(cm)

        if (isConnected) {
            val now = System.currentTimeMillis()
            // Throttle duplicate broadcasts within 2.5 seconds
            if (now - lastTriggerTime < 2500L) return
            lastTriggerTime = now

            Log.d(TAG, "Internet connection detected (WiFi or Mobile Data ON)! Checking for new notices immediately...")

            // 1. Ensure live background service is active
            NoticeBackgroundService.start(context)

            // 2. Perform zero-delay instant sync via goAsync to pop notifications immediately
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val count = NoticeSyncManager.performSyncAndNotify(context)
                    Log.d(TAG, "Instant network-reconnect sync completed. $count new notices notified.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in network-reconnect sync: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }

            // 3. Also trigger WorkManager for dual redundancy
            NoticeBackgroundSyncScheduler.triggerImmediateSync(context)
        }
    }

    private fun isInternetAvailable(cm: ConnectivityManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }
}
