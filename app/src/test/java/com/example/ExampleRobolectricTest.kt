package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.service.notification.NotificationListenerService
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.NotificationEntity
import com.example.service.NotificationCaptureService
import com.example.ui.components.buildNotificationBodyText
import com.example.ui.theme.darkenScheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun migrationFromV3ToV4PreservesExistingDataAndSchema(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v3_to_v4.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) {
            dbFile.delete()
        }

        // Create a real version 3 SQLite database with the exact v3 Room schema and a sample row
        val sqliteDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqliteDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notifications` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `packageName` TEXT NOT NULL,
                `appName` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `text` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `isOngoing` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        sqliteDb.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_notifications_timestamp` ON `notifications` (`timestamp`)"
        )
        sqliteDb.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_notifications_packageName` ON `notifications` (`packageName`)"
        )
        sqliteDb.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_notifications_appName` ON `notifications` (`appName`)"
        )
        sqliteDb.execSQL(
            """
            INSERT INTO `notifications` (`packageName`, `appName`, `title`, `text`, `timestamp`, `isOngoing`)
            VALUES ('com.whatsapp', 'WhatsApp', 'Alex', 'Legacy v3 message', 1700000000000, 0)
            """.trimIndent()
        )
        sqliteDb.version = 3
        sqliteDb.close()

        // Open via Room at version 4 with MIGRATION_3_4 (no fallbackToDestructiveMigration)
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_3,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()

        val dao = roomDb.notificationDao()
        val count = dao.getTotalSavedCountFlow().first()
        assertEquals(1, count)

        val loadResult = dao.getAllPaged(hideOngoingAndSystem = false).load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        val migratedRow = loadResult.data.single()
        assertEquals("com.whatsapp", migratedRow.packageName)
        assertEquals("WhatsApp", migratedRow.appName)
        assertEquals("Alex", migratedRow.title)
        assertEquals("Legacy v3 message", migratedRow.text)
        assertEquals(1700000000000L, migratedRow.timestamp)
        assertFalse(migratedRow.isOngoing)
        assertEquals("", migratedRow.notificationKey)
        assertNull(migratedRow.sender)
        assertFalse(migratedRow.isOutgoing)
        assertFalse(migratedRow.isGroup)
        assertNull(migratedRow.removedAt)
        assertNull(migratedRow.removalReason)

        roomDb.close()
        dbFile.delete()
    }

    @Test
    fun daoSearchMatchesSenderAndRemovalTrackingUpdatesAllRowsForKey(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.notificationDao()

        val key = "0|com.whatsapp|101|chat_group|1000"
        dao.insert(
            NotificationEntity(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Weekend Trip",
                text = "Hello everyone",
                timestamp = 1700000001000L,
                isOngoing = false,
                notificationKey = key,
                sender = "Naitik",
                isOutgoing = false,
                isGroup = true
            )
        )
        dao.insert(
            NotificationEntity(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Weekend Trip",
                text = "On my way",
                timestamp = 1700000002000L,
                isOngoing = false,
                notificationKey = key,
                sender = "You",
                isOutgoing = true,
                isGroup = true
            )
        )

        assertTrue(
            dao.existsMessage(
                notificationKey = key,
                sender = "Naitik",
                text = "Hello everyone",
                timestamp = 1700000001000L
            )
        )

        val searchResult = dao.searchPaged(
            query = "Naitik",
            hideOngoingAndSystem = false
        ).load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertEquals(1, searchResult.data.size)
        assertEquals("Naitik", searchResult.data.first().sender)

        val removedTime = 1700000099000L
        dao.markRemovedByKey(
            notificationKey = key,
            removedAt = removedTime,
            removalReason = NotificationCaptureService.mapRemovalReason(
                NotificationListenerService.REASON_CANCEL
            )
        )

        val allRows = (dao.getAllPaged(hideOngoingAndSystem = false).load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page).data

        assertEquals(2, allRows.size)
        assertTrue(allRows.all { it.removedAt == removedTime && it.removalReason == "Dismissed" })

        db.close()
    }

    @Test
    fun groupIncomingTextFormatsSenderBoldWithoutDuplicatingPrefix() {
        val formattedAdded = buildNotificationBodyText(
            text = "Hello",
            sender = "Naitik",
            isGroup = true,
            isOutgoing = false,
            boldColor = Color.White
        )
        assertEquals("Naitik: Hello", formattedAdded.text)

        val formattedAlreadyIncluded = buildNotificationBodyText(
            text = "Naitik: Hello",
            sender = "Naitik",
            isGroup = true,
            isOutgoing = false,
            boldColor = Color.White
        )
        assertEquals("Naitik: Hello", formattedAlreadyIncluded.text)
    }

    @Test
    fun removalReasonMappingAndThemeDarkenSchemeWorkCorrectly() {
        assertEquals(
            "Opened",
            NotificationCaptureService.mapRemovalReason(NotificationListenerService.REASON_CLICK)
        )
        assertEquals(
            "Dismissed",
            NotificationCaptureService.mapRemovalReason(NotificationListenerService.REASON_CANCEL)
        )
        assertEquals(
            "Cleared all",
            NotificationCaptureService.mapRemovalReason(NotificationListenerService.REASON_CANCEL_ALL)
        )
        assertEquals(
            "Removed by app",
            NotificationCaptureService.mapRemovalReason(NotificationListenerService.REASON_APP_CANCEL)
        )
        assertEquals(
            "Removed by app",
            NotificationCaptureService.mapRemovalReason(NotificationListenerService.REASON_APP_CANCEL_ALL)
        )
        assertEquals(
            "Snoozed",
            NotificationCaptureService.mapRemovalReason(NotificationListenerService.REASON_SNOOZED)
        )
        assertEquals(
            "Other",
            NotificationCaptureService.mapRemovalReason(999)
        )

        val baseDark = darkColorScheme()
        val darkenedDark = darkenScheme(baseDark, isDark = true)
        assertTrue(darkenedDark.surface.luminance() < baseDark.surface.luminance())
        assertEquals(baseDark.onSurface, darkenedDark.onSurface)
        assertEquals(baseDark.outline, darkenedDark.outline)

        val baseLight = lightColorScheme()
        val darkenedLight = darkenScheme(baseLight, isDark = false)
        assertTrue(darkenedLight.surface.luminance() < baseLight.surface.luminance())
        assertEquals(baseLight.onSurface, darkenedLight.onSurface)
    }
}
