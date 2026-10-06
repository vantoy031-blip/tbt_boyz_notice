package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Notice

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "important", "event", "general", "announcement"
    val isImportant: Boolean,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val publishedAt: Long
) {
    fun toDomain(): Notice = Notice(
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

    companion object {
        fun fromDomain(notice: Notice): NoticeEntity = NoticeEntity(
            id = notice.id,
            title = notice.title,
            description = notice.description,
            category = notice.category,
            isImportant = notice.isImportant,
            isPinned = notice.isPinned,
            isArchived = notice.isArchived,
            createdAt = notice.createdAt,
            updatedAt = notice.updatedAt,
            publishedAt = notice.publishedAt
        )
    }
}
