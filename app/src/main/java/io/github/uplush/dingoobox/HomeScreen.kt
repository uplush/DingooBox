package io.github.uplush.dingoobox

import android.content.res.Configuration
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
internal fun HomeScreen(
    games: List<GameEntry>,
    sortMode: GameSortMode,
    homePreferences: DingooPreferences,
    onSortMode: (GameSortMode) -> Unit,
    onAddGames: () -> Unit,
    onOpenGame: () -> Unit,
    onGameClick: (GameEntry) -> Unit,
    onLoadGameState: (GameEntry) -> Unit,
    onGameSummary: (GameEntry) -> Unit,
    onCoverSelected: (GameEntry) -> Unit,
    onSettings: () -> Unit,
    onControlsSettings: () -> Unit,
    onResetSettings: () -> Unit,
    onScanGames: () -> Unit,
    onManageSaveStates: () -> Unit,
    coverDownloadInProgress: Boolean,
    onDownloadGameCovers: () -> Unit,
    onCreateShortcut: (GameEntry) -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val inputModeManager = LocalInputModeManager.current
    val drawerFocus = remember { FocusRequester() }
    val homeMenuFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val searchButtonFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchVisible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var grid by rememberSaveable {
        mutableStateOf(
            homePreferences.getBoolean(
                "grid_view_enabled",
                false
            )
        )
    }
    var sortDialog by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }
    var actionGame by remember { mutableStateOf<GameEntry?>(null) }
    var restoreHomeFocusAfterDrawer by remember { mutableStateOf(false) }
    var restoreSearchButtonFocus by remember { mutableStateOf(false) }
    val drawerAcceptsFocus =
        drawerState.currentValue == DrawerValue.Open ||
            drawerState.targetValue == DrawerValue.Open
    val filtered = remember(games, query) {
        if (query.isBlank()) games else games.filter { it.title.contains(query, ignoreCase = true) }
    }

    val closeSearch: () -> Unit = {
        restoreSearchButtonFocus = true
        searchVisible = false
        query = ""
        keyboardController?.hide()
    }

    LaunchedEffect(drawerState.currentValue, drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open && !restoreHomeFocusAfterDrawer) {
            restoreHomeFocusAfterDrawer = true
            inputModeManager.requestInputMode(InputMode.Keyboard)
            drawerFocus.requestFocus()
        } else if (drawerState.currentValue == DrawerValue.Closed && restoreHomeFocusAfterDrawer) {
            homeMenuFocus.requestFocus()
            restoreHomeFocusAfterDrawer = false
        }
    }

    LaunchedEffect(searchVisible) {
        if (searchVisible) {
            restoreSearchButtonFocus = false
            delay(100)
            searchFocus.requestFocus()
            keyboardController?.show()
        } else if (restoreSearchButtonFocus) {
            searchButtonFocus.requestFocus()
            restoreSearchButtonFocus = false
        }
    }

    BackHandler(enabled = searchVisible && !drawerState.isOpen) {
        closeSearch()
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = drawerState.currentValue == DrawerValue.Open &&
            drawerState.targetValue == DrawerValue.Open,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.onPreviewKeyEvent { event ->
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                },
                windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
            ) {
                Column(
                    Modifier
                        .symmetricCutoutPadding(fraction = 0.5f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    DrawerItem(
                        appStringResource(R.string.home_drawer_all_games),
                        Icons.Default.Games,
                        true,
                        focusEnabled = drawerAcceptsFocus,
                        blockUp = true,
                        modifier = Modifier.focusRequester(drawerFocus)
                    ) {
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(
                        sortDrawerLabel(sortMode),
                        Icons.AutoMirrored.Filled.Sort,
                        false,
                        focusEnabled = drawerAcceptsFocus
                    ) {
                        sortDialog = true; scope.launch { drawerState.close() }
                    }
                    DrawerDivider()
                    DrawerItem(
                        appStringResource(R.string.home_drawer_open_game),
                        Icons.Default.FolderOpen,
                        false,
                        focusEnabled = drawerAcceptsFocus
                    ) {
                        onOpenGame(); scope.launch { drawerState.close() }
                    }
                    DrawerDivider()
                    DrawerItem(
                        appStringResource(R.string.home_drawer_app_settings),
                        Icons.Default.Settings,
                        false,
                        focusEnabled = drawerAcceptsFocus
                    ) {
                        scope.launch { drawerState.close() }; onSettings()
                    }
                    DrawerItem(
                        appStringResource(R.string.home_drawer_control_settings),
                        Icons.Default.SportsEsports,
                        false,
                        focusEnabled = drawerAcceptsFocus
                    ) {
                        scope.launch { drawerState.close() }; onControlsSettings()
                    }
                    DrawerDivider()
                    DrawerItem(
                        appStringResource(R.string.home_drawer_reset_settings),
                        Icons.Default.Restore,
                        false,
                        focusEnabled = drawerAcceptsFocus
                    ) {
                        scope.launch { drawerState.close() }; resetDialog = true
                    }
                    DrawerItem(
                        appStringResource(R.string.home_drawer_scan_games),
                        Icons.Default.Refresh,
                        false,
                        focusEnabled = drawerAcceptsFocus
                    ) {
                        onScanGames(); scope.launch { drawerState.close() }
                    }
                    DrawerItem(
                        appStringResource(R.string.home_drawer_save_state_manager),
                        Icons.Default.Save,
                        false,
                        focusEnabled = drawerAcceptsFocus
                    ) {
                        scope.launch { drawerState.close() }; onManageSaveStates()
                    }
                    DrawerItem(
                        appStringResource(
                            if (coverDownloadInProgress) {
                                R.string.home_drawer_downloading_game_covers
                            } else {
                                R.string.home_drawer_download_game_covers
                            }
                        ),
                        Icons.Default.Download,
                        false,
                        focusEnabled = drawerAcceptsFocus
                    ) {
                        if (!coverDownloadInProgress) {
                            onDownloadGameCovers()
                        }
                    }
                    DrawerDivider()
                    DrawerItem(
                        appStringResource(R.string.home_drawer_about),
                        Icons.Default.Info,
                        false,
                        focusEnabled = drawerAcceptsFocus,
                        blockDown = true
                    ) {
                        scope.launch { drawerState.close() }; onAbout()
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawerState.currentValue, drawerState.targetValue) {
                    if (drawerState.currentValue != drawerState.targetValue) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .symmetricCutoutPadding()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searchVisible) {
                    IconButton(onClick = closeSearch) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            appStringResource(R.string.home_action_exit_search)
                        )
                    }
                } else {
                    TextButton(
                        modifier = Modifier
                            .focusRequester(homeMenuFocus)
                            .onPreviewKeyEvent { event ->
                                event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                            },
                        onClick = { scope.launch { drawerState.open() } }
                    ) {
                        Text(
                            text = "☰",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 26.sp
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                if (searchVisible) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocus)
                            .onPreviewKeyEvent { event ->
                                val nativeEvent = event.nativeKeyEvent
                                val isCancelKey =
                                    nativeEvent.keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                                        nativeEvent.keyCode == KeyEvent.KEYCODE_BACK ||
                                        nativeEvent.keyCode == KeyEvent.KEYCODE_ESCAPE

                                if (isCancelKey) {
                                    if (
                                        nativeEvent.action == KeyEvent.ACTION_DOWN &&
                                        nativeEvent.repeatCount == 0
                                    ) {
                                        closeSearch()
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            .padding(horizontal = 8.dp),
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        appStringResource(R.string.home_search_hint),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                } else {
                    Text(
                        appStringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        modifier = Modifier.focusRequester(searchButtonFocus),
                        onClick = { searchVisible = true }
                    ) {
                        Icon(
                            Icons.Default.Search,
                            appStringResource(R.string.home_action_search_games)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        val newGridViewEnabled = !grid
                        grid = newGridViewEnabled
                        homePreferences
                            .edit()
                            .putBoolean(
                                "grid_view_enabled",
                                newGridViewEnabled
                            )
                            .apply()
                    }
                ) {
                    Icon(
                        imageVector = if (grid) {
                            Icons.AutoMirrored.Filled.List
                        } else {
                            Icons.Default.GridView
                        },
                        contentDescription = appStringResource(
                            if (grid) {
                                R.string.home_action_list_view
                            } else {
                                R.string.home_action_grid_view
                            }
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyLibrary(
                    searchQuery = query.trim(),
                    onAddGames = onAddGames,
                    modifier = Modifier.symmetricCutoutPadding()
                )
            } else if (grid) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .symmetricCutoutPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    LibraryHeader(filtered.size)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { game ->
                            GameGridItem(game, { onGameClick(game) }, { actionGame = game })
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .symmetricCutoutPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    item { LibraryHeader(filtered.size) }
                    items(filtered, key = { it.id }) { game ->
                        Column {
                            GameListItem(game, { onGameClick(game) }, { actionGame = game })
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (sortDialog) {
        GameSortDialog(
            currentMode = sortMode,
            onSelect = { mode ->
                onSortMode(mode)
                sortDialog = false
            },
            onDismiss = { sortDialog = false }
        )
    }

    if (resetDialog) {
        MiniQConfirmDialog(
            title = appStringResource(R.string.home_reset_settings_title),
            message = appStringResource(R.string.home_reset_settings_message),
            confirmLabel = appStringResource(R.string.home_reset_settings_yes),
            dismissLabel = appStringResource(R.string.home_reset_settings_no),
            onConfirm = {
                resetDialog = false
                onResetSettings()
            },
            onDismiss = { resetDialog = false }
        )
    }

    actionGame?.let { game ->
        GameActionDialog(
            game = game,
            onDismiss = { actionGame = null },
            onStart = { actionGame = null; onGameClick(game) },
            onLoadState = { actionGame = null; onLoadGameState(game) },
            onSummary = { actionGame = null; onGameSummary(game) },
            onCover = { actionGame = null; onCoverSelected(game) },
            onShortcut = { actionGame = null; onCreateShortcut(game) }
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    focusEnabled: Boolean,
    blockUp: Boolean = false,
    blockDown: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) }, icon = { Icon(icon, null) }, selected = selected,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .focusProperties {
                canFocus = focusEnabled
                if (blockUp) up = FocusRequester.Cancel
                if (blockDown) down = FocusRequester.Cancel
            }
    )
}

@Composable
private fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun sortDrawerLabel(mode: GameSortMode): String = appStringResource(
    when (mode) {
        GameSortMode.Title -> R.string.home_sort_by_title
        GameSortMode.LastPlayed -> R.string.home_sort_by_recently_played
        GameSortMode.PlayTime -> R.string.home_sort_by_play_time
        GameSortMode.Size -> R.string.home_sort_by_game_size
    }
)

@Composable
private fun sortOptionLabel(mode: GameSortMode): String = appStringResource(
    when (mode) {
        GameSortMode.Title -> R.string.home_sort_title
        GameSortMode.LastPlayed -> R.string.home_sort_recently_played
        GameSortMode.PlayTime -> R.string.home_sort_play_time
        GameSortMode.Size -> R.string.home_sort_game_size
    }
)

@Composable
private fun Modifier.handleGameItemControllerClick(
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val currentOnClick = rememberUpdatedState(onClick)
    val currentOnLongClick = rememberUpdatedState(onLongClick)
    val pressedKeyCode = remember { mutableStateOf<Int?>(null) }
    val longPressTriggered = remember { mutableStateOf(false) }
    val longPressJob = remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            longPressJob.value?.cancel()
        }
    }

    return onPreviewKeyEvent { composeEvent ->
        val event = composeEvent.nativeKeyEvent
        val isGamepadSource =
            (event.source and InputDevice.SOURCE_GAMEPAD) ==
                InputDevice.SOURCE_GAMEPAD

        // Odin reports its physical Y button first as BUTTON_Y and then as a
        // fallback SPACE event with the same scan code. Match MiNiQ and consume
        // both before combinedClickable can interpret either as activation.
        val isBlockedOdinYEvent =
            isGamepadSource &&
                event.scanCode == 308 &&
                (
                    event.keyCode == KeyEvent.KEYCODE_BUTTON_Y ||
                        event.keyCode == KeyEvent.KEYCODE_SPACE
                )

        if (isBlockedOdinYEvent) {
            true
        } else {
            val isConfirmKey = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_BUTTON_A -> true
                else -> false
            }

            if (!isConfirmKey) {
                false
            } else {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        // Only the first Down starts the timer. Hardware repeat
                        // events are consumed without restarting the long press.
                        if (
                            event.repeatCount == 0 ||
                            pressedKeyCode.value != event.keyCode
                        ) {
                            longPressJob.value?.cancel()

                            val keyCode = event.keyCode
                            val elapsedMilliseconds =
                                (SystemClock.uptimeMillis() - event.downTime)
                                    .coerceAtLeast(0L)
                            val remainingMilliseconds =
                                (
                                    ViewConfiguration.getLongPressTimeout().toLong() -
                                        elapsedMilliseconds
                                ).coerceAtLeast(0L)

                            pressedKeyCode.value = keyCode
                            longPressTriggered.value = false
                            longPressJob.value = coroutineScope.launch {
                                delay(remainingMilliseconds)
                                if (
                                    pressedKeyCode.value == keyCode &&
                                    !longPressTriggered.value
                                ) {
                                    longPressTriggered.value = true
                                    currentOnLongClick.value.invoke()
                                }
                            }
                        }
                        true
                    }

                    KeyEvent.ACTION_UP -> {
                        if (pressedKeyCode.value == event.keyCode) {
                            longPressJob.value?.cancel()
                            longPressJob.value = null

                            val performShortClick =
                                !longPressTriggered.value && !event.isCanceled

                            pressedKeyCode.value = null
                            longPressTriggered.value = false

                            if (performShortClick) {
                                currentOnClick.value.invoke()
                            }
                        }
                        true
                    }

                    else -> true
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameGridItem(game: GameEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .handleGameItemControllerClick(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            GameCover(game.coverFile, Modifier.fillMaxWidth().height(110.dp), 8.dp)
            Column(Modifier.padding(12.dp)) {
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Dingoo A320 · ${formatFileSize(game.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameListItem(game: GameEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
            .handleGameItemControllerClick(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GameCover(game.coverFile, Modifier.size(52.dp), 10.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Dingoo A320 · ${formatFileSize(game.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader(gameCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            appStringResource(R.string.home_game_library),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Text(
            appStringResource(
                if (gameCount == 1) {
                    R.string.home_game_count_single
                } else {
                    R.string.home_game_count_multiple
                },
                gameCount
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLibrary(
    searchQuery: String,
    onAddGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searching = searchQuery.isNotBlank()
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    appStringResource(
                        if (searching) {
                            R.string.home_no_games_found
                        } else {
                            R.string.home_empty_library_title
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (searching) {
                        appStringResource(R.string.home_no_games_match, searchQuery)
                    } else {
                        appStringResource(R.string.home_empty_library_description)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!searching) {
                    TextButton(onClick = onAddGames) {
                        Text(appStringResource(R.string.home_add_game_directory))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameSortDialog(
    currentMode: GameSortMode,
    onSelect: (GameSortMode) -> Unit,
    onDismiss: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column {
            Text(
                appStringResource(R.string.home_sort_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 16.dp)
            )
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(scrollState).padding(bottom = 16.dp)
            ) {
                GameSortMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .height(if (isLandscape) 48.dp else 52.dp)
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == currentMode, onClick = null)
                        Text(
                            sortOptionLabel(mode),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameActionDialog(
    game: GameEntry,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onLoadState: () -> Unit,
    onSummary: () -> Unit,
    onCover: () -> Unit,
    onShortcut: () -> Unit
) {
    // Direct layout port of MiNiQ HomeScreen's long-press game dialog.
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val dialogWidth = (
        configuration.screenWidthDp * if (isLandscape) 0.78f else 0.84f
    ).dp
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                }
                .symmetricSafeDrawingPadding()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(dialogWidth)
                    .heightIn(max = maxHeight)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {})
                    },
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            if (game.coverFile?.exists() == true) {
                                GameCover(
                                    file = game.coverFile,
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 0.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "DA",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                        ) {
                            Text(
                                text = game.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Dingoo A320",
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(weight = 1f, fill = false)
                            .verticalScroll(scrollState)
                    ) {
                        GameActionButton(
                            appStringResource(R.string.home_action_start_game),
                            Icons.Rounded.PlayArrow,
                            isLandscape,
                            onStart
                        )
                        GameActionButton(
                            appStringResource(R.string.home_action_load_save_state),
                            Icons.Rounded.Refresh,
                            isLandscape,
                            onLoadState
                        )
                        GameActionButton(
                            appStringResource(R.string.home_action_game_summary),
                            Icons.Rounded.Info,
                            isLandscape,
                            onSummary
                        )
                        GameActionButton(
                            appStringResource(R.string.home_action_choose_cover_image),
                            Icons.Rounded.Image,
                            isLandscape,
                            onCover
                        )
                        GameActionButton(
                            appStringResource(R.string.home_action_create_shortcut),
                            Icons.AutoMirrored.Rounded.AddToHomeScreen,
                            isLandscape,
                            onShortcut
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameActionButton(
    label: String,
    icon: ImageVector,
    isLandscape: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        modifier = Modifier.fillMaxWidth().height(if (isLandscape) 46.dp else 52.dp),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
