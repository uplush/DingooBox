package io.github.uplush.dingoobox

import android.content.res.Configuration
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private enum class ControllerSettingsTab(val titleResource: Int) {
    Settings(R.string.controller_tab_settings),
    Touchscreen(R.string.controller_tab_touch_controls),
    Mapping(R.string.controller_tab_mapping),
    Hotkeys(R.string.controller_tab_hotkeys)
}

internal val LocalControllerSettingsHorizontalPadding = staticCompositionLocalOf { 20.dp }

@Composable
fun ControllerSettingsScreen(
    settings: AppSettingsState,
    bindings: InputBindingPreferences,
    inGame: Boolean = false,
    embedded: Boolean = false,
    openTouchControlsInitially: Boolean = false,
    gameKey: String? = null,
    gameSpecificEnabled: Boolean = false,
    onGameSpecificEnabled: (Boolean) -> Unit = {},
    onSettingsChanged: (AppSettingsState) -> Unit,
    onEditLayout: (Boolean) -> Unit,
    onBack: () -> Unit = {}
) {
    if (!embedded) BackHandler(onBack = onBack)

    val content: @Composable (Modifier) -> Unit = { modifier ->
        ControllerSettingsContent(
            settings = settings,
            bindings = bindings,
            embeddedInPauseMenu = embedded && inGame,
            openTouchControlsInitially = openTouchControlsInitially,
            gameKey = gameKey,
            gameSpecificEnabled = gameSpecificEnabled,
            onGameSpecificEnabled = onGameSpecificEnabled,
            onSettingsChanged = onSettingsChanged,
            onEditLayout = onEditLayout,
            modifier = modifier
        )
    }

    if (embedded) {
        content(Modifier)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            MiniQPageHeader(
                title = appStringResource(R.string.controller_settings_title),
                onBack = onBack
            )
            content(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerSettingsContent(
    settings: AppSettingsState,
    bindings: InputBindingPreferences,
    embeddedInPauseMenu: Boolean,
    openTouchControlsInitially: Boolean,
    gameKey: String?,
    gameSpecificEnabled: Boolean,
    onGameSpecificEnabled: (Boolean) -> Unit,
    onSettingsChanged: (AppSettingsState) -> Unit,
    onEditLayout: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pauseMenuPageFocusRequester = LocalPauseMenuPageFocusRequester.current
    val context = LocalContext.current
    val localizedResources = appResources()
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val scopePreferences = remember(context) { ControlSettingsScopePreferences(context) }
    val profilePreferences = remember(context) { ControlProfilePreferences(context) }
    val activeGameKey = gameKey.takeIf { gameSpecificEnabled }
    val activeLayoutPreferences = remember(context, portrait, activeGameKey) {
        ControlLayoutPreferences(context, portrait, activeGameKey)
    }
    val activeBehaviorPreferences = remember(context, activeGameKey) {
        ControlBehaviorPreferences(context, activeGameKey)
    }

    var selectedTab by remember(openTouchControlsInitially) {
        mutableStateOf(
            if (openTouchControlsInitially) {
                ControllerSettingsTab.Touchscreen
            } else {
                ControllerSettingsTab.Settings
            }
        )
    }
    var localRevision by remember { mutableIntStateOf(0) }
    var pendingBindingAction by remember { mutableStateOf<DingooInputAction?>(null) }
    var pendingHotkeyAction by remember { mutableStateOf<DingooHotkeyAction?>(null) }
    var showVisibleControlsDialog by remember { mutableStateOf(false) }
    var pendingVisibleControls by remember { mutableStateOf(ControlId.entries.toSet()) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showLoadConfirmation by remember { mutableStateOf(false) }
    var showLoadProfileDialog by remember { mutableStateOf(false) }
    var showSaveProfileDialog by remember { mutableStateOf(false) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var profileNames by remember { mutableStateOf(profilePreferences.profileNames()) }
    var newProfileName by remember { mutableStateOf("") }

    fun notifyControlDataChanged() {
        localRevision++
        onGameSpecificEnabled(gameSpecificEnabled)
    }

    fun saveProfile(name: String): Boolean {
        val overwriting = profileNames.any { it.equals(name.trim(), ignoreCase = true) }
        val savedName = profilePreferences.saveProfile(name, activeGameKey, settings)
        if (savedName == null) {
            showMiniQToast(
                context,
                localizedResources.getString(R.string.controller_profile_save_failed)
            )
            return false
        }
        profileNames = profilePreferences.profileNames()
        showMiniQToast(
            context,
            localizedResources.getString(
                if (overwriting) {
                    R.string.controller_profile_overwritten
                } else {
                    R.string.controller_profile_created
                },
                savedName
            )
        )
        return true
    }

    if (showVisibleControlsDialog) {
        VirtualControlSelectionDialog(
            visibleControls = pendingVisibleControls,
            onDismiss = { showVisibleControlsDialog = false },
            onConfirm = { visibleControls ->
                val layout = activeLayoutPreferences.load()
                activeLayoutPreferences.save(
                    layout.copy(hidden = ControlId.entries.filterNotTo(mutableSetOf()) {
                        it in visibleControls
                    })
                )
                pendingVisibleControls = visibleControls
                showVisibleControlsDialog = false
                notifyControlDataChanged()
            }
        )
    }

    if (showResetConfirmation) {
        ControllerConfirmationDialog(
            title = appStringResource(
                if (gameSpecificEnabled) {
                    R.string.controller_profile_copy_global
                } else {
                    R.string.controller_profile_restore_defaults
                }
            ),
            message = appStringResource(
                if (gameSpecificEnabled) {
                    R.string.controller_profile_copy_confirmation
                } else {
                    R.string.controller_profile_restore_confirmation
                }
            ),
            confirmLabel = appStringResource(
                if (gameSpecificEnabled) {
                    R.string.controller_profile_copy
                } else {
                    R.string.controller_profile_restore
                }
            ),
            onDismiss = { showResetConfirmation = false },
            onConfirm = {
                if (gameSpecificEnabled && !gameKey.isNullOrBlank()) {
                    scopePreferences.copyGlobalToGame(gameKey)
                    showMiniQToast(
                        context,
                        localizedResources.getString(R.string.controller_profile_copy_success)
                    )
                } else {
                    bindings.resetToDefaults()
                    ControlBehaviorPreferences(context).clear()
                    ControlLayoutPreferences(context, portrait = false).reset()
                    ControlLayoutPreferences(context, portrait = true).reset()
                    onSettingsChanged(
                        settings.copy(
                            virtualControlsVisible = true,
                            vibrationEnabled = true
                        )
                    )
                }
                showResetConfirmation = false
                notifyControlDataChanged()
            }
        )
    }

    if (showLoadConfirmation) {
        ControllerConfirmationDialog(
            title = appStringResource(R.string.controller_profile_load),
            message = appStringResource(R.string.controller_profile_load_confirmation),
            confirmLabel = appStringResource(R.string.controller_profile_continue),
            onDismiss = { showLoadConfirmation = false },
            onConfirm = {
                showLoadConfirmation = false
                profileNames = profilePreferences.profileNames()
                showLoadProfileDialog = profileNames.isNotEmpty()
            }
        )
    }

    if (showLoadProfileDialog) {
        ProfileListDialog(
            title = appStringResource(R.string.controller_profile_load),
            profileNames = profileNames,
            onProfileClick = { profileName ->
                val loaded = profilePreferences.loadProfile(profileName, activeGameKey)
                if (loaded != null) {
                    if (activeGameKey == null) {
                        onSettingsChanged(
                            settings.copy(
                                virtualControlsVisible = loaded.virtualControlsVisible,
                                vibrationEnabled = loaded.vibrationEnabled
                            )
                        )
                    }
                    showLoadProfileDialog = false
                    notifyControlDataChanged()
                    showMiniQToast(
                        context,
                        localizedResources.getString(
                            R.string.controller_profile_loaded,
                            profileName
                        )
                    )
                } else {
                    showMiniQToast(
                        context,
                        localizedResources.getString(R.string.controller_profile_load_failed)
                    )
                }
            },
            onDismiss = { showLoadProfileDialog = false }
        )
    }

    if (showSaveProfileDialog) {
        SaveProfileDialog(
            profileNames = profileNames,
            onProfileClick = { profileName ->
                if (saveProfile(profileName)) showSaveProfileDialog = false
            },
            onCreate = {
                showSaveProfileDialog = false
                newProfileName = ""
                showCreateProfileDialog = true
            },
            onDismiss = { showSaveProfileDialog = false }
        )
    }

    if (showCreateProfileDialog) {
        CreateProfileDialog(
            name = newProfileName,
            onNameChange = { newProfileName = it.take(40) },
            onDismiss = { showCreateProfileDialog = false },
            onCreate = {
                if (saveProfile(newProfileName)) {
                    showCreateProfileDialog = false
                    newProfileName = ""
                }
            }
        )
    }

    pendingBindingAction?.let { action ->
        InputBindingDialog(
            title = appStringResource(action.displayNameResource),
            currentBinding = bindingDisplayName(bindings.getBinding(action)),
            onBindingCaptured = {
                bindings.setBinding(action, it)
                pendingBindingAction = null
                localRevision++
            },
            onClear = {
                bindings.setBinding(action, null)
                pendingBindingAction = null
                localRevision++
            },
            onCancel = { pendingBindingAction = null }
        )
    }

    pendingHotkeyAction?.let { action ->
        InputBindingDialog(
            title = appStringResource(action.displayNameResource),
            currentBinding = bindingDisplayName(bindings.getHotkeyBinding(action)),
            onBindingCaptured = {
                bindings.setHotkeyBinding(action, it)
                pendingHotkeyAction = null
                localRevision++
            },
            onClear = {
                bindings.setHotkeyBinding(action, null)
                pendingHotkeyAction = null
                localRevision++
            },
            onCancel = { pendingHotkeyAction = null }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) {
            ControllerSettingsTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Tab(
                    modifier = Modifier.focusProperties {
                        if (pauseMenuPageFocusRequester != null) {
                            up = pauseMenuPageFocusRequester
                            if (tab == ControllerSettingsTab.Settings) {
                                left = FocusRequester.Cancel
                            }
                            if (tab == ControllerSettingsTab.Hotkeys) {
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
            // Match MiNiQ: keep the page subtree stable and use the revision
            // only when reading each binding. Wrapping this content in
            // key(localRevision) destroys the focused row when a dialog closes.
            val activeBehavior = activeBehaviorPreferences.load(settings)
            when (selectedTab) {
                ControllerSettingsTab.Settings -> ControllerProfileSettings(
                    gameKey = gameKey,
                    gameSpecificEnabled = gameSpecificEnabled,
                    onGameSpecificChange = { enabled ->
                        val activeKey = gameKey
                        if (!activeKey.isNullOrBlank() &&
                            scopePreferences.setGameIndependent(activeKey, enabled)
                        ) {
                            onGameSpecificEnabled(enabled)
                            localRevision++
                        }
                    },
                    onReset = { showResetConfirmation = true },
                    onSaveProfile = {
                        profileNames = profilePreferences.profileNames()
                        showSaveProfileDialog = true
                    },
                    onLoadProfile = {
                        profileNames = profilePreferences.profileNames()
                        if (profileNames.isEmpty()) {
                            showMiniQToast(
                                context,
                                localizedResources.getString(
                                    R.string.controller_profile_none_saved
                                )
                            )
                        } else {
                            showLoadConfirmation = true
                        }
                    }
                )

                ControllerSettingsTab.Touchscreen -> ControllerTouchSettings(
                    virtualControlsVisible = activeBehavior.virtualControlsVisible,
                    embeddedInPauseMenu = embeddedInPauseMenu,
                    onVirtualControlsVisibleChange = { visible ->
                        activeBehaviorPreferences.save(
                            activeBehavior.copy(virtualControlsVisible = visible)
                        )
                        if (activeGameKey == null) {
                            onSettingsChanged(
                                settings.copy(virtualControlsVisible = visible)
                            )
                        }
                        notifyControlDataChanged()
                    },
                    onSelectControls = {
                        val layout = activeLayoutPreferences.load()
                        pendingVisibleControls = ControlId.entries
                            .filterNotTo(mutableSetOf()) { it in layout.hidden }
                        showVisibleControlsDialog = true
                    },
                    onEditLayout = { onEditLayout(portrait) }
                )

                ControllerSettingsTab.Mapping -> ControllerMappingSettings(
                    bindings = bindings,
                    bindingRevision = localRevision,
                    onBindingClick = { pendingBindingAction = it },
                    onBindingsChanged = { localRevision++ }
                )

                ControllerSettingsTab.Hotkeys -> ControllerHotkeySettings(
                    bindings = bindings,
                    bindingRevision = localRevision,
                    onBindingClick = { pendingHotkeyAction = it }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColumnScope.ControllerProfileSettings(
    gameKey: String?,
    gameSpecificEnabled: Boolean,
    onGameSpecificChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onSaveProfile: () -> Unit,
    onLoadProfile: () -> Unit
) {
    val gameSpecificAvailable = !gameKey.isNullOrBlank()
    ControllerSectionTitle(appStringResource(R.string.controller_profile_section))
    ControllerSwitchRow(
        title = appStringResource(R.string.controller_profile_game_specific),
        description = appStringResource(
            if (gameSpecificAvailable) {
                R.string.controller_profile_game_specific_description
            } else {
                R.string.controller_profile_game_specific_unavailable
            }
        ),
        checked = gameSpecificEnabled,
        enabled = gameSpecificAvailable,
        onCheckedChange = onGameSpecificChange
    )
    HorizontalDivider()
    ControllerSettingRow(
        title = appStringResource(
            if (gameSpecificEnabled) {
                R.string.controller_profile_copy_global
            } else {
                R.string.controller_profile_restore_defaults
            }
        ),
        description = appStringResource(
            if (gameSpecificEnabled) {
                R.string.controller_profile_copy_global_description
            } else {
                R.string.controller_profile_restore_defaults_description
            }
        ),
        onClick = onReset
    )
    HorizontalDivider()
    ControllerSettingRow(
        title = appStringResource(R.string.controller_profile_save),
        description = appStringResource(R.string.controller_profile_save_description),
        onClick = onSaveProfile
    )
    HorizontalDivider()
    ControllerSettingRow(
        title = appStringResource(R.string.controller_profile_load),
        description = appStringResource(R.string.controller_profile_load_description),
        onClick = onLoadProfile
    )
}

@Composable
private fun ColumnScope.ControllerTouchSettings(
    virtualControlsVisible: Boolean,
    embeddedInPauseMenu: Boolean,
    onVirtualControlsVisibleChange: (Boolean) -> Unit,
    onSelectControls: () -> Unit,
    onEditLayout: () -> Unit
) {
    ControllerSectionTitle(appStringResource(R.string.controller_touch_section))
    ControllerSwitchRow(
        title = appStringResource(R.string.controller_touch_show_controls),
        description = appStringResource(R.string.controller_touch_show_controls_description),
        checked = virtualControlsVisible,
        onCheckedChange = onVirtualControlsVisibleChange
    )
    HorizontalDivider()
    ControllerSettingRow(
        title = appStringResource(R.string.controller_touch_select_controls),
        description = appStringResource(
            if (embeddedInPauseMenu) {
                R.string.controller_touch_select_controls_description
            } else {
                R.string.controller_touch_game_required
            }
        ),
        onClick = onSelectControls,
        enabled = embeddedInPauseMenu
    )
    HorizontalDivider()
    ControllerSettingRow(
        title = appStringResource(R.string.controller_touch_edit_layout),
        description = appStringResource(
            if (embeddedInPauseMenu) {
                R.string.controller_touch_edit_layout_description
            } else {
                R.string.controller_touch_game_required
            }
        ),
        onClick = onEditLayout,
        enabled = embeddedInPauseMenu
    )
}

@Composable
private fun ColumnScope.ControllerMappingSettings(
    bindings: InputBindingPreferences,
    bindingRevision: Int,
    onBindingClick: (DingooInputAction) -> Unit,
    onBindingsChanged: () -> Unit
) {
    ControllerSectionTitle(appStringResource(R.string.controller_mapping_section))
    ControllerSettingRow(
        title = appStringResource(R.string.controller_mapping_restore_defaults),
        description = appStringResource(R.string.controller_mapping_restore_defaults_description),
        onClick = {
            bindings.resetToDefaults()
            onBindingsChanged()
        }
    )
    HorizontalDivider()
    ControllerSettingRow(
        title = appStringResource(R.string.controller_mapping_clear_all),
        description = appStringResource(R.string.controller_mapping_clear_all_description),
        onClick = {
            bindings.clearAllBindings()
            onBindingsChanged()
        }
    )
    ControllerSectionTitle(appStringResource(R.string.controller_mapping_buttons_section))
    DingooInputAction.entries.forEach { action ->
        val binding = remember(bindingRevision, action) {
            bindings.getBinding(action)
        }
        ControllerSettingRow(
            title = appStringResource(action.displayNameResource),
            description = appStringResource(
                R.string.controller_current_binding,
                bindingDisplayName(binding)
            ),
            onClick = { onBindingClick(action) }
        )
        HorizontalDivider()
    }
}

@Composable
private fun ColumnScope.ControllerHotkeySettings(
    bindings: InputBindingPreferences,
    bindingRevision: Int,
    onBindingClick: (DingooHotkeyAction) -> Unit
) {
    ControllerSectionTitle(appStringResource(R.string.controller_hotkeys_system_section))
    DingooHotkeyAction.entries.forEach { action ->
        val binding = remember(bindingRevision, action) {
            bindings.getHotkeyBinding(action)
        }
        ControllerSettingRow(
            title = appStringResource(action.displayNameResource),
            description = appStringResource(
                R.string.controller_current_binding,
                bindingDisplayName(binding)
            ),
            onClick = { onBindingClick(action) }
        )
        HorizontalDivider()
    }
}

@Composable
private fun VirtualControlSelectionDialog(
    visibleControls: Set<ControlId>,
    onDismiss: () -> Unit,
    onConfirm: (Set<ControlId>) -> Unit
) {
    var pending by remember(visibleControls) { mutableStateOf(visibleControls) }
    val optionHeight = if (
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) 48.dp else 52.dp
    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            DialogTitle(appStringResource(R.string.virtual_control_add_remove))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                ControlId.entries.forEach { id ->
                    val checked = id in pending
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pending = if (checked) pending - id else pending + id
                            }
                            .height(optionHeight)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { selected ->
                                pending = if (selected) pending + id else pending - id
                            }
                        )
                        Text(
                            text = controlDisplayName(id),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            DialogButtons(
                onDismiss = onDismiss,
                confirmLabel = appStringResource(R.string.virtual_control_confirm),
                onConfirm = { onConfirm(pending) }
            )
        }
    }
}

@Composable
private fun ControllerConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(18.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(appStringResource(R.string.controller_profile_cancel))
                }
                TextButton(onClick = onConfirm) { Text(confirmLabel) }
            }
        }
    }
}

@Composable
private fun ProfileListDialog(
    title: String,
    profileNames: List<String>,
    onProfileClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val rowHeight = if (
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) 48.dp else 52.dp
    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column {
            DialogTitle(title)
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                profileNames.forEach { profileName ->
                    Text(
                        text = profileName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .clickable { onProfileClick(profileName) }
                            .padding(horizontal = 28.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                    )
                }
            }
            DialogButtons(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun SaveProfileDialog(
    profileNames: List<String>,
    onProfileClick: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val rowHeight = if (
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) 48.dp else 52.dp
    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column {
            DialogTitle(appStringResource(R.string.controller_profile_save))
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                (profileNames + appStringResource(R.string.controller_profile_create_option))
                    .forEachIndexed { index, label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .clickable {
                                    if (index < profileNames.size) {
                                        onProfileClick(profileNames[index])
                                    } else {
                                        onCreate()
                                    }
                                }
                                .padding(horizontal = 28.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }
            }
            DialogButtons(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun CreateProfileDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column {
            DialogTitle(appStringResource(R.string.controller_profile_enter_name))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                label = { Text(appStringResource(R.string.controller_profile_name)) },
                singleLine = true
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(appStringResource(R.string.controller_profile_cancel))
                }
                TextButton(enabled = name.trim().isNotEmpty(), onClick = onCreate) {
                    Text(appStringResource(R.string.controller_profile_create))
                }
            }
        }
    }
}

@Composable
private fun DialogTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 16.dp)
    )
}

@Composable
private fun DialogButtons(
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onDismiss) {
            Text(appStringResource(R.string.controller_profile_cancel))
        }
        if (confirmLabel != null && onConfirm != null) {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        }
    }
}

@Composable
private fun InputBindingDialog(
    title: String,
    currentBinding: String,
    onBindingCaptured: (PhysicalInputBinding) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    MiniQStandardDialog(onDismissRequest = onCancel) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            var pendingAxisBinding: PhysicalInputBinding? = null
            val listener = View.OnGenericMotionListener { _, event ->
                if (event.action != MotionEvent.ACTION_MOVE ||
                    (!event.isFromSource(InputDevice.SOURCE_JOYSTICK) &&
                        !event.isFromSource(InputDevice.SOURCE_GAMEPAD))
                ) {
                    false
                } else {
                    val pending = pendingAxisBinding
                    if (pending == null) {
                        val detected = detectControllerAxis(event)
                        if (detected == null) {
                            false
                        } else {
                            pendingAxisBinding = detected
                            true
                        }
                    } else {
                        if (abs(event.getAxisValue(pending.code)) <= 0.30f) {
                            onBindingCaptured(pending)
                        }
                        true
                    }
                }
            }
            dialogView.setOnGenericMotionListener(listener)
            onDispose { dialogView.setOnGenericMotionListener(null) }
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        Column(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { composeEvent ->
                    val event = composeEvent.nativeKeyEvent
                    val keyCode = event.keyCode
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                        keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                    ) return@onPreviewKeyEvent false
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) onCancel()
                        return@onPreviewKeyEvent true
                    }
                    val isDirectionKey = keyCode in setOf(
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT
                    )
                    val hasHatAxis = event.device?.let { device ->
                        device.getMotionRange(MotionEvent.AXIS_HAT_X) != null ||
                            device.getMotionRange(MotionEvent.AXIS_HAT_Y) != null
                    } == true
                    if (isDirectionKey && hasHatAxis) return@onPreviewKeyEvent true
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            if (event.repeatCount == 0 && keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                                onBindingCaptured(
                                    PhysicalInputBinding(PhysicalInputBindingType.KEY, keyCode)
                                )
                            }
                            true
                        }
                        KeyEvent.ACTION_UP -> true
                        else -> false
                    }
                }
                .focusable()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            Text(appStringResource(R.string.input_binding_instruction), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            Text(
                appStringResource(R.string.input_binding_current),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(currentBinding, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClear) {
                    Text(appStringResource(R.string.input_binding_clear))
                }
                TextButton(onClick = onCancel) {
                    Text(appStringResource(R.string.input_binding_cancel))
                }
            }
        }
    }
}

