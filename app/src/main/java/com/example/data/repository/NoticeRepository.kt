package com.example.data.repository

import com.example.data.firebase.FirebaseManager
import com.example.data.local.NoticeDao
import com.example.data.local.NoticeEntity
import com.example.data.model.Notice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

data class NoticeStats(
    val totalNotices: Int,
    val activeNotices: Int,
    val archivedNotices: Int,
    val importantNotices: Int
)

class NoticeRepository(
    private val noticeDao: NoticeDao
) {

    val activeNoticesFlow: Flow<List<Notice>> =
        noticeDao.getActiveNoticesFlow().map { entities ->
            entities.map { it.toDomain() }
        }

    val allNoticesFlow: Flow<List<Notice>> =
        noticeDao.getAllNoticesFlow().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getStatisticsFlow(): Flow<NoticeStats> {
        return combine(
            noticeDao.getAllNoticesFlow()
        ) { noticesList ->
            val total = noticesList.first().size
            val active = noticesList.first().count { !it.isArchived }
            val archived = noticesList.first().count { it.isArchived }
            val important = noticesList.first().count { it.isImportant && !it.isArchived }
            NoticeStats(
                totalNotices = total,
                activeNotices = active,
                archivedNotices = archived,
                importantNotices = important
            )
        }
    }

    suspend fun createNotice(
        title: String,
        description: String,
        category: String,
        isImportant: Boolean,
        isPinned: Boolean
    ): Notice {
        val now = System.currentTimeMillis()
        val notice = Notice(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            category = category,
            isImportant = isImportant,
            isPinned = isPinned,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            publishedAt = now
        )
        noticeDao.insert(NoticeEntity.fromDomain(notice))
        FirebaseManager.saveNoticeToCloud(notice)
        return notice
    }

    suspend fun updateNotice(notice: Notice) {
        val updated = notice.copy(updatedAt = System.currentTimeMillis())
        noticeDao.update(NoticeEntity.fromDomain(updated))
        FirebaseManager.saveNoticeToCloud(updated)
    }

    suspend fun deleteNotice(id: String) {
        noticeDao.deleteById(id)
        FirebaseManager.deleteNoticeFromCloud(id)
    }

    suspend fun deleteAllNotices() {
        noticeDao.deleteAllNotices()
        FirebaseManager.deleteAllNoticesFromCloud()
    }

    suspend fun togglePin(id: String) {
        val entity = noticeDao.getNoticeById(id) ?: return
        val newPinned = !entity.isPinned
        val updated = entity.copy(
            isPinned = newPinned,
            updatedAt = System.currentTimeMillis()
        )
        noticeDao.update(updated)
        FirebaseManager.updateNoticeFieldsInCloud(
            id,
            mapOf("isPinned" to newPinned, "updatedAt" to updated.updatedAt)
        )
    }

    suspend fun toggleImportant(id: String) {
        val entity = noticeDao.getNoticeById(id) ?: return
        val newImportant = !entity.isImportant
        val updated = entity.copy(
            isImportant = newImportant,
            updatedAt = System.currentTimeMillis()
        )
        noticeDao.update(updated)
        FirebaseManager.updateNoticeFieldsInCloud(
            id,
            mapOf("isImportant" to newImportant, "updatedAt" to updated.updatedAt)
        )
    }

    suspend fun toggleArchive(id: String) {
        val entity = noticeDao.getNoticeById(id) ?: return
        val newArchived = !entity.isArchived
        val updated = entity.copy(
            isArchived = newArchived,
            updatedAt = System.currentTimeMillis()
        )
        noticeDao.update(updated)
        FirebaseManager.updateNoticeFieldsInCloud(
            id,
            mapOf("isArchived" to newArchived, "updatedAt" to updated.updatedAt)
        )
    }

    suspend fun cleanupLegacySeedNotices() {
        val seedTitles = listOf(
            "Upcoming TBT Annual Gala & Awards Night",
            "Urgent: Infrastructure Maintenance & Cloud Portal Upgrade",
            "Monthly General Community Meeting",
            "New Community Guidelines & Resource Library Released",
            "Campus Security & Visitor Pass Requirements"
        )
        for (title in seedTitles) {
            noticeDao.deleteByTitle(title)
        }
    }

    suspend fun ensureInitialData() {
        // Purge any legacy dummy/inbuilt seed notices so they are gone for everyone
        cleanupLegacySeedNotices()
    }
}
