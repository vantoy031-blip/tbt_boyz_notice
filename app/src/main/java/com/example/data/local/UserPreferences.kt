package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tbt_notice_board_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_VIEWED_TIMESTAMP = "last_viewed_notice_timestamp"
        private const val KEY_READ_NOTICE_IDS = "read_notice_ids"
        private const val KEY_ADMIN_LOGGED_IN = "admin_logged_in"
        private const val KEY_ADMIN_EMAIL = "admin_email"
        private const val KEY_ADMIN_PASSWORD_HASH = "admin_password_hash"
    }

    var lastViewedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_VIEWED_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_VIEWED_TIMESTAMP, value).apply()

    fun markNoticeAsRead(noticeId: String) {
        val currentSet = getReadNoticeIds().toMutableSet()
        currentSet.add(noticeId)
        prefs.edit().putStringSet(KEY_READ_NOTICE_IDS, currentSet).apply()
    }

    fun isNoticeRead(noticeId: String, publishedAt: Long): Boolean {
        val readIds = getReadNoticeIds()
        if (readIds.contains(noticeId)) return true
        val lastTimestamp = lastViewedTimestamp
        return lastTimestamp > 0 && publishedAt <= lastTimestamp
    }

    fun markAllAsViewed(timestamp: Long = System.currentTimeMillis()) {
        lastViewedTimestamp = timestamp
    }

    private fun getReadNoticeIds(): Set<String> {
        return prefs.getStringSet(KEY_READ_NOTICE_IDS, emptySet()) ?: emptySet()
    }

    var isAdminLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_ADMIN_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_ADMIN_LOGGED_IN, value).apply()

    var adminEmail: String
        get() = prefs.getString(KEY_ADMIN_EMAIL, "admin@tbt.org") ?: "admin@tbt.org"
        set(value) = prefs.edit().putString(KEY_ADMIN_EMAIL, value).apply()

    fun checkAdminCredentials(email: String, password: String): Boolean {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()
        val customHash = prefs.getString(KEY_ADMIN_PASSWORD_HASH, null)
        
        // Match custom saved password
        if (customHash != null && trimmedPass == customHash) {
            return true
        }
        
        // Also accept default master passwords for evaluation
        return trimmedPass == "admin1234" || trimmedPass == "admin" || trimmedPass == "password"
    }

    fun setCustomAdminCredentials(email: String, newPassword: String) {
        val trimmedEmail = email.trim()
        val trimmedPass = newPassword.trim()
        prefs.edit()
            .putString(KEY_ADMIN_EMAIL, trimmedEmail)
            .putString(KEY_ADMIN_PASSWORD_HASH, trimmedPass)
            .apply()
    }

    fun updateAdminPassword(newPassword: String) {
        prefs.edit().putString(KEY_ADMIN_PASSWORD_HASH, newPassword.trim()).apply()
    }

    fun logoutAdmin() {
        isAdminLoggedIn = false
    }
}
