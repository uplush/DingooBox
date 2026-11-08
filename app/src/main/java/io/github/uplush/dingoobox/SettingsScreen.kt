package io.github.uplush.dingoobox

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Direct layout port of MiNiQ SettingsScreen at source baseline
 * 596088d5c684d7990ef27d5f8e49d1897205ece6. AppSettingsState is the
 * Dingoo adapter that preserves the source preference values and callbacks.
 */
private enum class SettingsTab(val titleResource: Int) {
    General(R.string.settings_tab_general),
    Display(R.string.settings_tab_display),
    Audio(R.string.settings_tab_audio),
    Advanced(R.string.settings_tab_advanced)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    inGame: Boolean = false,
    embedded: Boolean = false,
    onSettingsChanged: (AppSettingsState) -> Unit,
    onBack: () -> Unit = {}
) {
    if (!embedded) BackHandler(onBack = onBack)

    val content: @Composable (Modifier) -> Unit = { modifier ->
        SettingsContent(
            settings = settings,
            embeddedInPauseMenu = embedded && inGame,
            onSettingsChanged = onSettingsChanged,
            modifier = modifier
        )
    }

    if (embedded) {
        content(Modifier)
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = appStringResource(R.string.action_back)
                        )
                    }

                    Text(
                        text = appStringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                content(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: AppSettingsState,
    embeddedInPauseMenu: Boolean,
    onSettingsChanged: (AppSettingsState) -> Unit,
    modifier: Modifier = Modifier
) {
    val pauseMenuPageFocusRequester = LocalPauseMenuPageFocusRequester.current
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val aspectRatioSettingEnabled = embeddedInPauseMenu && isLandscape

    var selectedTab by remember { mutableStateOf(SettingsTab.General) }
    var orientationDialogVisible by remember { mutableStateOf(false) }
    var languageDialogVisible by remember { mutableStateOf(false) }
    var themeDialogVisible by remember { mutableStateOf(false) }
    var aspectRatioDialogVisible by remember(aspectRatioSettingEnabled) {
        mutableStateOf(false)
    }
    var imageFilterDialogVisible by remember { mutableStateOf(false) }
    var fastForwardDialogVisible by remember { mutableStateOf(false) }

    if (orientationDialogVisible) {
        SettingsSelectionDialog(
            title = appStringResource(R.string.settings_screen_orientation_title),
            options = ScreenOrientationMode.entries,
            selected = settings.orientation,
            label = { appStringResource(it.displayNameResource) },
            onSelect = {
                onSettingsChanged(settings.copy(orientation = it))
                orientationDialogVisible = false
            },
            onDismiss = { orientationDialogVisible = false }
        )
    }

    if (languageDialogVisible) {
        SettingsSelectionDialog(
            title = appStringResource(R.string.settings_language_title),
            options = AppLanguage.entries,
            selected = settings.language,
            label = { appStringResource(it.displayNameResource) },
            onSelect = {
                onSettingsChanged(settings.copy(language = it))
                languageDialogVisible = false
            },
            onDismiss = { languageDialogVisible = false }
        )
    }

    if (themeDialogVisible) {
        SettingsSelectionDialog(
            title = appStringResource(R.string.settings_theme_title),
            options = AppThemeMode.entries,
            selected = settings.themeMode,
            label = { appStringResource(it.displayNameResource) },
            onSelect = {
                onSettingsChanged(settings.copy(themeMode = it))
                themeDialogVisible = false
            },
            onDismiss = { themeDialogVisible = false }
        )
    }

    if (aspectRatioDialogVisible && aspectRatioSettingEnabled) {
        SettingsSelectionDialog(
            title = appStringResource(R.string.settings_aspect_ratio_title),
            options = AspectRatioMode.entries,
            selected = settings.aspectRatio,
            label = { appStringResource(it.displayNameResource) },
            onSelect = {
                onSettingsChanged(settings.copy(aspectRatio = it))
                aspectRatioDialogVisible = false
            },
            onDismiss = { aspectRatioDialogVisible = false }
        )
    }

    if (imageFilterDialogVisible) {
        SettingsSelectionDialog(
            title = appStringResource(R.string.settings_image_filter_title),
            options = ImageFilterMode.entries,
            selected = settings.imageFilter,
            label = { appStringResource(it.displayNameResource) },
            onSelect = {
                onSettingsChanged(settings.copy(imageFilter = it))
                imageFilterDialogVisible = false
            },
            onDismiss = { imageFilterDialogVisible = false }
        )
    }

    if (fastForwardDialogVisible) {
        SettingsSelectionDialog(
            title = appStringResource(R.string.settings_fast_forward_multiplier_title),
            options = listOf(2, 3, 4, 0),
            selected = settings.fastForwardMultiplier,
            label = { fastForwardMultiplierDisplayName(it) },
            onSelect = {
                onSettingsChanged(settings.copy(fastForwardMultiplier = it))
                fastForwardDialogVisible = false
            },
            onDismiss = { fastForwardDialogVisible = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) {
            SettingsTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Tab(
                    modifier = Modifier.focusProperties {
                        if (pauseMenuPageFocusRequester != null) {
                            up = pauseMenuPageFocusRequester
                            if (tab == SettingsTab.General) {
                                left = FocusRequester.Cancel
                            }
                            if (tab == SettingsTab.Advanced) {
                                right = FocusRequester.Cancel
                            }
                        }
                    },
                    selected = selected,
                    onClick = { selectedTab = tab },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = appStringResource(tab.titleResource),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                SettingsTab.General -> GeneralSettings(
                    settings = settings,
                    update = onSettingsChanged,
                    onOrientation = { orientationDialogVisible = true },
                    onLanguage = { languageDialogVisible = true },
                    onTheme = { themeDialogVisible = true }
                )

                SettingsTab.Display -> DisplaySettings(
                    settings = settings,
                    aspectRatioSettingEnabled = aspectRatioSettingEnabled,
                    onAspectRatio = { aspectRatioDialogVisible = true },
                    onImageFilter = { imageFilterDialogVisible = true }
                )

                SettingsTab.Audio -> AudioSettings(
                    settings = settings,
                    update = onSettingsChanged
                )

                SettingsTab.Advanced -> AdvancedSettings(
                    settings = settings,
                    embeddedInPauseMenu = embeddedInPauseMenu,
                    update = onSettingsChanged,
                    onFastForward = { fastForwardDialogVisible = true }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.GeneralSettings(
    settings: AppSettingsState,
    update: (AppSettingsState) -> Unit,
    onOrientation: () -> Unit,
    onLanguage: () -> Unit,
    onTheme: () -> Unit
) {
    SettingsSectionTitle(appStringResource(R.string.settings_section_interface))
    SettingRow(
        title = appStringResource(R.string.settings_screen_orientation_title),
        description = appStringResource(R.string.settings_screen_orientation_description),
        value = appStringResource(settings.orientation.displayNameResource),
        onClick = onOrientation
    )
    HorizontalDivider()
    SettingRow(
        title = appStringResource(R.string.settings_language_title),
        description = appStringResource(R.string.settings_language_description),
        value = appStringResource(settings.language.displayNameResource),
        onClick = onLanguage
    )
    HorizontalDivider()
    SettingRow(
        title = appStringResource(R.string.settings_theme_title),
        description = appStringResource(R.string.settings_theme_description),
        value = appStringResource(settings.themeMode.displayNameResource),
        onClick = onTheme
    )
    HorizontalDivider()
    SettingsSectionTitle(appStringResource(R.string.settings_section_on_screen_display))
    SwitchSettingRow(
        title = appStringResource(R.string.settings_show_information_title),
        description = appStringResource(R.string.settings_show_information_description),
        checked = settings.showInformation,
        onCheckedChange = { update(settings.copy(showInformation = it)) }
    )
    HorizontalDivider()
    SwitchSettingRow(
        title = appStringResource(R.string.settings_show_emulation_speed_title),
        description = appStringResource(R.string.settings_show_emulation_speed_description),
        checked = settings.showEmulationSpeed,
        enabled = settings.showInformation,
        onCheckedChange = { update(settings.copy(showEmulationSpeed = it)) }
    )
    HorizontalDivider()
    SwitchSettingRow(
        title = appStringResource(R.string.settings_show_fps_title),
        description = appStringResource(R.string.settings_show_fps_description),
        checked = settings.showFps,
        enabled = settings.showInformation,
        onCheckedChange = { update(settings.copy(showFps = it)) }
    )
}

@Composable
private fun ColumnScope.DisplaySettings(
    settings: AppSettingsState,
    aspectRatioSettingEnabled: Boolean,
    onAspectRatio: () -> Unit,
    onImageFilter: () -> Unit
) {
    SettingRow(
        title = appStringResource(R.string.settings_aspect_ratio_title),
        description = appStringResource(
            if (aspectRatioSettingEnabled) {
                R.string.settings_aspect_ratio_description
            } else {
                R.string.settings_aspect_ratio_unavailable
            }
        ),
        value = appStringResource(settings.aspectRatio.displayNameResource),
        onClick = onAspectRatio,
        enabled = aspectRatioSettingEnabled
    )
    HorizontalDivider()
    SettingRow(
        title = appStringResource(R.string.settings_image_filter_title),
        description = appStringResource(R.string.settings_image_filter_description),
        value = appStringResource(settings.imageFilter.displayNameResource),
        onClick = onImageFilter
    )
}

@Composable
private fun ColumnScope.AudioSettings(
    settings: AppSettingsState,
    update: (AppSettingsState) -> Unit
) {
    SwitchSettingRow(
        title = appStringResource(R.string.settings_game_muted_title),
        description = appStringResource(R.string.settings_game_muted_description),
        checked = settings.gameMuted,
        onCheckedChange = { update(settings.copy(gameMuted = it)) }
    )
    HorizontalDivider()
    PercentageSettingRow(
        title = appStringResource(R.string.settings_game_volume_title),
        description = appStringResource(R.string.settings_game_volume_description),
        percent = settings.volumePercent,
        minimumPercent = 0,
        maximumPercent = 100,
        enabled = !settings.gameMuted,
        onPercentChange = { update(settings.copy(volumePercent = it)) }
    )
    HorizontalDivider()
    SwitchSettingRow(
        title = appStringResource(R.string.settings_sound_softening_title),
        description = appStringResource(R.string.settings_sound_softening_description),
        checked = settings.soundSofteningEnabled,
        onCheckedChange = { update(settings.copy(soundSofteningEnabled = it)) }
    )
    HorizontalDivider()
    PercentageSettingRow(
        title = appStringResource(R.string.settings_softening_level_title),
        description = appStringResource(R.string.settings_softening_level_description),
        percent = settings.soundSofteningPercent,
        minimumPercent = 5,
        maximumPercent = 95,
        enabled = settings.soundSofteningEnabled,
        onPercentChange = { update(settings.copy(soundSofteningPercent = it)) }
    )
}

@Composable
private fun ColumnScope.AdvancedSettings(
    settings: AppSettingsState,
    embeddedInPauseMenu: Boolean,
    update: (AppSettingsState) -> Unit,
    onFastForward: () -> Unit
) {
    SwitchSettingRow(
        title = appStringResource(R.string.settings_auto_save_title),
        description = appStringResource(
            if (embeddedInPauseMenu) {
                R.string.settings_auto_save_description
            } else {
                R.string.settings_auto_save_game_only_description
            }
        ),
        checked = settings.autoSaveEnabled,
        enabled = embeddedInPauseMenu,
        onCheckedChange = { update(settings.copy(autoSaveEnabled = it)) }
    )
    HorizontalDivider()
    SettingRow(
        title = appStringResource(R.string.settings_fast_forward_multiplier_title),
        description = appStringResource(R.string.settings_fast_forward_multiplier_description),
        value = fastForwardMultiplierDisplayName(settings.fastForwardMultiplier),
        onClick = onFastForward
    )
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun PercentageSettingRow(
    title: String,
    description: String,
    percent: Int,
    minimumPercent: Int,
    maximumPercent: Int,
    enabled: Boolean = true,
    onPercentChange: (Int) -> Unit
) {
    val pauseMenuPageFocusRequester = LocalPauseMenuPageFocusRequester.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .symmetricCutoutPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "$percent%",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(
            modifier = Modifier.focusProperties {
                if (pauseMenuPageFocusRequester != null) {
                    left = FocusRequester.Cancel
                }
            },
            onClick = { onPercentChange((percent - 1).coerceAtLeast(minimumPercent)) },
            enabled = enabled && percent > minimumPercent
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = appStringResource(R.string.action_decrease_setting, title),
                tint = percentageButtonTint(
                    active = enabled && percent > minimumPercent,
                    rowEnabled = enabled
                )
            )
        }
        IconButton(
            modifier = Modifier.focusProperties {
                if (pauseMenuPageFocusRequester != null) {
                    right = FocusRequester.Cancel
                }
            },
            onClick = { onPercentChange((percent + 1).coerceAtMost(maximumPercent)) },
            enabled = enabled && percent < maximumPercent
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = appStringResource(R.string.action_increase_setting, title),
                tint = percentageButtonTint(
                    active = enabled && percent < maximumPercent,
                    rowEnabled = enabled
                )
            )
        }
    }
}

@Composable
private fun percentageButtonTint(
    active: Boolean,
    rowEnabled: Boolean
): androidx.compose.ui.graphics.Color = if (active) {
    MaterialTheme.colorScheme.primary
} else {
    MaterialTheme.colorScheme.onSurface.copy(alpha = if (rowEnabled) 0.38f else 1f)
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val pauseMenuPageFocusRequester = LocalPauseMenuPageFocusRequester.current
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .focusProperties {
                if (pauseMenuPageFocusRequester != null) {
                    left = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                }
            }
            .clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier
            .alpha(if (enabled) 1f else 0.38f)
            .symmetricCutoutPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (value != null) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (onClick != null) {
            Text(
                text = "›",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val pauseMenuPageFocusRequester = LocalPauseMenuPageFocusRequester.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .symmetricCutoutPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else 0.38f
                )
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.38f
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Switch(
            modifier = Modifier.focusProperties {
                if (pauseMenuPageFocusRequester != null) {
                    left = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                }
            },
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun <T> SettingsSelectionDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val rowHeight = if (
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) 48.dp else 52.dp

    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = 28.dp,
                    top = 28.dp,
                    end = 28.dp,
                    bottom = 16.dp
                )
            )
            Column(
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .clickable { onSelect(option) }
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == option, onClick = null)
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = appStringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun fastForwardMultiplierDisplayName(multiplier: Int): String = when (multiplier) {
    0 -> appStringResource(R.string.fast_forward_unlimited)
    3 -> "3×"
    4 -> "4×"
    else -> "2×"
}
