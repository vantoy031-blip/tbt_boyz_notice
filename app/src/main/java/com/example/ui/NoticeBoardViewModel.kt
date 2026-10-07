package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.Notice
import com.example.data.repository.NoticeRepository
import com.example.data.repository.NoticeStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NoticeBoardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = NoticeRepository(database.noticeDao())
    val userPreferences = UserPreferences(application)

    // Category filter
    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected Notice for full details view
    private val _selectedNotice = MutableStateFlow<Notice?>(null)
    val selectedNotice: StateFlow<Notice?> = _selectedNotice.asStateFlow()

    // Admin UI states
    private val _showAdminLoginDialog = MutableStateFlow(false)
    val showAdminLoginDialog: StateFlow<Boolean> = _showAdminLoginDialog.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(userPreferences.isAdminLoggedIn)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _showAdminDashboard = MutableStateFlow(false)
    val showAdminDashboard: StateFlow<Boolean> = _showAdminDashboard.asStateFlow()

    private val _showNoticeFormDialog = MutableStateFlow(false)
    val showNoticeFormDialog: StateFlow<Boolean> = _showNoticeFormDialog.asStateFlow()

    private val _editingNotice = MutableStateFlow<Notice?>(null)
    val editingNotice: StateFlow<Notice?> = _editingNotice.asStateFlow()

    // Read status tracker version to force recomposition when items marked as read
    private val _readStateVersion = MutableStateFlow(0)
    val readStateVersion: StateFlow<Int> = _readStateVersion.asStateFlow()

    // Pull-to-refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Flow of active notices from repository
    val activeNotices: StateFlow<List<Notice>> = repository.activeNoticesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flow of all notices for Admin
    val allNotices: StateFlow<List<Notice>> = repository.allNoticesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered notices for normal user feed
    val filteredNotices: StateFlow<List<Notice>> = combine(
        activeNotices,
        _selectedCategory,
        _searchQuery
    ) { notices, category, query ->
        notices.filter { notice ->
            val matchesCategory = if (category.equals("all", ignoreCase = true)) {
                true
            } else if (category.equals("important", ignoreCase = true)) {
                notice.isImportant || notice.category.equals("important", ignoreCase = true)
            } else {
                notice.category.equals(category, ignoreCase = true)
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                notice.title.contains(query, ignoreCase = true) ||
                        notice.description.contains(query, ignoreCase = true)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Unread count
    val unreadCount: StateFlow<Int> = combine(
        activeNotices,
        _readStateVersion
    ) { notices, _ ->
        notices.count { !userPreferences.isNoticeRead(it.id, it.publishedAt) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Statistics for admin dashboard
    val statistics: StateFlow<NoticeStats> = repository.getStatisticsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NoticeStats(0, 0, 0, 0)
        )

    // Firebase Realtime Sync Status
    val isFirebaseConnected: StateFlow<Boolean> = com.example.data.firebase.FirebaseManager.isFirebaseConnected
    val firebaseSyncStatus: StateFlow<String> = com.example.data.firebase.FirebaseManager.syncStatusMessage

    init {
        viewModelScope.launch {
            repository.ensureInitialData()
            com.example.data.firebase.FirebaseManager.initialize(
                application,
                database.noticeDao(),
                viewModelScope
            )
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun refreshNotices(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = com.example.data.firebase.FirebaseManager.refreshFromFirestore(database.noticeDao())
            _isRefreshing.value = false
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                onComplete(true, "Synced with Firestore ($count notices)")
            } else {
                onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "Sync completed")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openNoticeDetails(notice: Notice) {
        _selectedNotice.value = notice
        userPreferences.markNoticeAsRead(notice.id)
        _readStateVersion.value += 1
    }

    fun openNoticeById(id: String) {
        viewModelScope.launch {
            val entity = database.noticeDao().getNoticeById(id)
            if (entity != null) {
                openNoticeDetails(entity.toDomain())
            }
        }
    }

    fun closeNoticeDetails() {
        _selectedNotice.value = null
    }

    fun markAllAsRead() {
        userPreferences.markAllAsViewed(System.currentTimeMillis())
        _readStateVersion.value += 1
    }

    fun isNoticeUnread(notice: Notice): Boolean {
        return !userPreferences.isNoticeRead(notice.id, notice.publishedAt)
    }

    // Admin Authentication & Navigation
    fun openAdminAccess() {
        if (_isAdminLoggedIn.value) {
            _showAdminDashboard.value = true
        } else {
            _showAdminLoginDialog.value = true
        }
    }

    fun closeAdminLogin() {
        _showAdminLoginDialog.value = false
    }

    fun loginAdmin(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        viewModelScope.launch {
            var firebaseAuthSuccess = false

            // Attempt Firebase Auth sign-in if online
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val authResult = auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                if (authResult.user != null) {
                    firebaseAuthSuccess = true
                    userPreferences.setCustomAdminCredentials(trimmedEmail, trimmedPass)
                }
            } catch (e: Exception) {
                android.util.Log.d("NoticeBoardVM", "Firebase Auth note: ${e.message}")
            }

            // Check local credentials
            val localMatch = userPreferences.checkAdminCredentials(trimmedEmail, trimmedPass)

            if (firebaseAuthSuccess || localMatch) {
                userPreferences.isAdminLoggedIn = true
                _isAdminLoggedIn.value = true
                _showAdminLoginDialog.value = false
                _showAdminDashboard.value = true
                onResult(true, null)
            } else {
                onResult(
                    false,
                    "Password does not match. Tap 'Set as my Admin Password' below to use this password."
                )
            }
        }
    }

    fun setCustomAdminPassword(email: String, newPassword: String) {
        val trimmedEmail = email.trim()
        val trimmedPass = newPassword.trim()
        userPreferences.setCustomAdminCredentials(trimmedEmail, trimmedPass)
        com.example.data.firebase.FirebaseManager.loginAdminFirebase(trimmedEmail, trimmedPass)
        userPreferences.isAdminLoggedIn = true
        _isAdminLoggedIn.value = true
        _showAdminLoginDialog.value = false
        _showAdminDashboard.value = true
    }

    fun logoutAdmin() {
        userPreferences.logoutAdmin()
        _isAdminLoggedIn.value = false
        _showAdminDashboard.value = false
    }

    fun closeAdminDashboard() {
        _showAdminDashboard.value = false
    }

    // Notice CRUD actions
    fun openCreateNotice() {
        _editingNotice.value = null
        _showNoticeFormDialog.value = true
    }

    fun openEditNotice(notice: Notice) {
        _editingNotice.value = notice
        _showNoticeFormDialog.value = true
    }

    fun closeNoticeForm() {
        _showNoticeFormDialog.value = false
        _editingNotice.value = null
    }

    fun saveNotice(
        title: String,
        description: String,
        category: String,
        isImportant: Boolean,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            val editing = _editingNotice.value
            if (editing != null) {
                val updated = editing.copy(
                    title = title,
                    description = description,
                    category = category,
                    isImportant = isImportant,
                    isPinned = isPinned
                )
                repository.updateNotice(updated)
            } else {
                val created = repository.createNotice(
                    title = title,
                    description = description,
                    category = category,
                    isImportant = isImportant,
                    isPinned = isPinned
                )
                com.example.notification.NoticeNotificationHelper.showNoticeNotification(
                    context = getApplication(),
                    notice = created
                )
            }
            closeNoticeForm()
        }
    }

    fun sendTestNotification() {
        com.example.notification.NoticeNotificationHelper.sendTestNotification(getApplication())
    }

    fun deleteNotice(id: String) {
        viewModelScope.launch {
            repository.deleteNotice(id)
            if (_selectedNotice.value?.id == id) {
                _selectedNotice.value = null
            }
        }
    }

    fun deleteAllNotices() {
        viewModelScope.launch {
            repository.deleteAllNotices()
            _selectedNotice.value = null
        }
    }

    fun togglePin(id: String) {
        viewModelScope.launch {
            repository.togglePin(id)
        }
    }

    fun toggleImportant(id: String) {
        viewModelScope.launch {
            repository.toggleImportant(id)
        }
    }

    fun toggleArchive(id: String) {
        viewModelScope.launch {
            repository.toggleArchive(id)
        }
    }
}
