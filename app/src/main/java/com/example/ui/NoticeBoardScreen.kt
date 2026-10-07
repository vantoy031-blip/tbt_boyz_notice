package com.example.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.admin.AdminDashboard
import com.example.ui.admin.AdminLoginDialog
import com.example.ui.admin.NoticeFormDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.NoticeCard
import com.example.ui.components.NoticeDetailsDialog
import com.example.ui.components.NoticeFilterBar
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
    viewModel: NoticeBoardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val filteredNotices by viewModel.filteredNotices.collectAsState()
    val allNotices by viewModel.allNotices.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val selectedNotice by viewModel.selectedNotice.collectAsState()
    val stats by viewModel.statistics.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val showAdminLoginDialog by viewModel.showAdminLoginDialog.collectAsState()
    val showAdminDashboard by viewModel.showAdminDashboard.collectAsState()
    val showNoticeFormDialog by viewModel.showNoticeFormDialog.collectAsState()
    val editingNotice by viewModel.editingNotice.collectAsState()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val readVersion by viewModel.readStateVersion.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    // Intercept back button when Admin Dashboard is open
    BackHandler(enabled = showAdminDashboard) {
        viewModel.closeAdminDashboard()
    }

    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsState()

    // Notification permission launcher for Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Mobile notifications enabled for TBT BOYz Notice!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!com.example.notification.NoticeNotificationHelper.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (showAdminDashboard) {
        AdminDashboard(
            notices = allNotices,
            stats = stats,
            cloudStatusText = firebaseSyncStatus,
            onLogout = { viewModel.logoutAdmin() },
            onClose = { viewModel.closeAdminDashboard() },
            onCreateNotice = { viewModel.openCreateNotice() },
            onEditNotice = { viewModel.openEditNotice(it) },
            onDeleteNotice = { viewModel.deleteNotice(it) },
            onTogglePin = { viewModel.togglePin(it) },
            onToggleImportant = { viewModel.toggleImportant(it) },
            onToggleArchive = { viewModel.toggleArchive(it) },
            onTestNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !com.example.notification.NoticeNotificationHelper.hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.sendTestNotification()
                    Toast.makeText(context, "Test notification dispatched to status bar!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            containerColor = DarkBackground,
            topBar = {
                HeaderTopBar(
                    unreadCount = unreadCount,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    isAdminLoggedIn = isAdminLoggedIn,
                    onSearchToggle = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.setSearchQuery("")
                    },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onMarkAllAsRead = {
                        viewModel.markAllAsRead()
                        Toast.makeText(context, "All notices marked as read", Toast.LENGTH_SHORT).show()
                    },
                    onAdminClick = { viewModel.openAdminAccess() }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Section Title Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📢",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Notice Board",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary
                        )
                    }

                    if (unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CrimsonPrimary.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.markAllAsRead()
                                    Toast.makeText(context, "All marked as read", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Mark all read",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$unreadCount new",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = CrimsonPrimary
                                )
                            }
                        }
                    }
                }

                // Horizontal Category Filter Bar
                NoticeFilterBar(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setCategory(it) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Notices Feed with Pull-To-Refresh to sync with Firestore
                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        viewModel.refreshNotices { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    state = pullRefreshState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pull_to_refresh_feed"),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = DarkSurfaceElevated,
                            color = CrimsonPrimary
                        )
                    }
                ) {
                    if (filteredNotices.isEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
                        ) {
                            item {
                                EmptyState(
                                    message = if (searchQuery.isNotBlank()) "No matching notices" else "No notices available",
                                    subMessage = if (searchQuery.isNotBlank()) {
                                        "Try searching with different keywords or switch categories."
                                    } else {
                                        "Pull down to sync with Firestore or check back later."
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                        ) {
                            items(filteredNotices, key = { it.id }) { notice ->
                                val isUnread = viewModel.isNoticeUnread(notice)
                                NoticeCard(
                                    notice = notice,
                                    isUnread = isUnread,
                                    onClick = { viewModel.openNoticeDetails(notice) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Details Dialog
    selectedNotice?.let { notice ->
        NoticeDetailsDialog(
            notice = notice,
            onDismiss = { viewModel.closeNoticeDetails() }
        )
    }

    // Admin Login Dialog
    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onDismiss = { viewModel.closeAdminLogin() },
            onLogin = { email, password, onResult ->
                viewModel.loginAdmin(email, password, onResult)
            },
            onSetCustomPassword = { email, password ->
                viewModel.setCustomAdminPassword(email, password)
                Toast.makeText(context, "Admin password updated & logged in!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Notice Form Dialog (Create / Edit)
    if (showNoticeFormDialog) {
        NoticeFormDialog(
            initialNotice = editingNotice,
            onDismiss = { viewModel.closeNoticeForm() },
            onSave = { title, description, category, isImportant, isPinned ->
                viewModel.saveNotice(title, description, category, isImportant, isPinned)
            }
        )
    }
}

@Composable
fun HeaderTopBar(
    unreadCount: Int,
    isSearchActive: Boolean,
    searchQuery: String,
    isAdminLoggedIn: Boolean,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onAdminClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_header"),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(0.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Branding: TBT Notice Board with sleek crest
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("app_branding")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = CrimsonPrimary,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TBT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "TBT BOYz Notice",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = 0.2.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Official Announcements",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    }
                }

                // Action Icons: Search, Notifications Bell, Admin Lock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Search toggle
                    IconButton(
                        onClick = onSearchToggle,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSearchActive) DarkSurfaceElevated else Color.Transparent)
                            .testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Close Search" else "Search Notices",
                            tint = if (isSearchActive) CrimsonPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bell with unread badge counter
                    IconButton(
                        onClick = onMarkAllAsRead,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .testTag("notification_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = CrimsonPrimary,
                                        contentColor = Color.White,
                                        modifier = Modifier.testTag("bell_badge_count")
                                    ) {
                                        Text(
                                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notification Bell",
                                tint = if (unreadCount > 0) GoldAccent else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Admin Access Button
                    IconButton(
                        onClick = onAdminClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isAdminLoggedIn) GoldContainer else DarkSurfaceElevated)
                            .testTag("admin_access_button")
                    ) {
                        Icon(
                            imageVector = if (isAdminLoggedIn) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                            contentDescription = "Admin Access",
                            tint = if (isAdminLoggedIn) GoldAccent else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expandable Search Bar
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search notices by title or content...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
