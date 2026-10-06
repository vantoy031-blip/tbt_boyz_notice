package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoticeDao {

    // Main active notices query sorted by Pinned -> Important -> Newest publishedAt
    @Query("""
        SELECT * FROM notices 
        WHERE isArchived = 0 
        ORDER BY isPinned DESC, isImportant DESC, publishedAt DESC
    """)
    fun getActiveNoticesFlow(): Flow<List<NoticeEntity>>

    // All notices for Admin (including active & archived)
    @Query("""
        SELECT * FROM notices 
        ORDER BY isPinned DESC, isImportant DESC, publishedAt DESC
    """)
    fun getAllNoticesFlow(): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE id = :id LIMIT 1")
    suspend fun getNoticeById(id: String): NoticeEntity?

    @Query("SELECT COUNT(*) FROM notices WHERE isArchived = 0")
    fun getActiveCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notices WHERE isArchived = 1")
    fun getArchivedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notices WHERE isImportant = 1 AND isArchived = 0")
    fun getImportantCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notices")
    suspend fun getCount(): Int

    @Query("SELECT * FROM notices")
    suspend fun getAllNoticesList(): List<NoticeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notice: NoticeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notices: List<NoticeEntity>)

    @Update
    suspend fun update(notice: NoticeEntity)

    @Delete
    suspend fun delete(notice: NoticeEntity)

    @Query("DELETE FROM notices WHERE id = :id")
    suspend fun deleteById(id: String)
}
