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

    @Query(
        """
        SELECT * FROM notifications
        WHERE (:hideOngoingAndSystem = 0 OR (
            isOngoing = 0
            AND packageName NOT IN (
                'android',
                'com.android.systemui',
                'com.android.settings',
                'com.google.android.gms',
                'com.android.providers.downloads',
                'com.android.packageinstaller',
                'com.google.android.packageinstaller',
                'com.android.permissioncontroller',
                'com.google.android.permissioncontroller',
                'com.android.bluetooth',
                'com.android.nfc',
                'com.android.shell'
            )
            AND packageName NOT LIKE 'com.android.providers.%'
            AND LOWER(title) NOT LIKE '%updating messages%'
            AND LOWER(text) NOT LIKE '%updating messages%'
            AND LOWER(text) NOT LIKE '%checking for new messages%'
        ))
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun getAllPaged(hideOngoingAndSystem: Boolean): PagingSource<Int, NotificationEntity>

    @Query(
        """
        SELECT * FROM notifications
        WHERE packageName = :packageName
          AND (:hideOngoingAndSystem = 0 OR (
              isOngoing = 0
              AND packageName NOT IN (
                  'android',
                  'com.android.systemui',
                  'com.android.settings',
                  'com.google.android.gms',
                  'com.android.providers.downloads',
                  'com.android.packageinstaller',
                  'com.google.android.packageinstaller',
                  'com.android.permissioncontroller',
                  'com.google.android.permissioncontroller',
                  'com.android.bluetooth',
                  'com.android.nfc',
                  'com.android.shell'
              )
              AND packageName NOT LIKE 'com.android.providers.%'
              AND LOWER(title) NOT LIKE '%updating messages%'
              AND LOWER(text) NOT LIKE '%updating messages%'
              AND LOWER(text) NOT LIKE '%checking for new messages%'
          ))
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun getByPackagePaged(
        packageName: String,
        hideOngoingAndSystem: Boolean
    ): PagingSource<Int, NotificationEntity>

    @Query(
        """
        SELECT * FROM notifications
        WHERE (title LIKE '%' || :query || '%'
               OR text LIKE '%' || :query || '%'
               OR appName LIKE '%' || :query || '%'
               OR COALESCE(sender, '') LIKE '%' || :query || '%')
          AND (:hideOngoingAndSystem = 0 OR (
              isOngoing = 0
              AND packageName NOT IN (
                  'android',
                  'com.android.systemui',
                  'com.android.settings',
                  'com.google.android.gms',
                  'com.android.providers.downloads',
                  'com.android.packageinstaller',
                  'com.google.android.packageinstaller',
                  'com.android.permissioncontroller',
                  'com.google.android.permissioncontroller',
                  'com.android.bluetooth',
                  'com.android.nfc',
                  'com.android.shell'
              )
              AND packageName NOT LIKE 'com.android.providers.%'
              AND LOWER(title) NOT LIKE '%updating messages%'
              AND LOWER(text) NOT LIKE '%updating messages%'
              AND LOWER(text) NOT LIKE '%checking for new messages%'
          ))
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun searchPaged(
        query: String,
        hideOngoingAndSystem: Boolean
    ): PagingSource<Int, NotificationEntity>

    @Query(
        """
        SELECT * FROM notifications
        WHERE packageName = :packageName
          AND (title LIKE '%' || :query || '%'
               OR text LIKE '%' || :query || '%'
               OR appName LIKE '%' || :query || '%'
               OR COALESCE(sender, '') LIKE '%' || :query || '%')
          AND (:hideOngoingAndSystem = 0 OR (
              isOngoing = 0
              AND packageName NOT IN (
                  'android',
                  'com.android.systemui',
                  'com.android.settings',
                  'com.google.android.gms',
                  'com.android.providers.downloads',
                  'com.android.packageinstaller',
                  'com.google.android.packageinstaller',
                  'com.android.permissioncontroller',
                  'com.google.android.permissioncontroller',
                  'com.android.bluetooth',
                  'com.android.nfc',
                  'com.android.shell'
              )
              AND packageName NOT LIKE 'com.android.providers.%'
              AND LOWER(title) NOT LIKE '%updating messages%'
              AND LOWER(text) NOT LIKE '%updating messages%'
              AND LOWER(text) NOT LIKE '%checking for new messages%'
          ))
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun searchByPackagePaged(
        packageName: String,
        query: String,
        hideOngoingAndSystem: Boolean
    ): PagingSource<Int, NotificationEntity>

    @Query(
        """
        SELECT packageName, appName, COUNT(*) AS count
        FROM notifications
        WHERE (:hideOngoingAndSystem = 0 OR (
            isOngoing = 0
            AND packageName NOT IN (
                'android',
                'com.android.systemui',
                'com.android.settings',
                'com.google.android.gms',
                'com.android.providers.downloads',
                'com.android.packageinstaller',
                'com.google.android.packageinstaller',
                'com.android.permissioncontroller',
                'com.google.android.permissioncontroller',
                'com.android.bluetooth',
                'com.android.nfc',
                'com.android.shell'
            )
            AND packageName NOT LIKE 'com.android.providers.%'
            AND LOWER(title) NOT LIKE '%updating messages%'
            AND LOWER(text) NOT LIKE '%updating messages%'
            AND LOWER(text) NOT LIKE '%checking for new messages%'
        ))
        GROUP BY packageName
        ORDER BY count DESC, appName ASC
        """
    )
    fun getDistinctAppsWithCounts(hideOngoingAndSystem: Boolean): Flow<List<AppCount>>

    @Query(
        """
        SELECT COUNT(*) FROM notifications
        WHERE (:hideOngoingAndSystem = 0 OR (
            isOngoing = 0
            AND packageName NOT IN (
                'android',
                'com.android.systemui',
                'com.android.settings',
                'com.google.android.gms',
                'com.android.providers.downloads',
                'com.android.packageinstaller',
                'com.google.android.packageinstaller',
                'com.android.permissioncontroller',
                'com.google.android.permissioncontroller',
                'com.android.bluetooth',
                'com.android.nfc',
                'com.android.shell'
            )
            AND packageName NOT LIKE 'com.android.providers.%'
            AND LOWER(title) NOT LIKE '%updating messages%'
            AND LOWER(text) NOT LIKE '%updating messages%'
            AND LOWER(text) NOT LIKE '%checking for new messages%'
        ))
        """
    )
    fun getVisibleCountFlow(hideOngoingAndSystem: Boolean): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    fun getTotalSavedCountFlow(): Flow<Int>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM notifications
            WHERE notificationKey = :notificationKey
              AND ((sender IS NULL AND :sender IS NULL) OR sender = :sender)
              AND text = :text
              AND timestamp = :timestamp
        )
        """
    )
    suspend fun existsMessage(
        notificationKey: String,
        sender: String?,
        text: String,
        timestamp: Long
    ): Boolean

    @Query(
        """
        SELECT * FROM notifications
        WHERE notificationKey = :notificationKey
        ORDER BY timestamp DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun getLatestByKey(notificationKey: String): NotificationEntity?

    @Query(
        """
        UPDATE notifications
        SET removedAt = :removedAt,
            removalReason = :removalReason
        WHERE notificationKey = :notificationKey
          AND removedAt IS NULL
        """
    )
    suspend fun markRemovedByKey(
        notificationKey: String,
        removedAt: Long,
        removalReason: String
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM notifications WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
