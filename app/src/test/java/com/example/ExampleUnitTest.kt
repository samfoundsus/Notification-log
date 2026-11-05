package com.example

import android.content.ContentValues
import android.database.Cursor
import android.os.CancellationSignal
import android.service.notification.NotificationListenerService
import android.util.Pair
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import com.example.data.db.AppDatabase
import com.example.data.db.NotificationEntity
import com.example.service.NotificationCaptureService
import com.example.ui.components.buildNotificationBodyText
import com.example.ui.theme.darkenScheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun migration3To4AddsAllColumnsAndIndexWithExpectedDefaults() {
        val executedStatements = mutableListOf<String>()
        val fakeDb = FakeSupportSQLiteDatabase(executedStatements)

        AppDatabase.MIGRATION_3_4.migrate(fakeDb)

        assertEquals(7, executedStatements.size)
        assertTrue(
            executedStatements.any {
                it.contains("ADD COLUMN notificationKey TEXT NOT NULL DEFAULT ''")
            }
        )
        assertTrue(
            executedStatements.any {
                it.contains("ADD COLUMN sender TEXT")
            }
        )
        assertTrue(
            executedStatements.any {
                it.contains("ADD COLUMN isOutgoing INTEGER NOT NULL DEFAULT 0")
            }
        )
        assertTrue(
            executedStatements.any {
                it.contains("ADD COLUMN isGroup INTEGER NOT NULL DEFAULT 0")
            }
        )
        assertTrue(
            executedStatements.any {
                it.contains("ADD COLUMN removedAt INTEGER")
            }
        )
        assertTrue(
            executedStatements.any {
                it.contains("ADD COLUMN removalReason TEXT")
            }
        )
        assertTrue(
            executedStatements.any {
                it.contains(
                    "CREATE INDEX IF NOT EXISTS index_notifications_notificationKey ON notifications(notificationKey)"
                )
            }
        )

        val defaultEntity = NotificationEntity(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alex",
            text = "Hey there",
            timestamp = 1700000000000L
        )
        assertEquals("", defaultEntity.notificationKey)
        assertNull(defaultEntity.sender)
        assertFalse(defaultEntity.isOutgoing)
        assertFalse(defaultEntity.isGroup)
        assertNull(defaultEntity.removedAt)
        assertNull(defaultEntity.removalReason)
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

        val outgoingInGroup = buildNotificationBodyText(
            text = "On my way",
            sender = "You",
            isGroup = true,
            isOutgoing = true,
            boldColor = Color.White
        )
        assertEquals("On my way", outgoingInGroup.text)
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
        assertTrue(darkenedDark.surfaceContainer.luminance() < baseDark.surfaceContainer.luminance())
        assertTrue(darkenedDark.secondaryContainer.luminance() < baseDark.secondaryContainer.luminance())
        assertEquals(baseDark.onSurface, darkenedDark.onSurface)
        assertEquals(baseDark.outline, darkenedDark.outline)

        val baseLight = lightColorScheme()
        val darkenedLight = darkenScheme(baseLight, isDark = false)
        assertTrue(darkenedLight.surface.luminance() < baseLight.surface.luminance())
        assertTrue(darkenedLight.surfaceContainer.luminance() < baseLight.surfaceContainer.luminance())
        assertEquals(baseLight.onSurface, darkenedLight.onSurface)
    }

    private class FakeSupportSQLiteDatabase(
        private val executedStatements: MutableList<String>
    ) : SupportSQLiteDatabase {
        override fun execSQL(sql: String) {
            executedStatements.add(sql)
        }

        override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
            executedStatements.add(sql)
        }

        override fun compileStatement(sql: String): SupportSQLiteStatement =
            throw UnsupportedOperationException()
        override fun beginTransaction() {}
        override fun beginTransactionNonExclusive() {}
        override fun beginTransactionWithListener(
            transactionListener: android.database.sqlite.SQLiteTransactionListener
        ) {}
        override fun beginTransactionWithListenerNonExclusive(
            transactionListener: android.database.sqlite.SQLiteTransactionListener
        ) {}
        override fun endTransaction() {}
        override fun setTransactionSuccessful() {}
        override fun inTransaction(): Boolean = false
        override val isDbLockedByCurrentThread: Boolean = false
        override fun yieldIfContendedSafely(): Boolean = false
        override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean = false
        override var version: Int = 3
        override var maximumSize: Long = 0L
        override fun setMaximumSize(numBytes: Long): Long = 0L
        override var pageSize: Long = 4096L
        override fun query(query: String): Cursor = throw UnsupportedOperationException()
        override fun query(query: String, bindArgs: Array<out Any?>): Cursor =
            throw UnsupportedOperationException()
        override fun query(query: SupportSQLiteQuery): Cursor =
            throw UnsupportedOperationException()
        override fun query(
            query: SupportSQLiteQuery,
            cancellationSignal: CancellationSignal?
        ): Cursor = throw UnsupportedOperationException()
        override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long = 0L
        override fun delete(
            table: String,
            whereClause: String?,
            whereArgs: Array<out Any?>?
        ): Int = 0
        override fun update(
            table: String,
            conflictAlgorithm: Int,
            values: ContentValues,
            whereClause: String?,
            whereArgs: Array<out Any?>?
        ): Int = 0
        override val isReadOnly: Boolean = false
        override val isOpen: Boolean = true
        override fun needUpgrade(newVersion: Int): Boolean = newVersion > version
        override val path: String = ":memory:"
        override fun setLocale(locale: Locale) {}
        override fun setMaxSqlCacheSize(cacheSize: Int) {}
        override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {}
        override fun enableWriteAheadLogging(): Boolean = false
        override fun disableWriteAheadLogging() {}
        override val isWriteAheadLoggingEnabled: Boolean = false
        override val attachedDbs: List<Pair<String, String>>? = emptyList()
        override val isDatabaseIntegrityOk: Boolean = true
        override fun close() {}
    }
}
