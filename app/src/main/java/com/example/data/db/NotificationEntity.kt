package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["packageName"]),
        Index(value = ["appName"]),
        Index(value = ["notificationKey"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isOngoing: Boolean = false,
    val notificationKey: String = "",
    val sender: String? = null,
    val isOutgoing: Boolean = false,
    val isGroup: Boolean = false,
    val removedAt: Long? = null,
    val removalReason: String? = null
)
