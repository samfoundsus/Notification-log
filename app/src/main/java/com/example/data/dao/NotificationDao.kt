package com.example.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC, id DESC")
    fun getAllPaged(): PagingSource<Int, NotificationEntity>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY timestamp DESC, id DESC")
    fun getByPackagePaged(packageName: String): PagingSource<Int, NotificationEntity>

    @Query(
        """
        SELECT * FROM notifications
        WHERE title LIKE '%' || :query || '%'
           OR text LIKE '%' || :query || '%'
           OR appName LIKE '%' || :query || '%'
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun searchPaged(query: String): PagingSource<Int, NotificationEntity>

    @Query(
        """
        SELECT * FROM notifications
        WHERE packageName = :packageName
          AND (title LIKE '%' || :query || '%'
               OR text LIKE '%' || :query || '%'
               OR appName LIKE '%' || :query || '%')
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun searchByPackagePaged(packageName: String, query: String): PagingSource<Int, NotificationEntity>

    @Query(
        """
        SELECT packageName, appName, COUNT(*) AS count
        FROM notifications
        GROUP BY packageName
        ORDER BY count DESC, appName ASC
        """
    )
    fun getDistinctAppsWithCounts(): Flow<List<AppCount>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
