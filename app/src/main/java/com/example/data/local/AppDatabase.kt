package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [NoticeEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noticeDao(): NoticeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tbt_notices.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialNotices(database.noticeDao())
                    }
                }
            }

            private suspend fun populateInitialNotices(dao: NoticeDao) {
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
                dao.insertAll(seedNotices)
            }
        }
    }
}
