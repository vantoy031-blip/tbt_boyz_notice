package com.example.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoticeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NoticeSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting background notice worker synchronization...")
        try {
            NoticeSyncManager.performSyncAndNotify(applicationContext)
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Notice sync worker cancelled: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Background sync encountered error: ${e.message}", e)
            Result.retry()
        }
    }
}
