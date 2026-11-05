package com.example.ui.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.AutoDeleteOption
import com.example.data.repository.ThemeOption
import com.example.ui.components.GroupPosition
import com.example.ui.components.toCardShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isNotificationAccessEnabled: Boolean,
    onOpenNotificationAccessSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showAutoDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = colorScheme.onPrimary,
        checkedTrackColor = colorScheme.primary,
        checkedBorderColor = colorScheme.primary,
        uncheckedThumbColor = colorScheme.onSurfaceVariant,
        uncheckedTrackColor = colorScheme.surfaceContainerHighest,
        uncheckedBorderColor = colorScheme.outlineVariant
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surfaceContainer,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("settings_list"),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsSectionHeader(text = stringResource(R.string.settings_section_appearance))
            }

            item {
                SettingsRowCard(
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = themeLabel(settings.themeOption),
                    groupPosition = GroupPosition.TOP,
                    onClick = { showThemeDialog = true },
                    testTag = "settings_row_theme"
                )
                Spacer(modifier = Modifier.height(2.dp))
                SettingsRowCard(
                    title = stringResource(R.string.settings_dynamic_color_title),
                    subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                    groupPosition = GroupPosition.BOTTOM,
                    onClick = { viewModel.updateDynamicColorEnabled(!settings.dynamicColorEnabled) },
                    testTag = "settings_row_dynamic_color_switch",
                    trailingContent = {
                        Switch(
                            checked = settings.dynamicColorEnabled,
                            onCheckedChange = { viewModel.updateDynamicColorEnabled(it) },
                            colors = switchColors,
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(text = stringResource(R.string.settings_section_storage))
            }

            item {
                SettingsRowCard(
                    title = stringResource(R.string.settings_auto_delete_enable_title),
                    subtitle = stringResource(R.string.settings_auto_delete_enable_subtitle),
                    groupPosition = GroupPosition.TOP,
                    onClick = { viewModel.updateAutoDeleteEnabled(!settings.autoDeleteEnabled) },
                    testTag = "settings_row_auto_delete_switch",
                    trailingContent = {
                        Switch(
                            checked = settings.autoDeleteEnabled,
                            onCheckedChange = { viewModel.updateAutoDeleteEnabled(it) },
                            colors = switchColors,
                            modifier = Modifier.testTag("auto_delete_switch")
                        )
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                SettingsRowCard(
                    title = stringResource(R.string.settings_auto_delete_title),
                    subtitle = autoDeleteLabel(settings.autoDeleteOption),
                    groupPosition = GroupPosition.BOTTOM,
                    onClick = { showAutoDeleteDialog = true },
                    testTag = "settings_row_auto_delete_period"
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(text = stringResource(R.string.settings_section_system))
            }

            item {
                SettingsRowCard(
                    title = stringResource(R.string.settings_notification_access_title),
                    subtitle = stringResource(
                        if (isNotificationAccessEnabled) {
                            R.string.settings_notification_access_enabled
                        } else {
                            R.string.settings_notification_access_disabled
                        }
                    ),
                    groupPosition = GroupPosition.TOP,
                    onClick = onOpenNotificationAccessSettings,
                    testTag = "settings_row_notification_access"
                )
                Spacer(modifier = Modifier.height(2.dp))
                SettingsRowCard(
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.settings_about_subtitle),
                    groupPosition = GroupPosition.BOTTOM,
                    onClick = null,
                    testTag = "settings_row_about"
                )
            }
        }
    }

    if (showThemeDialog) {
        OptionSelectionDialog(
            title = stringResource(R.string.settings_theme_title),
            options = ThemeOption.entries,
            selectedOption = settings.themeOption,
            optionLabel = { themeLabel(it) },
            onSelectOption = {
                viewModel.updateTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAutoDeleteDialog) {
        OptionSelectionDialog(
            title = stringResource(R.string.settings_auto_delete_title),
            options = AutoDeleteOption.entries,
            selectedOption = settings.autoDeleteOption,
            optionLabel = { autoDeleteLabel(it) },
            onSelectOption = {
                viewModel.updateAutoDeleteOption(it)
                showAutoDeleteDialog = false
            },
            onDismiss = { showAutoDeleteDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRowCard(
    title: String,
    subtitle: String,
    groupPosition: GroupPosition,
    onClick: (() -> Unit)?,
    testTag: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val shape = remember(groupPosition) { groupPosition.toCardShape() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = if (isPressed) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .testTag(testTag),
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

@Composable
private fun <T> OptionSelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onSelectOption: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainerHigh,
        titleContentColor = colorScheme.onSurface,
        textContentColor = colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    val selected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .selectable(
                                selected = selected,
                                onClick = { onSelectOption(option) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colorScheme.primary,
                                unselectedColor = colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    color = colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun themeLabel(option: ThemeOption): String = when (option) {
    ThemeOption.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeOption.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeOption.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun autoDeleteLabel(option: AutoDeleteOption): String = when (option) {
    AutoDeleteOption.OFF -> stringResource(R.string.settings_auto_delete_off)
    AutoDeleteOption.DAYS_7 -> stringResource(R.string.settings_auto_delete_7_days)
    AutoDeleteOption.DAYS_30 -> stringResource(R.string.settings_auto_delete_30_days)
    AutoDeleteOption.DAYS_90 -> stringResource(R.string.settings_auto_delete_90_days)
}
