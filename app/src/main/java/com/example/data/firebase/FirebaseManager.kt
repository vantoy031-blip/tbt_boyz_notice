package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.NoticeDao
import com.example.data.local.NoticeEntity
import com.example.data.model.Notice
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    const val COLLECTION_NOTICES = "notices"

    // Configuration credentials provided by the user
    const val API_KEY = "AIzaSyDcOvUmZW6UfFe1LaeoEp2dYyp4Qgu3nPw"
    const val AUTH_DOMAIN = "tbt-boyz-notice-board.firebaseapp.com"
    const val PROJECT_ID = "tbt-boyz-notice-board"
    const val STORAGE_BUCKET = "tbt-boyz-notice-board.firebasestorage.app"
    const val MESSAGING_SENDER_ID = "1060086938276"
    const val APP_ID = "1:1060086938276:web:110021adbbbcda5cecc431"

    private var firestore: FirebaseFirestore? = null
    private var snapshotListener: ListenerRegistration? = null
    private var appContext: Context? = null
    private var isFirstSnapshot = true

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Initializing Firebase...")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    fun initialize(context: Context, noticeDao: NoticeDao, scope: CoroutineScope) {
        appContext = context.applicationContext
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(API_KEY)
                    .setApplicationId(APP_ID)
                    .setProjectId(PROJECT_ID)
                    .setStorageBucket(STORAGE_BUCKET)
                    .setGcmSenderId(MESSAGING_SENDER_ID)
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }

            val db = FirebaseFirestore.getInstance(app)
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
            firestore = db
            _isFirebaseConnected.value = true
            _syncStatusMessage.value = "Connected to $PROJECT_ID"
            Log.d(TAG, "Firebase initialized successfully with project $PROJECT_ID")

            // Start Real-Time Firestore Sync with Collection "notices"
            startRealtimeSync(db, noticeDao, scope)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init error (using offline local mode): ${e.message}")
            _isFirebaseConnected.value = false
            _syncStatusMessage.value = "Local mode (offline ready)"
        }
    }

    private fun startRealtimeSync(db: FirebaseFirestore, noticeDao: NoticeDao, scope: CoroutineScope) {
        snapshotListener?.remove()
        try {
            snapshotListener = db.collection(COLLECTION_NOTICES)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore listen failed: ${error.message}")
                        _syncStatusMessage.value = "Sync error: ${error.message}"
                        return@addSnapshotListener
                    }

                    if (snapshots == null) return@addSnapshotListener

                    // 1. Immediately delete any removed documents from local Room database
                    for (change in snapshots.documentChanges) {
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            val removedId = change.document.id
                            scope.launch(Dispatchers.IO) {
                                noticeDao.deleteById(removedId)
                                Log.d(TAG, "Notice $removedId deleted from local DB via real-time change")
                            }
                        }
                    }

                    if (!snapshots.isEmpty) {
                        // Check for new notices to dispatch mobile push/system notifications
                        if (!isFirstSnapshot) {
                            val context = appContext
                            if (context != null) {
                                for (change in snapshots.documentChanges) {
                                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                        val doc = change.document
                                        val title = doc.getString("title") ?: ""
                                        val desc = doc.getString("description") ?: ""
                                        val isArchived = doc.getBoolean("isArchived") ?: false
                                        if (title.isNotBlank() && !isArchived) {
                                            com.example.notification.NoticeNotificationHelper.showNoticeNotification(
                                                context = context,
                                                noticeId = doc.id,
                                                title = title,
                                                description = desc,
                                                category = doc.getString("category") ?: "general",
                                                isImportant = doc.getBoolean("isImportant") ?: false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        isFirstSnapshot = false

                        scope.launch(Dispatchers.IO) {
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
                                val createdAt = extractTimestamp(doc, "createdAt")
                                val updatedAt = extractTimestamp(doc, "updatedAt")
                                val publishedAt = extractTimestamp(doc, "publishedAt", fallback = createdAt)

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
                                // CRITICAL: Delete any notices in local database that do NOT exist in Firestore!
                                // This ensures any deleted notice or local dummy notice is deleted for EVERYONE!
                                noticeDao.deleteNoticesNotIn(cloudIds)
                                _syncStatusMessage.value = "Synced ${cloudNotices.size} notices with cloud"
                            } else {
                                noticeDao.deleteAllNotices()
                                _syncStatusMessage.value = "Synced (0 notices)"
                            }
                        }
                    } else {
                        // Cloud collection is completely empty (admin deleted all notices, or none exist yet)
                        isFirstSnapshot = false
                        scope.launch(Dispatchers.IO) {
                            // Purge all notices from local database so no ghost/deleted notices remain
                            noticeDao.deleteAllNotices()
                            _syncStatusMessage.value = "Synced: No active notices"
                            Log.d(TAG, "Cloud notices collection is empty. Cleared local notices.")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach real-time listener: ${e.message}")
        }
    }

    private fun extractTimestamp(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        field: String,
        fallback: Long = System.currentTimeMillis()
    ): Long {
        return try {
            when (val value = doc.get(field)) {
                is com.google.firebase.Timestamp -> value.toDate().time
                is Number -> value.toLong()
                is java.util.Date -> value.time
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    suspend fun saveNoticeToCloud(notice: Notice) {
        val db = firestore ?: return
        try {
            val data = hashMapOf(
                "title" to notice.title,
                "description" to notice.description,
                "category" to notice.category,
                "isImportant" to notice.isImportant,
                "isPinned" to notice.isPinned,
                "isArchived" to notice.isArchived,
                "createdAt" to notice.createdAt,
                "updatedAt" to notice.updatedAt,
                "publishedAt" to notice.publishedAt
            )
            db.collection(COLLECTION_NOTICES).document(notice.id).set(data).await()
            Log.d(TAG, "Successfully synced notice ${notice.id} to Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Could not save notice to Firestore: ${e.message}")
        }
    }

    suspend fun deleteNoticeFromCloud(noticeId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_NOTICES).document(noticeId).delete().await()
            Log.d(TAG, "Successfully deleted notice $noticeId from Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete notice from Firestore: ${e.message}")
        }
    }

    suspend fun deleteAllNoticesFromCloud() {
        val db = firestore ?: return
        try {
            val snapshots = db.collection(COLLECTION_NOTICES).get().await()
            if (snapshots.isEmpty) return
            val batch = db.batch()
            for (doc in snapshots.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Log.d(TAG, "Successfully deleted all ${snapshots.size()} notices from Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete all notices from Firestore: ${e.message}")
        }
    }

    suspend fun updateNoticeFieldsInCloud(noticeId: String, fields: Map<String, Any>) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_NOTICES).document(noticeId).update(fields).await()
            Log.d(TAG, "Successfully updated notice $noticeId in Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Could not update notice in Firestore: ${e.message}")
        }
    }

    suspend fun refreshFromFirestore(noticeDao: NoticeDao): Result<Int> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        return try {
            val snapshots = db.collection(COLLECTION_NOTICES).get().await()
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
                val createdAt = extractTimestamp(doc, "createdAt")
                val updatedAt = extractTimestamp(doc, "updatedAt")
                val publishedAt = extractTimestamp(doc, "publishedAt", fallback = createdAt)

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
                _syncStatusMessage.value = "Synced ${cloudNotices.size} notices from cloud"
            } else {
                noticeDao.deleteAllNotices()
                _syncStatusMessage.value = "Synced: 0 notices"
            }
            Result.success(cloudNotices.size)
        } catch (e: Exception) {
            Log.w(TAG, "Manual refresh failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun loginAdminFirebase(email: String, password: String) {
        try {
            val auth = FirebaseAuth.getInstance()
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Log.d(TAG, "Firebase Auth admin signed in successfully: ${it.user?.email}")
                }
                .addOnFailureListener {
                    // Try to register if first time setup, or record failure
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { res ->
                            Log.d(TAG, "Created initial Firebase Auth admin: ${res.user?.email}")
                        }
                        .addOnFailureListener { err ->
                            Log.d(TAG, "Firebase Auth notice: ${err.message}")
                        }
                }
        } catch (e: Exception) {
            Log.d(TAG, "Firebase Auth not active: ${e.message}")
        }
    }
}
