package com.example.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.local.NoticeEntity
import com.example.data.local.UserPreferences
import com.example.notification.NoticeNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NoticeBackgroundService : Service() {

    companion object {
        private const val TAG = "NoticeBgService"
        const val SERVICE_NOTIFICATION_ID = 9001

        fun start(context: Context) {
            val prefs = UserPreferences(context)
            if (!prefs.isLiveBackgroundServiceEnabled) {
                Log.d(TAG, "Live background service is disabled in preferences; skipping start")
                return
            }
            try {
                val intent = Intent(context, NoticeBackgroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "NoticeBackgroundService start command dispatched")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start NoticeBackgroundService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, NoticeBackgroundService::class.java)
                context.stopService(intent)
                Log.d(TAG, "NoticeBackgroundService stop command dispatched")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop NoticeBackgroundService", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var snapshotListener: ListenerRegistration? = null
    private var isFirstSnapshot = true
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NoticeBackgroundService onCreate called")

        NoticeNotificationHelper.createNotificationChannel(this)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val foregroundNotification = NotificationCompat.Builder(this, NoticeNotificationHelper.SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("TBT BOYz Notice Service")
            .setContentText("Monitoring live announcements")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                SERVICE_NOTIFICATION_ID,
                foregroundNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, foregroundNotification)
        }

        attachLiveFirestoreListener()
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Internet connection active (WiFi or Data ON). Instantly checking for notices...")
                    serviceScope.launch {
                        try {
                            NoticeSyncManager.performSyncAndNotify(applicationContext)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in onAvailable notice sync: ${e.message}", e)
                        }
                    }
                }
            }
            cm.registerNetworkCallback(request, callback)
            networkCallback = callback
            Log.d(TAG, "Registered NetworkCallback for instant network-reconnect notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NetworkCallback", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NoticeBackgroundService onStartCommand called")
        if (snapshotListener == null) {
            attachLiveFirestoreListener()
        }
        return START_STICKY
    }

    private fun attachLiveFirestoreListener() {
        snapshotListener?.remove()
        try {
            val app = if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(FirebaseManager.API_KEY)
                    .setApplicationId(FirebaseManager.APP_ID)
                    .setProjectId(FirebaseManager.PROJECT_ID)
                    .setStorageBucket(FirebaseManager.STORAGE_BUCKET)
                    .setGcmSenderId(FirebaseManager.MESSAGING_SENDER_ID)
                    .build()
                FirebaseApp.initializeApp(this, options)
            } else {
                FirebaseApp.getInstance()
            }

            val db = FirebaseFirestore.getInstance(app)
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings

            val userPreferences = UserPreferences(this)
            val noticeDao = AppDatabase.getInstance(this).noticeDao()

            snapshotListener = db.collection(FirebaseManager.COLLECTION_NOTICES)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Background Firestore listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots == null) return@addSnapshotListener

                    // 1. Immediately delete removed documents from local Room database
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) {
                            val removedId = change.document.id
                            serviceScope.launch {
                                noticeDao.deleteById(removedId)
                                Log.d(TAG, "Background service removed notice $removedId from local database")
                            }
                        }
                    }

                    // 2. Detect newly added notices to dispatch mobile notifications
                    if (!snapshots.isEmpty) {
                        if (!isFirstSnapshot) {
                            for (change in snapshots.documentChanges) {
                                if (change.type == DocumentChange.Type.ADDED) {
                                    val doc = change.document
                                    val title = doc.getString("title") ?: ""
                                    val description = doc.getString("description") ?: ""
                                    val category = doc.getString("category") ?: "general"
                                    val isImportant = doc.getBoolean("isImportant") ?: false
                                    val isArchived = doc.getBoolean("isArchived") ?: false

                                    if (title.isNotBlank() && !isArchived && !userPreferences.isNoticeNotified(doc.id)) {
                                        Log.d(TAG, "Background service: Dispatching notification for $title")
                                        NoticeNotificationHelper.showNoticeNotification(
                                            context = this@NoticeBackgroundService,
                                            noticeId = doc.id,
                                            title = title,
                                            description = description,
                                            category = category,
                                            isImportant = isImportant
                                        )
                                        userPreferences.markNoticeAsNotified(doc.id)
                                    }
                                }
                            }
                        }
                        isFirstSnapshot = false

                        // Sync with local Room database
                        serviceScope.launch {
                            val cloudNotices = mutableListOf<NoticeEntity>()
                            val cloudIds = mutableListOf<String>()

                            for (doc in snapshots.documents) {
                                val id = doc.id
                                val title = doc.getString("title") ?: ""
                                val description = doc.getString("description") ?: ""
                                val category = doc.getString("category") ?: "general"
                                val isImportant = doc.getBoolean("isImportant") ?: false
                                val isPinned = doc.getBoolean("isPinned") ?: false
                                val isArchived = doc.getBoolean("isArchived") ?: false

                                val createdAt = try {
                                    when (val v = doc.get("createdAt")) {
                                        is com.google.firebase.Timestamp -> v.toDate().time
                                        is Number -> v.toLong()
                                        else -> System.currentTimeMillis()
                                    }
                                } catch (_: Exception) {
                                    System.currentTimeMillis()
                                }

                                val updatedAt = try {
                                    when (val v = doc.get("updatedAt")) {
                                        is com.google.firebase.Timestamp -> v.toDate().time
                                        is Number -> v.toLong()
                                        else -> createdAt
                                    }
                                } catch (_: Exception) {
                                    createdAt
                                }

                                val publishedAt = try {
                                    when (val v = doc.get("publishedAt")) {
                                        is com.google.firebase.Timestamp -> v.toDate().time
                                        is Number -> v.toLong()
                                        else -> createdAt
                                    }
                                } catch (_: Exception) {
                                    createdAt
                                }

                                if (title.isNotBlank()) {
                                    cloudIds.add(id)
                                    cloudNotices.add(
                                        NoticeEntity(
                                            id = id,
                                            title = title,
                                            description = description,
                                            category = category,
                                            isImportant = isImportant,
                                            isPinned = isPinned,
                                            isArchived = isArchived,
                                            createdAt = createdAt,
                                            updatedAt = updatedAt,
                                            publishedAt = publishedAt
                                        )
                                    )
                                }
                            }

                            if (cloudNotices.isNotEmpty()) {
                                noticeDao.insertAll(cloudNotices)
                                noticeDao.deleteNoticesNotIn(cloudIds)
                            } else {
                                noticeDao.deleteAllNotices()
                            }
                        }
                    } else {
                        isFirstSnapshot = false
                        serviceScope.launch {
                            noticeDao.deleteAllNotices()
                        }
                    }
                }
            Log.d(TAG, "Attached live Firestore listener to background service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach live Firestore listener in background service", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "App task removed from Recents. Guaranteeing background service stays alive...")

        val prefs = UserPreferences(applicationContext)
        if (!prefs.isLiveBackgroundServiceEnabled) return

        // Schedule an immediate alarm to resurrect or maintain the service if Android kills the process
        try {
            val restartIntent = Intent(applicationContext, NoticeBackgroundService::class.java).apply {
                setPackage(packageName)
            }
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                1008,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000L,
                restartPendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule restart on task removed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NoticeBackgroundService onDestroy called")
        snapshotListener?.remove()

        networkCallback?.let {
            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }

        serviceScope.cancel()

        val prefs = UserPreferences(applicationContext)
        if (prefs.isLiveBackgroundServiceEnabled) {
            // Self-heal: restart if killed unexpectedly
            try {
                val restartIntent = Intent(applicationContext, NoticeBackgroundService::class.java).apply {
                    setPackage(packageName)
                }
                val restartPendingIntent = PendingIntent.getService(
                    applicationContext,
                    1009,
                    restartIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 1500L,
                    restartPendingIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule restart on destroy", e)
            }
        }
    }
}
