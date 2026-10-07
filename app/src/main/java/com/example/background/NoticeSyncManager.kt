package com.example.background

import android.content.Context
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.local.NoticeEntity
import com.example.data.local.UserPreferences
import com.example.notification.NoticeNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object NoticeSyncManager {
    private const val TAG = "NoticeSyncManager"

    suspend fun performSyncAndNotify(context: Context): Int = withContext(Dispatchers.IO) {
        Log.d(TAG, "Performing notice synchronization and notification check...")
        try {
            // 1. Ensure Firebase is initialized
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

            // Try fetching directly from SERVER first so newly published notices are discovered instantly
            val snapshots = try {
                db.collection(FirebaseManager.COLLECTION_NOTICES).get(Source.SERVER).await()
            } catch (e: Exception) {
                Log.d(TAG, "Source.SERVER get had notice (${e.message}), trying default source")
                db.collection(FirebaseManager.COLLECTION_NOTICES).get().await()
            }

            val userPreferences = UserPreferences(context)
            val database = AppDatabase.getInstance(context)
            val noticeDao = database.noticeDao()

            if (snapshots.isEmpty) {
                Log.d(TAG, "Cloud notices collection is empty. Purging local notices.")
                noticeDao.deleteAllNotices()
                userPreferences.lastBackgroundSyncTime = System.currentTimeMillis()
                return@withContext 0
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

                    // If notice is active and has not yet been notified on this mobile device
                    if (!isArchived && !userPreferences.isNoticeNotified(id)) {
                        newActiveNoticesToNotify.add(entity)
                    }
                }
            }

            // Sync with local Room database
            if (cloudNotices.isNotEmpty()) {
                noticeDao.insertAll(cloudNotices)
                noticeDao.deleteNoticesNotIn(cloudIds)
            } else {
                noticeDao.deleteAllNotices()
            }

            // Immediately dispatch notifications to mobile status bar for each new notice
            for (notice in newActiveNoticesToNotify) {
                Log.d(TAG, "Dispatching instant notification for new notice: ${notice.title}")
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
            Log.d(TAG, "Sync complete. Dispatched ${newActiveNoticesToNotify.size} notifications.")
            newActiveNoticesToNotify.size
        } catch (e: Exception) {
            Log.e(TAG, "performSyncAndNotify encountered an error: ${e.message}", e)
            0
        }
    }
}
