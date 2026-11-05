package com.example.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.data.dao.AppCount
import com.example.data.dao.NotificationDao
import com.example.data.db.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val dao: NotificationDao
) {
    val distinctAppsWithCounts: Flow<List<AppCount>> = dao.getDistinctAppsWithCounts()
    val totalCount: Flow<Int> = dao.getTotalCountFlow()

    suspend fun insert(notification: NotificationEntity) {
        dao.insert(notification)
    }

    fun getPagedNotifications(
        packageName: String?,
        searchQuery: String
    ): Flow<PagingData<NotificationEntity>> {
        val trimmedQuery = searchQuery.trim()
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE
            ),
            pagingSourceFactory = {
                when {
                    packageName.isNullOrEmpty() && trimmedQuery.isEmpty() -> {
                        dao.getAllPaged()
                    }
                    !packageName.isNullOrEmpty() && trimmedQuery.isEmpty() -> {
                        dao.getByPackagePaged(packageName)
                    }
                    packageName.isNullOrEmpty() && trimmedQuery.isNotEmpty() -> {
                        dao.searchPaged(trimmedQuery)
                    }
                    else -> {
                        dao.searchByPackagePaged(packageName!!, trimmedQuery)
                    }
                }
            }
        ).flow
    }

    companion object {
        const val PAGE_SIZE = 30
        private const val PREFETCH_DISTANCE = 15
    }
}
