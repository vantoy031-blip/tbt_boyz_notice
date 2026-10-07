package com.example.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.local.NoticeEntity
import com.example.data.local.UserPreferences
import com.example.notification.NoticeNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NoticeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NoticeSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        Log.d(TAG, "Starting background notice synchronization...")

        try {
            // 1. Ensure FirebaseApp is initialized in the background worker process
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(FirebaseManager.API_KEY)
                    .setApplicationId(FirebaseManager.APP_ID)
                    .setProjectId(FirebaseManager.PROJECT_ID)
                    .setStorageBucket(FirebaseManager.STORAGE_BUCKET)
                    .setGcmSenderId(FirebaseManager.MESSAGING_SENDER_ID)
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }

            val db = FirebaseFirestore.getInstance(app)
            val snapshots = db.collection(FirebaseManager.COLLECTION_NOTICES).get().await()

            val userPreferences = UserPreferences(context)
            val database = AppDatabase.getInstance(context)
            val noticeDao = database.noticeDao()

            if (snapshots.isEmpty) {
                Log.d(TAG, "Background sync: Cloud collection is empty. Clearing local notices.")
                noticeDao.deleteAllNotices()
                userPreferences.lastBackgroundSyncTime = System.currentTimeMillis()
                return@withContext Result.success()
            }

            val cloudNotices = mutableListOf<NoticeEntity>()
            val cloudIds = mutableListOf<String>()
            val newActiveNoticesToNotify = mutableListOf<NoticeEntity>()

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
                    val entity = NoticeEntity(
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
                    cloudNotices.add(entity)

                    // If notice is active and hasn't been notified yet on this device
                    if (!isArchived && !userPreferences.isNoticeNotified(id)) {
                        newActiveNoticesToNotify.add(entity)
                    }
                }
            }

            // Sync with local Room database: insert valid, delete anything removed from cloud
            if (cloudNotices.isNotEmpty()) {
                noticeDao.insertAll(cloudNotices)
                noticeDao.deleteNoticesNotIn(cloudIds)
            } else {
                noticeDao.deleteAllNotices()
            }

            // Dispatch notifications for any new notices discovered while the app was closed
            for (notice in newActiveNoticesToNotify) {
                Log.d(TAG, "Background sync: Dispatching notification for new notice: ${notice.title}")
                NoticeNotificationHelper.showNoticeNotification(
                    context = context,
                    noticeId = notice.id,
                    title = notice.title,
                    description = notice.description,
                    category = notice.category,
                    isImportant = notice.isImportant
                )
                userPreferences.markNoticeAsNotified(notice.id)
            }

            userPreferences.lastBackgroundSyncTime = System.currentTimeMillis()
            Log.d(TAG, "Background sync completed successfully (${cloudNotices.size} notices, ${newActiveNoticesToNotify.size} notified)")
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Background sync worker cancelled: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Background sync encountered error: ${e.message}", e)
            Result.retry()
        }
    }
}
