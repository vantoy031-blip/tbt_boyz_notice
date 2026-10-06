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

    suspend fun ensureInitialData() {
        if (noticeDao.getCount() == 0) {
            val now = System.currentTimeMillis()
            val oneHour = 3600 * 1000L
            val oneDay = 24 * 3600 * 1000L

            val seedNotices = listOf(
                NoticeEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Upcoming TBT Annual Gala & Awards Night",
                    description = "All members, staff, and distinguished partners are cordially invited to the 2026 TBT Annual General Gala and Excellence Awards.\n\nDate: Tomorrow at 8:00 PM\nVenue: Main Auditorium & Live Stream\nAgenda: Strategic Vision 2027, Committee Elections, and Outstanding Achievement Awards.\n\nCocktails and welcome refreshments begin at 7:15 PM in the foyer. Formal business attire requested.",
                    category = "event",
                    isImportant = true,
                    isPinned = true,
                    isArchived = false,
                    createdAt = now - (2 * oneHour),
                    updatedAt = now - (2 * oneHour),
                    publishedAt = now - (2 * oneHour)
                ),
                NoticeEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Urgent: Infrastructure Maintenance & Cloud Portal Upgrade",
                    description = "Please be advised that central portal services and account validation will undergo planned maintenance on October 8, 2026, between 01:00 AM and 05:00 AM UTC.\n\nDuring this window, cached read access will remain available on mobile, while submission forms will be queued. If you encounter any anomalies, please contact tech-support@tbt.org.",
                    category = "important",
                    isImportant = true,
                    isPinned = false,
                    isArchived = false,
                    createdAt = now - (5 * oneHour),
                    updatedAt = now - (5 * oneHour),
                    publishedAt = now - (5 * oneHour)
                ),
                NoticeEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Monthly General Community Meeting",
                    description = "Join our monthly open forum this Thursday to review community milestones, upcoming initiatives, and hear member proposals.\n\nTime: 10:00 AM - 11:30 AM\nFormat: Hybrid (Conference Hall B / Video Link)\nAll registered members are encouraged to participate and vote on upcoming initiatives.",
                    category = "event",
                    isImportant = false,
                    isPinned = false,
                    isArchived = false,
                    createdAt = now - oneDay,
                    updatedAt = now - oneDay,
                    publishedAt = now - oneDay
                ),
                NoticeEntity(
                    id = UUID.randomUUID().toString(),
                    title = "New Community Guidelines & Resource Library Released",
                    description = "The updated 2026 community handbook and open digital resource library are now officially available.\n\nThe library includes reference kits, collaborative templates, and standards for open knowledge sharing. Access the digital repository anytime through the member services link.",
                    category = "announcement",
                    isImportant = false,
                    isPinned = false,
                    isArchived = false,
                    createdAt = now - (2 * oneDay),
                    updatedAt = now - (2 * oneDay),
                    publishedAt = now - (2 * oneDay)
                ),
                NoticeEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Campus Security & Visitor Pass Requirements",
                    description = "A friendly reminder to all visitors and guests: identification passes must be requested at the reception desk on Ground Floor and worn visibly at all times within the building.\n\nThank you for your cooperation in maintaining a safe and professional environment for everyone.",
                    category = "general",
                    isImportant = false,
                    isPinned = false,
                    isArchived = false,
                    createdAt = now - (3 * oneDay),
                    updatedAt = now - (3 * oneDay),
                    publishedAt = now - (3 * oneDay)
                )
            )
            noticeDao.insertAll(seedNotices)
        }
    }
}
