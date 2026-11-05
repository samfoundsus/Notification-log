package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.R
import com.example.data.dao.AppCount
import com.example.data.db.NotificationEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GroupPosition
import com.example.ui.components.NotificationCard
import com.example.util.AppIconProvider
import com.example.util.TimeFormatUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    iconProvider: AppIconProvider,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPackage by viewModel.selectedPackage.collectAsStateWithLifecycle()
    val expandedIds by viewModel.expandedIds.collectAsStateWithLifecycle()
    val appCounts by viewModel.appCounts.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val lazyPagingItems: LazyPagingItems<NotificationEntity> =
        viewModel.pagedNotifications.collectAsLazyPagingItems()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val isEmptyState by remember(lazyPagingItems) {
        derivedStateOf {
            lazyPagingItems.loadState.refresh is LoadState.NotLoading &&
                lazyPagingItems.itemCount == 0
        }
    }

    val isFilteredOrSearching by remember(searchQuery, selectedPackage) {
        derivedStateOf {
            searchQuery.isNotBlank() || selectedPackage != null
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("notification_lazy_column"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "header_search_and_chips", contentType = "search_and_chips") {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SearchPillBar(
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClear = viewModel::clearSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    AppFilterChipRow(
                        totalCount = totalCount,
                        appCounts = appCounts,
                        selectedPackage = selectedPackage,
                        onSelectPackage = viewModel::onSelectPackage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            if (isEmptyState) {
                item(key = "empty_state_item", contentType = "empty_state") {
                    EmptyStateView(
                        isFilteredOrSearching = isFilteredOrSearching,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                var lastDayEpoch: Long? = null
                val count = lazyPagingItems.itemCount

                for (index in 0 until count) {
                    val peeked = lazyPagingItems.peek(index)
                    val currentDayEpoch = peeked?.let { TimeFormatUtil.getEpochDay(it.timestamp) }

                    if (currentDayEpoch != null && currentDayEpoch != lastDayEpoch) {
                        val headerTimestamp = peeked.timestamp
                        stickyHeader(
                            key = "day_header_$currentDayEpoch",
                            contentType = "day_header"
                        ) {
                            DaySectionHeader(timestamp = headerTimestamp)
                        }
                        lastDayEpoch = currentDayEpoch
                    }

                    item(
                        key = peeked?.id ?: "placeholder_$index",
                        contentType = "notification_card"
                    ) {
                        val notification = lazyPagingItems[index]
                        if (notification != null) {
                            val prev = if (index > 0) lazyPagingItems.peek(index - 1) else null
                            val next = if (index < count - 1) lazyPagingItems.peek(index + 1) else null

                            val itemDay = TimeFormatUtil.getEpochDay(notification.timestamp)
                            val prevDay = prev?.let { TimeFormatUtil.getEpochDay(it.timestamp) }
                            val nextDay = next?.let { TimeFormatUtil.getEpochDay(it.timestamp) }

                            val isFirstInGroup = prevDay != itemDay
                            val isLastInGroup = nextDay != itemDay

                            val groupPosition = when {
                                isFirstInGroup && isLastInGroup -> GroupPosition.SINGLE
                                isFirstInGroup -> GroupPosition.TOP
                                isLastInGroup -> GroupPosition.BOTTOM
                                else -> GroupPosition.MIDDLE
                            }

                            val isExpanded = notification.id in expandedIds

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                NotificationCard(
                                    notification = notification,
                                    groupPosition = groupPosition,
                                    expanded = isExpanded,
                                    onToggleExpand = { viewModel.toggleExpanded(notification.id) },
                                    iconProvider = iconProvider
                                )

                                Spacer(
                                    modifier = Modifier.height(
                                        if (isLastInGroup) 8.dp else 2.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPillBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme
    val transparentFromScheme = colorScheme.surfaceContainerHigh.copy(alpha = 0f)

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.testTag("search_pill_bar"),
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(R.string.cd_search_icon),
                tint = colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = {
                        onClear()
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            focusedContainerColor = colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = colorScheme.surfaceContainerHigh,
            disabledContainerColor = colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = transparentFromScheme,
            unfocusedIndicatorColor = transparentFromScheme,
            disabledIndicatorColor = transparentFromScheme,
            cursorColor = colorScheme.primary
        )
    )
}

@Composable
private fun AppFilterChipRow(
    totalCount: Int,
    appCounts: List<AppCount>,
    selectedPackage: String?,
    onSelectPackage: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val allLabel = stringResource(R.string.filter_all)
    val colorScheme = MaterialTheme.colorScheme
    val transparentContainer = colorScheme.surface.copy(alpha = 0f)

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = transparentContainer,
        labelColor = colorScheme.onSurfaceVariant,
        iconColor = colorScheme.onSurfaceVariant,
        selectedContainerColor = colorScheme.secondaryContainer,
        selectedLabelColor = colorScheme.onSecondaryContainer,
        selectedLeadingIconColor = colorScheme.onSecondaryContainer
    )

    LazyRow(
        modifier = modifier.testTag("filter_chips_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "chip_all") {
            val isAllSelected = selectedPackage == null
            FilterChip(
                selected = isAllSelected,
                onClick = { onSelectPackage(null) },
                label = {
                    Text(
                        text = if (totalCount > 0) "$allLabel ($totalCount)" else allLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = if (isAllSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(16.dp),
                colors = chipColors,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isAllSelected,
                    borderColor = colorScheme.outlineVariant,
                    selectedBorderColor = transparentContainer,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                ),
                modifier = Modifier
                    .animateContentSize()
                    .testTag("filter_chip_all")
            )
        }

        items(
            items = appCounts,
            key = { it.packageName }
        ) { appCount ->
            val isSelected = selectedPackage == appCount.packageName
            FilterChip(
                selected = isSelected,
                onClick = { onSelectPackage(appCount.packageName) },
                label = {
                    Text(
                        text = "${appCount.appName} (${appCount.count})",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(16.dp),
                colors = chipColors,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = colorScheme.outlineVariant,
                    selectedBorderColor = transparentContainer,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                ),
                modifier = Modifier
                    .animateContentSize()
                    .testTag("filter_chip_${appCount.packageName}")
            )
        }
    }
}

@Composable
private fun DaySectionHeader(
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    val todayLabel = stringResource(R.string.day_today)
    val yesterdayLabel = stringResource(R.string.day_yesterday)
    val headerText = remember(timestamp, todayLabel, yesterdayLabel) {
        TimeFormatUtil.formatDayHeader(
            timestamp = timestamp,
            todayLabel = todayLabel,
            yesterdayLabel = yesterdayLabel
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