private fun detectControllerAxis(event: MotionEvent): PhysicalInputBinding? {
    val axes = intArrayOf(
        MotionEvent.AXIS_X,
        MotionEvent.AXIS_Y,
        MotionEvent.AXIS_Z,
        MotionEvent.AXIS_RX,
        MotionEvent.AXIS_RY,
        MotionEvent.AXIS_RZ,
        MotionEvent.AXIS_HAT_X,
        MotionEvent.AXIS_HAT_Y
    )
    var selectedAxis = -1
    var selectedValue = 0f
    axes.forEach { axis ->
        val value = event.getAxisValue(axis)
        if (abs(value) > abs(selectedValue)) {
            selectedAxis = axis
            selectedValue = value
        }
    }
    if (selectedAxis < 0 || abs(selectedValue) < 0.65f) return null
    return PhysicalInputBinding(
        type = PhysicalInputBindingType.AXIS,
        code = selectedAxis,
        direction = if (selectedValue < 0f) -1 else 1
    )
}

@Composable
private fun ControllerSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            horizontal = LocalControllerSettingsHorizontalPadding.current,
            vertical = 16.dp
        ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ControllerSettingRow(
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
            .padding(
                horizontal = LocalControllerSettingsHorizontalPadding.current,
                vertical = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(3.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp).widthIn(max = 160.dp)
            )
        }
    }
}

