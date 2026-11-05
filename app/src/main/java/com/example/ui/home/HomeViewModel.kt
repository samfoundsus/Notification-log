package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.dao.AppCount
import com.example.data.db.NotificationEntity
import com.example.data.repository.NotificationRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: NotificationRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPackage = MutableStateFlow<String?>(null)
    val selectedPackage: StateFlow<String?> = _selectedPackage.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedIds: StateFlow<Set<Long>> = _expandedIds.asStateFlow()

    private val hideOngoingAndSystemFlow: Flow<Boolean> = settingsRepository.settingsFlow
        .map { it.hideOngoingAndSystem }
        .distinctUntilChanged()

    val appCounts: StateFlow<List<AppCount>> = hideOngoingAndSystemFlow
        .flatMapLatest { hideOngoing ->
            repository.getDistinctAppsWithCounts(hideOngoing)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> = hideOngoingAndSystemFlow
        .flatMapLatest { hideOngoing ->
            repository.getVisibleCount(hideOngoing)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    private val debouncedQuery: Flow<String> = _searchQuery
        .debounce { query -> if (query.isEmpty()) 0L else 250L }
        .distinctUntilChanged()

    val pagedNotifications: Flow<PagingData<NotificationEntity>> = combine(
        _selectedPackage,
        debouncedQuery,
        hideOngoingAndSystemFlow
    ) { pkg, query, hideOngoing ->
         FilterParams(pkg, query, hideOngoing)
    }.flatMapLatest { params ->
        repository.getPagedNotifications(
            packageName = params.packageName,
            searchQuery = params.searchQuery,
            hideOngoingAndSystem = params.hideOngoingAndSystem
        )
    }.cachedIn(viewModelScope)

    // Holds only the single most recently swiped row while the Undo Snackbar is visible
    @Volatile
    private var pendingDeletedNotification: NotificationEntity? = null

    private val _deletedSnackbarChannel = Channel<Long>(Channel.CONFLATED)
    val deletedSnackbarEvents: Flow<Long> = _deletedSnackbarChannel.receiveAsFlow()

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun onSelectPackage(packageName: String?) {
        _selectedPackage.update { current ->
            if (current == packageName) null else packageName
        }
    }

    fun toggleExpanded(notificationId: Long) {
        _expandedIds.update { current ->
            if (notificationId in current) {
                current - notificationId
            } else {
                current + notificationId
            }
        }
    }

    fun deleteNotification(notification: NotificationEntity) {
        pendingDeletedNotification = notification
        _expandedIds.update { it - notification.id }
        viewModelScope.launch {
            repository.deleteById(notification.id)
            _deletedSnackbarChannel.trySend(notification.id)
        }
    }

    fun undoLastDelete() {
        val toRestore = pendingDeletedNotification ?: return
        pendingDeletedNotification = null
        viewModelScope.launch {
            repository.insert(toRestore)
        }
    }

    fun clearPendingDeleted() {
        pendingDeletedNotification = null
    }

    private data class FilterParams(
        val packageName: String?,
        val searchQuery: String,
        val hideOngoingAndSystem: Boolean
    )

    class Factory(
        private val repository: NotificationRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, settingsRepository) as T
        }
    }
}
