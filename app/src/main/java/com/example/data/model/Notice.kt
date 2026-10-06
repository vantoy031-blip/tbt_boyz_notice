package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NoticeCategory(val id: String, val displayName: String) {
    IMPORTANT("important", "Important"),
    EVENT("event", "Event"),
    GENERAL("general", "General"),
    ANNOUNCEMENT("announcement", "Announcement");

    companion object {
        fun fromId(id: String): NoticeCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GENERAL
        }
    }
}

data class Notice(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "important" | "event" | "general" | "announcement"
    val isImportant: Boolean,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val publishedAt: Long
) {
    val categoryEnum: NoticeCategory
        get() = NoticeCategory.fromId(category)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            return sdf.format(Date(publishedAt))
        }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(publishedAt))
        }

    val shortDescription: String
        get() {
            val trimmed = description.trim()
            val firstLine = trimmed.lines().firstOrNull() ?: ""
            return if (firstLine.length > 90) {
                firstLine.take(87) + "..."
            } else if (trimmed.length > 90) {
                firstLine.ifEmpty { trimmed.take(87) + "..." }
            } else {
                trimmed
            }
        }
}
