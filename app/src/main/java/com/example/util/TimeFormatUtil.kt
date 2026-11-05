package com.example.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

object TimeFormatUtil {

    private val dayHeaderFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    private val exactDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.getDefault())

    fun getEpochDay(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate().toEpochDay()
    }

    fun formatDayHeader(
        timestamp: Long,
        todayLabel: String = "Today",
        yesterdayLabel: String = "Yesterday",
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val itemDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        val daysDiff = ChronoUnit.DAYS.between(itemDate, today)
        return when (daysDiff) {
            0L -> todayLabel
            1L -> yesterdayLabel
            else -> itemDate.format(dayHeaderFormatter)
        }
    }

    fun formatRelativeTime(
        timestamp: Long,
        nowMillis: Long = System.currentTimeMillis(),
        justNowLabel: String = "Now",
        yesterdayLabel: String = "Yesterday",
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val diffMillis = max(0L, nowMillis - timestamp)
        val minutes = diffMillis / 60_000L
        if (minutes < 1L) return justNowLabel
        if (minutes < 60L) return "${minutes}m"

        val itemDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(itemDate, today)

        return when {
            daysDiff == 0L -> {
                val hours = max(1L, minutes / 60L)
                "${hours}h"
            }
            daysDiff == 1L -> yesterdayLabel
            else -> itemDate.format(shortDateFormatter)
        }
    }

    fun formatExactDateTime(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).format(exactDateTimeFormatter)
    }
}
