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
    val totalSavedCount: Flow<Int> = dao.getTotalSavedCountFlow()

    fun getDistinctAppsWithCounts(hideOngoingAndSystem: Boolean): Flow<List<AppCount>> {
        return dao.getDistinctAppsWithCounts(hideOngoingAndSystem)
    }

    fun getVisibleCount(hideOngoingAndSystem: Boolean): Flow<Int> {
        return dao.getVisibleCountFlow(hideOngoingAndSystem)
    }

    suspend fun insert(notification: NotificationEntity) {
        dao.insert(notification)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun deleteOlderThan(cutoff: Long) {
        dao.deleteOlderThan(cutoff)
    }

    fun getPagedNotifications(
        packageName: String?,
        searchQuery: String,
        hideOngoingAndSystem: Boolean
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
                        dao.getAllPaged(hideOngoingAndSystem)
                    }
                    !packageName.isNullOrEmpty() && trimmedQuery.isEmpty() -> {
                        dao.getByPackagePaged(packageName, hideOngoingAndSystem)
                    }
                    packageName.isNullOrEmpty() && trimmedQuery.isNotEmpty() -> {
                        dao.searchPaged(trimmedQuery, hideOngoingAndSystem)
                    }
                    else -> {
                        dao.searchByPackagePaged(packageName!!, trimmedQuery, hideOngoingAndSystem)
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