@Composable
private fun ControllerSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val pauseMenuPageFocusRequester = LocalPauseMenuPageFocusRequester.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .padding(
                horizontal = LocalControllerSettingsHorizontalPadding.current,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(3.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun bindingDisplayName(binding: PhysicalInputBinding?): String {
    if (binding == null) return appStringResource(R.string.controller_binding_unbound)
    return when (binding.type) {
        PhysicalInputBindingType.KEY -> when (binding.code) {
            KeyEvent.KEYCODE_DPAD_UP -> appStringResource(R.string.controller_binding_dpad_up)
            KeyEvent.KEYCODE_DPAD_DOWN -> appStringResource(R.string.controller_binding_dpad_down)
            KeyEvent.KEYCODE_DPAD_LEFT -> appStringResource(R.string.controller_binding_dpad_left)
            KeyEvent.KEYCODE_DPAD_RIGHT -> appStringResource(R.string.controller_binding_dpad_right)
            else -> keyDisplayName(binding.code)
        }
        PhysicalInputBindingType.AXIS -> axisDisplayName(binding.code, binding.direction)
    }
}

@Composable
private fun axisDisplayName(axis: Int, direction: Int): String {
    val negative = direction < 0
    return when (axis) {
        MotionEvent.AXIS_X -> appStringResource(
            if (negative) R.string.controller_binding_left_stick_left
            else R.string.controller_binding_left_stick_right
        )
        MotionEvent.AXIS_Y -> appStringResource(
            if (negative) R.string.controller_binding_left_stick_up
            else R.string.controller_binding_left_stick_down
        )
        MotionEvent.AXIS_Z, MotionEvent.AXIS_RX -> appStringResource(
            if (negative) R.string.controller_binding_right_stick_left
            else R.string.controller_binding_right_stick_right
        )
        MotionEvent.AXIS_RZ, MotionEvent.AXIS_RY -> appStringResource(
            if (negative) R.string.controller_binding_right_stick_up
            else R.string.controller_binding_right_stick_down
        )
        MotionEvent.AXIS_HAT_X -> appStringResource(
            if (negative) R.string.controller_binding_dpad_left
            else R.string.controller_binding_dpad_right
        )
        MotionEvent.AXIS_HAT_Y -> appStringResource(
            if (negative) R.string.controller_binding_dpad_up
            else R.string.controller_binding_dpad_down
        )
        else -> appStringResource(
            R.string.controller_binding_axis,
            axis,
            if (negative) "-" else "+"
        )
    }
}

@Composable
private fun keyDisplayName(keyCode: Int): String {
    val displayName = KeyEvent.keyCodeToString(keyCode)
        .removePrefix("KEYCODE_")
        .removePrefix("BUTTON_")
        .replace('_', ' ')
    return appStringResource(R.string.controller_binding_key, displayName)
}

@Composable
private fun controlDisplayName(id: ControlId): String = appStringResource(
    when (id) {
        ControlId.DPad -> R.string.virtual_control_d_pad
        ControlId.A -> R.string.controller_action_a
        ControlId.B -> R.string.controller_action_b
        ControlId.X -> R.string.controller_action_x
        ControlId.Y -> R.string.controller_action_y
        ControlId.L -> R.string.controller_action_l
        ControlId.R -> R.string.controller_action_r
        ControlId.Start -> R.string.controller_action_start
        ControlId.Select -> R.string.controller_action_select
    }
)
