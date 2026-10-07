package com.example.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notification.NoticeNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TestBackgroundNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("TestBackgroundWorker", "Test background worker executed from background!")
        NoticeNotificationHelper.showNoticeNotification(
            context = applicationContext,
            noticeId = "test_bg_${System.currentTimeMillis()}",
            title = "Background Notification Working!",
            description = "TBT BOYz Notice background service successfully delivered this notification while the app was closed.",
            category = "announcement",
            isImportant = true
        )
        Result.success()
    }
}
