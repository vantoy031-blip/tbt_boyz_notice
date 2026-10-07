package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.notification.NoticeNotificationHelper
import com.example.ui.NoticeBoardScreen
import com.example.ui.NoticeBoardViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TBTNoticeBoardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NoticeBoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure high-priority notification channel is created
        NoticeNotificationHelper.createNotificationChannel(this)

        // Handle notice notification tap
        intent?.getStringExtra("notice_id")?.let { noticeId ->
            viewModel.openNoticeById(noticeId)
        }

        enableEdgeToEdge()
        setContent {
            TBTNoticeBoardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    NoticeBoardScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("notice_id")?.let { noticeId ->
            viewModel.openNoticeById(noticeId)
        }
    }
}
