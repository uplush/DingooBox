package io.github.uplush.dingoobox

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppPage { Home, Settings, Controls, Summary, SaveStates, LayoutEditor }

@Composable
fun DingooApp(
    shortcutLaunchRequest: ShortcutLaunchRequest? = null,
    onShortcutLaunchConsumed: () -> Unit = {},
    onThemeChanged: (AppThemeMode) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    val localizedResources = appResources()
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val repository = remember { GameRepository(context) }
    val appPreferences = remember { AppPreferences(context) }
    val homePreferences = remember(context) {
        DingooPreferenceRepository(context).settings("home_sort")
    }
    val globalBindings = remember { InputBindingPreferences(context) }
    var settings by remember { mutableStateOf(appPreferences.load()) }
    var sortMode by rememberSaveable {
        mutableStateOf(
            GameSortMode.entries.firstOrNull { mode ->
                mode.name == homePreferences.getString(
                    "sort_mode",
                    GameSortMode.Title.name
                )
            } ?: GameSortMode.Title
        )
    }
    var games by remember { mutableStateOf(repository.games(sortMode)) }
    var page by remember { mutableStateOf(AppPage.Home) }
    var selectedGame by remember { mutableStateOf<GameEntry?>(null) }
    var pendingAutoSaveGame by remember { mutableStateOf<GameEntry?>(null) }
    var pendingLaunchFromShortcut by remember { mutableStateOf(false) }
    var currentGameLaunchedFromShortcut by remember { mutableStateOf(false) }
    var summaryGame by remember { mutableStateOf<GameEntry?>(null) }
    var coverTarget by remember { mutableStateOf<GameEntry?>(null) }
    var coverDownloadInProgress by remember { mutableStateOf(false) }
    var initialStateSlot by remember { mutableStateOf<SaveStateSlot?>(null) }
    var managedSaveStates by remember {
        mutableStateOf<List<ManagedSaveStateInfo>>(emptyList())
    }
    var saveStateGameFilterId by remember { mutableStateOf<String?>(null) }
    var editorPortrait by remember { mutableStateOf(false) }
    var editorGameKey by remember { mutableStateOf<String?>(null) }
    var aboutVisible by remember { mutableStateOf(false) }
    fun refresh() { games = repository.games(sortMode) }
    fun refreshManagedSaveStates() {
        managedSaveStates = repository.managedSaveStates(
            games = games,
            gameId = saveStateGameFilterId
        )
    }
    fun updateSettings(value: AppSettingsState) {
        settings = value
        appPreferences.save(value)
        onThemeChanged(value.themeMode)
        onLanguageChanged(value.language)
        activity?.requestedOrientation = value.orientation.requestedOrientation
    }
    fun startGame(
        game: GameEntry,
        stateSlot: SaveStateSlot? = null,
        launchedFromShortcut: Boolean = false
    ) {
        pendingAutoSaveGame = null
        pendingLaunchFromShortcut = false
        currentGameLaunchedFromShortcut = launchedFromShortcut
        initialStateSlot = stateSlot
        selectedGame = game
    }
    fun requestGameLaunch(game: GameEntry, launchedFromShortcut: Boolean = false) {
        val autoState = repository.stateFile(game, SaveStateSlot.Auto)
        if (settings.autoSaveEnabled && autoState.isFile) {
            pendingAutoSaveGame = game
            pendingLaunchFromShortcut = launchedFromShortcut
        } else {
            startGame(
                game = game,
                launchedFromShortcut = launchedFromShortcut
            )
        }
    }

    val directoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            repository.setDirectory(uri)
                .onSuccess { count ->
                    refresh()
                    val message = if (count == 0) {
                        localizedResources.getString(R.string.main_library_no_games)
                    } else {
                        localizedResources.getString(
                            R.string.main_library_games_found,
                            count
                        )
                    }
                    showMiniQToast(
                        context,
                        message
                    )
                }
                .onFailure { error ->
                    showMiniQToast(
                        context,
                        localizedResources.getString(
                            R.string.main_library_read_failed,
                            error.message.orEmpty()
                        )
                    )
                }
        }
    }
    val gamePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            repository.game(uri)
                .onSuccess { game -> requestGameLaunch(game) }
                .onFailure { error ->
                    showMiniQToast(
                        context,
                        localizedResources.getString(
                            R.string.main_game_load_failed_detail,
                            error.message.orEmpty()
                        )
                    )
                }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = coverTarget
        if (uri != null && target != null) {
            repository.setCover(target, uri)
                .onSuccess {
                    refresh()
                    showMiniQToast(
                        context,
                        localizedResources.getString(R.string.main_cover_updated)
                    )
                }
                .onFailure { error ->
                    showMiniQToast(
                        context,
                        localizedResources.getString(
                            R.string.main_cover_update_failed,
                            error.message.orEmpty()
                        )
                    )
                }
        }
        coverTarget = null
    }
    LaunchedEffect(coverTarget) {
        if (coverTarget != null) coverPicker.launch(arrayOf("image/*"))
    }
    LaunchedEffect(sortMode) { refresh() }
    LaunchedEffect(shortcutLaunchRequest) {
        val request = shortcutLaunchRequest ?: return@LaunchedEffect
        onShortcutLaunchConsumed()

        if (selectedGame != null || pendingAutoSaveGame != null) {
            showMiniQToast(
                context,
                localizedResources.getString(R.string.main_shortcut_game_running)
            )
            return@LaunchedEffect
        }

        val game = request.gameUri?.let { gameUri ->
            games.firstOrNull { candidate -> candidate.uri == gameUri }
                // Android front-ends commonly pass a raw /storage path. On
                // scoped-storage devices, reuse the matching SAF library URI
                // instead of requesting broad all-files access.
                ?: gameUri.lastPathSegment?.let { externalFileName ->
                    games.singleOrNull { candidate ->
                        candidate.fileName.equals(externalFileName, ignoreCase = true)
                    }
                }
                ?: repository.game(gameUri).getOrNull()
        } ?: games.firstOrNull { candidate ->
            !request.gameId.isNullOrBlank() && candidate.id == request.gameId
        } ?: request.legacyGamePath
            ?.substringAfterLast('/')
            ?.let { legacyFileName ->
                games.firstOrNull { candidate ->
                    candidate.fileName.equals(legacyFileName, ignoreCase = true)
                }
            }

        if (game == null) {
            showMiniQToast(
                context,
                localizedResources.getString(R.string.main_shortcut_game_missing)
            )
        } else {
            requestGameLaunch(game, launchedFromShortcut = true)
        }
    }

    selectedGame?.let { game ->
        val bindings = remember(game.id) { InputBindingPreferences(context, null) }
        EmulationScreen(
            game = game,
            repository = repository,
            settings = settings,
            bindings = bindings,
            initialStateSlot = initialStateSlot,
            onSettingsChanged = ::updateSettings,
            onExit = {
                val shouldFinishActivity = currentGameLaunchedFromShortcut
                currentGameLaunchedFromShortcut = false
                selectedGame = null
                initialStateSlot = null
                if (shouldFinishActivity) {
                    activity?.finish()
                } else {
                    refresh()
                    page = AppPage.Home
                }
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when (page) {
        AppPage.Home -> HomeScreen(
            games = games,
            sortMode = sortMode,
            homePreferences = homePreferences,
            onSortMode = { mode ->
                sortMode = mode
                homePreferences
                    .edit()
                    .putString("sort_mode", mode.name)
                    .apply()
            },
            onAddGames = { directoryPicker.launch(repository.savedDirectory()) },
            onOpenGame = { gamePicker.launch(arrayOf("application/octet-stream", "*/*")) },
            onGameClick = {
                requestGameLaunch(it)
            },
            onLoadGameState = { game ->
                saveStateGameFilterId = game.id
                managedSaveStates = repository.managedSaveStates(games, game.id)
                page = AppPage.SaveStates
            },
            onGameSummary = { summaryGame = it; page = AppPage.Summary },
            onCoverSelected = { coverTarget = it },
            onSettings = { page = AppPage.Settings },
            onControlsSettings = { page = AppPage.Controls },
            onResetSettings = {
                DingooPreferenceRepository(context).resetAllSettings()
                globalBindings.resetToDefaults()
                ControlLayoutPreferences(context, portrait = true).reset()
                ControlLayoutPreferences(context, portrait = false).reset()
                updateSettings(AppSettingsState())
                showMiniQToast(
                    context,
                    localizedResources.getString(R.string.main_settings_reset_success)
                )
            },
            onScanGames = {
                refresh()
                val message = if (games.isEmpty()) {
                    localizedResources.getString(R.string.main_library_no_games)
                } else {
                    localizedResources.getString(
                        R.string.main_library_games_found,
                        games.size
                    )
                }
                showMiniQToast(
                    context,
                    message
                )
            },
            onManageSaveStates = {
                saveStateGameFilterId = null
                managedSaveStates = repository.managedSaveStates(games)
                page = AppPage.SaveStates
            },
            coverDownloadInProgress = coverDownloadInProgress,
            onDownloadGameCovers = {
                if (!coverDownloadInProgress) {
                    val gamesToDownload = games
                    if (gamesToDownload.isEmpty()) {
                        showMiniQToast(
                            context,
                            localizedResources.getString(R.string.main_cover_download_no_games)
                        )
                    } else {
                        coverDownloadInProgress = true
                        showMiniQToast(
                            context,
                            localizedResources.getString(R.string.main_cover_download_started)
                        )
                        scope.launch {
                            val summary = runCatching {
                                withContext(Dispatchers.IO) {
                                    DingooCoverDownloader().downloadMissingCovers(
                                        games = gamesToDownload,
                                        installCover = repository::setDownloadedCover
                                    )
                                }
                            }.getOrNull()

                            coverDownloadInProgress = false
                            if (summary == null) {
                                showMiniQToast(
                                    context,
                                    localizedResources.getString(R.string.main_cover_download_failed)
                                )
                            } else {
                                refresh()
                                showMiniQToast(
                                    context,
                                    localizedResources.getString(
                                        R.string.main_cover_download_result,
                                        summary.downloadedCount,
                                        summary.existingCount,
                                        summary.notFoundCount,
                                        summary.failedCount
                                    )
                                )
                            }
                        }
                    }
                }
            },
            onCreateShortcut = { game ->
                createShortcut(context, game)
            },
            onAbout = { aboutVisible = true }
        )
        AppPage.Settings -> SettingsScreen(settings, onSettingsChanged = ::updateSettings, onBack = { page = AppPage.Home })
        AppPage.Controls -> ControllerSettingsScreen(
            settings = settings, bindings = globalBindings, onSettingsChanged = ::updateSettings,
            onEditLayout = { editorPortrait = it; editorGameKey = null; page = AppPage.LayoutEditor },
            onBack = { page = AppPage.Home }
        )
        AppPage.Summary -> summaryGame?.let { game ->
            GameSummaryScreen(
                game, onBack = { page = AppPage.Home },
                onRename = { newTitle ->
                    repository.rename(game, newTitle)
                    refresh()
                    summaryGame = repository.games(sortMode).firstOrNull { it.id == game.id } ?: game
                }
            )
        } ?: run { page = AppPage.Home }
        AppPage.SaveStates -> SaveStateManagerScreen(
            states = managedSaveStates,
            onBack = {
                saveStateGameFilterId = null
                page = AppPage.Home
            },
            onLoad = { state ->
                page = AppPage.Home
                startGame(state.game, state.slot)
            },
            onDelete = { state ->
                val slotName = localizedResources.getString(
                    state.slot.displayNameResource
                )
                val deleted = repository.deleteState(state.game, state.slot)
                if (deleted) {
                    refreshManagedSaveStates()
                }
                showMiniQToast(
                    context,
                    localizedResources.getString(
                        if (deleted) {
                            R.string.save_state_message_deleted
                        } else {
                            R.string.save_state_message_delete_failed
                        },
                        slotName
                    )
                )
            }
        )
        AppPage.LayoutEditor -> VirtualControlEditorScreen(editorPortrait, editorGameKey) {
            page = AppPage.Controls
        }
            }
        }
    }

    if (aboutVisible) {
        AboutDialog(onDismiss = { aboutVisible = false })
    }

    pendingAutoSaveGame?.let { game ->
        val autoState = repository.stateFile(game, SaveStateSlot.Auto)
        AutoSaveResumeDialog(
            lastModified = autoState.lastModified(),
            previewPath = repository.previewFile(game, SaveStateSlot.Auto)
                .takeIf { it.isFile }
                ?.absolutePath,
            onDismiss = {
                pendingAutoSaveGame = null
                if (pendingLaunchFromShortcut) {
                    pendingLaunchFromShortcut = false
                    activity?.finish()
                } else {
                    showMiniQToast(
                        context,
                        localizedResources.getString(
                            R.string.main_game_launch_cancelled
                        )
                    )
                }
            },
            onDeleteState = {
                val deleted = repository.deleteState(game, SaveStateSlot.Auto)
                showMiniQToast(
                    context,
                    localizedResources.getString(
                        if (deleted) {
                            R.string.main_auto_save_deleted
                        } else {
                            R.string.main_auto_save_delete_failed
                        }
                    )
                )
                deleted
            },
            onCleanBoot = {
                startGame(
                    game = game,
                    launchedFromShortcut = pendingLaunchFromShortcut
                )
            },
            onLoadState = {
                startGame(
                    game = game,
                    stateSlot = SaveStateSlot.Auto,
                    launchedFromShortcut = pendingLaunchFromShortcut
                )
            }
        )
    }
}

private fun createShortcut(context: android.content.Context, game: GameEntry) {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = game.uri
        flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        putExtra(MainActivity.EXTRA_GAME_ID, game.id)
    }
    val coverBitmap = game.coverFile
        ?.takeIf { it.isFile }
        ?.let { coverFile ->
            runCatching {
                BitmapFactory.decodeFile(coverFile.absolutePath)
            }.getOrNull()
    }
    val shortcutIcon = if (coverBitmap != null) {
        IconCompat.createWithBitmap(coverBitmap)
    } else {
        IconCompat.createWithResource(context, R.mipmap.ic_launcher)
    }
    val shortcut = ShortcutInfoCompat.Builder(context, "game-${game.id}")
        .setShortLabel(game.title)
        .setLongLabel(game.title)
        .setIcon(shortcutIcon)
        .setIntent(intent)
        .build()
    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}
