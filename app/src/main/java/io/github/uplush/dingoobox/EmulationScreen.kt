package io.github.uplush.dingoobox

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.LockSupport
import kotlin.math.max

private const val PORTRAIT_GAME_X = 0.50f
private const val PORTRAIT_GAME_Y = 0.36f
private const val PORTRAIT_GAME_SCALE = 1.0f

@Composable
fun EmulationScreen(
    game: GameEntry,
    repository: GameRepository,
    settings: AppSettingsState,
    bindings: InputBindingPreferences,
    initialStateSlot: SaveStateSlot? = null,
    onSettingsChanged: (AppSettingsState) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val localizedResources = appResources()
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val gameAudioFocus = remember(context) { GameAudioFocus(context) }
    val screenshotManager = remember(context) { ScreenshotManager(context) }
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val gameUri = game.uri.toString()
    val bitmap = remember(gameUri) { createBitmap(320, 240, Bitmap.Config.RGB_565) }
    val bitmapView = remember(gameUri) { BitmapView(context, bitmap) }
    var startedAt by remember(gameUri) { mutableStateOf<Long?>(null) }
    val session = remember(gameUri, initialStateSlot) {
        EmulationSession(
            romData = repository.readGame(game).getOrNull(),
            romName = game.fileName,
            saveDirectory = repository.savesDirectory,
            frameRateEnhancementEnabled = settings.frameRateEnhancementEnabled,
            bitmap = bitmap,
            view = bitmapView,
            onCoreShutdown = {
                bitmapView.post {
                    // A state captured after the emulated application has
                    // returned only resumes to its blank exit framebuffer.
                    // Keep manual/quick states, but remove stale auto-resume
                    // data. Native deinitialization still flushes SRAM/EEPROM.
                    if (!repository.deleteState(game, SaveStateSlot.Auto)) {
                        Log.w(
                            "DingooLifecycle",
                            "Unable to remove stale auto state after game exit"
                        )
                    }
                    startedAt?.let { startTime ->
                        repository.addPlayTime(
                            game,
                            System.currentTimeMillis() - startTime
                        )
                    }
                    startedAt = null
                    onExit()
                }
            }
        )
    }
    val currentSettings by rememberUpdatedState(settings)
    var pauseMenu by remember { mutableStateOf(false) }
    var saveStateScreenMode by remember { mutableStateOf<SaveStateScreenMode?>(null) }
    var saveStateRevision by remember { mutableIntStateOf(0) }
    var screenshotMessage by remember { mutableStateOf<String?>(null) }
    var pendingScreenshotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fastForward by remember { mutableStateOf(false) }
    var editorPortrait by remember { mutableStateOf<Boolean?>(null) }
    // MiNiQ recreates the emulation/pause subtree after closing its separate
    // control editor. Re-key only the pause presentation to preserve this
    // first-frame-visible behavior without disposing DingooEmu's core session.
    var pauseMenuPresentationKey by remember { mutableIntStateOf(0) }
    var layoutRevision by remember { mutableIntStateOf(0) }
    var restoreControllerTouchPage by remember { mutableStateOf(false) }
    var gameSpecific by remember(game.id) {
        mutableStateOf(
            ControlSettingsScopePreferences(context).isGameIndependent(game.id)
        )
    }
    val activeBindings = remember(game.id, gameSpecific) {
        if (gameSpecific) InputBindingPreferences(context, game.id) else bindings
    }
    val controlLayout = remember(portrait, gameSpecific, layoutRevision) {
        ControlLayoutPreferences(context, portrait, game.id.takeIf { gameSpecific }).load()
    }
    val controlBehavior = remember(
        game.id,
        gameSpecific,
        layoutRevision,
        settings.virtualControlsVisible,
        settings.vibrationEnabled
    ) {
        ControlBehaviorPreferences(context, game.id.takeIf { gameSpecific }).load(settings)
    }
    val focusRequester = remember { FocusRequester() }
    val saveStateSlots = remember(game.id, saveStateRevision) {
        repository.saveStateSlots(game)
    }
    var sessionStarted by remember(session) { mutableStateOf(false) }

    fun startPreparedGame(message: String? = null) {
        if (!sessionStarted) {
            gameAudioFocus.request()
            sessionStarted = session.start()
            if (sessionStarted) {
                startedAt = System.currentTimeMillis()
                repository.recordStarted(game)
            } else {
                gameAudioFocus.abandon()
            }
        }
        if (message != null) showMiniQToast(context, message)
        focusRequester.requestFocus()
    }

    fun showStatusToast(message: String) {
        showMiniQToast(context, message)
    }

    fun save(slot: SaveStateSlot): Boolean {
        val okay = session.saveState(repository.stateFile(game, slot))
        if (okay) savePreview(bitmap, repository.previewFile(game, slot))
        return okay
    }
    fun exitGame() {
        if (settings.autoSaveEnabled && session.started) save(SaveStateSlot.Auto)
        onExit()
    }
    fun releaseAllGameInput() {
        DingooInputAction.entries.forEach { action ->
            NativeBridge.nativeSetButton(action.retroButtonId, false)
        }
    }
    fun resume() {
        restoreControllerTouchPage = false
        pauseMenu = false
        gameAudioFocus.request()
        session.resume()
        focusRequester.requestFocus()
    }
    val saveScreenshotLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val pendingBitmap = pendingScreenshotBitmap
        if (uri == null) {
            pendingScreenshotBitmap = null
            pauseMenu = true
        } else {
            val saved = pendingBitmap != null && synchronized(pendingBitmap) {
                screenshotManager.saveToUri(pendingBitmap, uri)
            }
            pendingScreenshotBitmap = null
            screenshotMessage = localizedResources.getString(
                if (saved) {
                    R.string.main_screenshot_saved
                } else {
                    R.string.main_screenshot_failed
                }
            )
            resume()
        }
    }
    fun openPause() {
        restoreControllerTouchPage = false
        releaseAllGameInput()
        session.pause()
        pauseMenu = true
    }
    fun toggleFastForward() {
        fastForward = !fastForward
        showStatusToast(
            localizedResources.getString(
                if (fastForward) {
                    R.string.main_fast_forward_enabled
                } else {
                    R.string.main_fast_forward_disabled
                }
            )
        )
    }
    fun quickSave() {
        val slot = SaveStateSlot.Quick
        val slotName = localizedResources.getString(slot.displayNameResource)
        showStatusToast(
            localizedResources.getString(
                if (save(slot)) {
                    R.string.save_state_message_save_success
                } else {
                    R.string.save_state_message_save_failed
                },
                slotName
            )
        )
        saveStateRevision++
    }
    fun quickLoad() {
        val slot = SaveStateSlot.Quick
        val slotName = localizedResources.getString(slot.displayNameResource)
        val stateFile = repository.stateFile(game, slot)
        val message = when {
            !stateFile.isFile -> localizedResources.getString(
                R.string.save_state_message_no_state,
                slotName
            )
            session.loadState(stateFile) -> localizedResources.getString(
                R.string.save_state_message_load_success,
                slotName
            )
            else -> localizedResources.getString(
                R.string.save_state_message_load_failed,
                slotName
            )
        }
        showStatusToast(message)
    }
    fun saveScreenshotDirectly() {
        screenshotMessage = localizedResources.getString(
            if (synchronized(bitmap) { screenshotManager.save(bitmap) }) {
                R.string.main_screenshot_saved
            } else {
                R.string.main_screenshot_failed
            }
        )
    }

    fun requestScreenshotDestination() {
        pendingScreenshotBitmap = bitmap
        val baseName = game.fileName
            .substringBeforeLast(".")
            .takeIf { it.isNotBlank() }
            ?: "DingooBox_Screenshot"
        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss",
            Locale.US
        ).format(Date())
        pauseMenu = false
        saveScreenshotLauncher.launch("$baseName  $timestamp.png")
    }
    fun resetGame() {
        session.reset()
        showStatusToast(
            localizedResources.getString(R.string.pause_message_restart_success)
        )
    }

    LaunchedEffect(screenshotMessage) {
        if (screenshotMessage != null) {
            kotlinx.coroutines.delay(1800)
            screenshotMessage = null
        }
    }

    DisposableEffect(session, initialStateSlot) {
        val initialStateLoaded = initialStateSlot?.let { slot ->
            session.loadState(repository.stateFile(game, slot))
        }
        if (initialStateLoaded == false) session.reset()
        val launchMessage = when {
            initialStateSlot == SaveStateSlot.Auto && initialStateLoaded == true ->
                localizedResources.getString(R.string.main_auto_save_loaded)
            initialStateSlot == SaveStateSlot.Auto && initialStateLoaded == false ->
                localizedResources.getString(
                    R.string.main_auto_save_load_failed_started
                )
            else -> null
        }
        startPreparedGame(launchMessage)
        onDispose {
            if (session.started) {
                session.pause()
                gameAudioFocus.abandon()
                if (currentSettings.autoSaveEnabled) {
                    save(SaveStateSlot.Auto)
                }
                startedAt?.let { startTime ->
                    repository.addPlayTime(
                        game,
                        System.currentTimeMillis() - startTime
                    )
                }
            }
            session.stop()
        }
    }

    LaunchedEffect(
        settings.gameMuted,
        settings.volumePercent,
        settings.imageFilter,
        settings.soundSofteningEnabled,
        settings.soundSofteningPercent,
        fastForward,
        settings.fastForwardMultiplier
    ) {
        session.setVolume(if (settings.gameMuted) 0f else settings.volumePercent / 100f)
        session.setFastForward(if (fastForward) settings.fastForwardMultiplier else 1)
        session.setSoundSoftening(
            enabled = settings.soundSofteningEnabled,
            percent = settings.soundSofteningPercent
        )
        bitmapView.setDisplaySettings(settings.imageFilter)
    }
    DisposableEffect(
        settings.immersiveMode,
        pauseMenu,
        sessionStarted,
        activity
    ) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (
                settings.immersiveMode &&
                sessionStarted &&
                !pauseMenu
            ) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(lifecycleOwner, pauseMenu, editorPortrait, sessionStarted) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (sessionStarted) {
                        if (currentSettings.autoSaveEnabled) save(SaveStateSlot.Auto)
                        session.pause()
                        gameAudioFocus.abandon()
                    }
                }
                Lifecycle.Event.ON_RESUME -> if (
                    sessionStarted && !pauseMenu && editorPortrait == null
                ) {
                    gameAudioFocus.request()
                    session.resume()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(
        rootView,
        activeBindings,
        pauseMenu,
        editorPortrait,
        sessionStarted
    ) {
        if (sessionStarted && !pauseMenu && editorPortrait == null) {
            val pressedActions = mutableSetOf<DingooInputAction>()
            val activeHotkeys = mutableSetOf<DingooHotkeyAction>()
            rootView.setOnGenericMotionListener { _, event ->
                handlePhysicalMotion(
                    event = event,
                    bindings = activeBindings,
                    pressedActions = pressedActions,
                    activeHotkeys = activeHotkeys,
                    onPause = ::openPause,
                    onFastForward = ::toggleFastForward,
                    onQuickSave = ::quickSave,
                    onQuickLoad = ::quickLoad,
                    onScreenshot = ::saveScreenshotDirectly,
                    onReset = ::resetGame
                )
            }
            onDispose {
                pressedActions.forEach {
                    NativeBridge.nativeSetButton(it.retroButtonId, false)
                }
                rootView.setOnGenericMotionListener(null)
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BackHandler(
        enabled = sessionStarted && !pauseMenu && editorPortrait == null
    ) {
        openPause()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black)
            .then(if (pauseMenu) Modifier.blur(4.dp) else Modifier)
            .focusRequester(focusRequester).focusable()
            .onPreviewKeyEvent { event ->
                if (!sessionStarted || pauseMenu || editorPortrait != null) {
                    false
                } else {
                    handlePhysicalKey(
                        event.nativeKeyEvent, activeBindings,
                        onPause = ::openPause,
                        onFastForward = ::toggleFastForward,
                        onQuickSave = ::quickSave,
                        onQuickLoad = ::quickLoad,
                        onScreenshot = ::saveScreenshotDirectly,
                        onReset = ::resetGame
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (session.initialized) {
            if (sessionStarted) {
                GameViewport(bitmapView, settings.aspectRatio, portrait)
                if (
                    controlBehavior.virtualControlsVisible &&
                    !pauseMenu &&
                    editorPortrait == null
                ) {
                    VirtualControls(
                        layout = controlLayout,
                        vibration = controlBehavior.vibrationEnabled,
                        portrait = portrait,
                        onButton = { id, pressed -> NativeBridge.nativeSetButton(id, pressed) }
                    )
                }
                PerformanceOverlay(
                    session = session,
                    settings = settings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .symmetricCutoutPadding()
                        .padding(
                            top = if (portrait) 38.dp else 30.dp,
                            end = 104.dp
                        )
                )
                GameOverlayControls(
                    fastForwardEnabled = fastForward,
                    onPause = ::openPause,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .symmetricCutoutPadding()
                        .padding(
                            top = if (portrait) 24.dp else 16.dp,
                            end = 16.dp
                        )
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text(
                    appStringResource(R.string.emulation_start_failed_title),
                    color = ComposeColor.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    appStringResource(
                        R.string.emulation_start_failed_message,
                        game.title
                    ),
                    color = ComposeColor.LightGray
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onExit) {
                    Text(appStringResource(R.string.emulation_return_to_library))
                }
            }
        }

        GameScreenshotMessage(
            message = screenshotMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(
                    if (portrait) {
                        Modifier.padding(bottom = 96.dp)
                    } else {
                        Modifier
                            .symmetricCutoutPadding()
                            .padding(bottom = 20.dp)
                    }
                )
        )
    }

    key(pauseMenuPresentationKey) {
        AnimatedVisibility(
            visible = pauseMenu,
            enter = fadeIn(animationSpec = tween(durationMillis = 150, easing = LinearEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = LinearEasing))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            )
        }
    }

    if (saveStateScreenMode == null) {
        key(pauseMenuPresentationKey) {
            AnimatedVisibility(
                visible = pauseMenu,
                enter = fadeIn(animationSpec = tween(durationMillis = 150, easing = LinearEasing)),
                exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = LinearEasing))
            ) {
                PauseMenu(
                    game = game,
                    settings = settings,
                    bindings = activeBindings,
                    gameSpecificEnabled = gameSpecific,
                    openControllerSettingsInitially = restoreControllerTouchPage,
                    onGameSpecificEnabled = {
                        gameSpecific = it
                        layoutRevision++
                    },
                    onSettingsChanged = onSettingsChanged,
                    onResume = ::resume,
                    onLoadState = {
                        saveStateRevision++
                        saveStateScreenMode = SaveStateScreenMode.LOAD
                    },
                    onSaveState = {
                        saveStateRevision++
                        saveStateScreenMode = SaveStateScreenMode.SAVE
                    },
                    onToggleFastForward = {
                        toggleFastForward()
                    },
                    onExit = ::exitGame,
                    onScreenshot = {
                        requestScreenshotDestination()
                    },
                    onReset = {
                        resetGame()
                        resume()
                    },
                    onRename = { repository.rename(game, it) },
                    onEditLayout = {
                        releaseAllGameInput()
                        session.pause()
                        editorPortrait = it
                        restoreControllerTouchPage = true
                        pauseMenu = false
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
        }
    }
    saveStateScreenMode?.let { mode ->
        SaveStateScreen(
            slots = saveStateSlots,
            mode = mode,
            onSave = { slot ->
                val slotName = localizedResources.getString(slot.displayNameResource)
                showStatusToast(
                    localizedResources.getString(
                        if (save(slot)) {
                            R.string.save_state_message_save_success
                        } else {
                            R.string.save_state_message_save_failed
                        },
                        slotName
                    )
                )
                saveStateRevision++
            },
            onLoad = { slot ->
                val slotName = localizedResources.getString(slot.displayNameResource)
                val stateFile = repository.stateFile(game, slot)
                val message = when {
                    !stateFile.isFile -> localizedResources.getString(
                        R.string.save_state_message_no_state,
                        slotName
                    )
                    session.loadState(stateFile) -> {
                        saveStateScreenMode = null
                        resume()
                        localizedResources.getString(
                            R.string.save_state_message_load_success,
                            slotName
                        )
                    }
                    else -> localizedResources.getString(
                        R.string.save_state_message_load_failed,
                        slotName
                    )
                }
                showStatusToast(message)
            },
            onDelete = { slot ->
                val slotName = localizedResources.getString(slot.displayNameResource)
                showStatusToast(
                    localizedResources.getString(
                        if (repository.deleteState(game, slot)) {
                            R.string.save_state_message_deleted
                        } else {
                            R.string.save_state_message_delete_failed
                        },
                        slotName
                    )
                )
                saveStateRevision++
            },
            onBack = { saveStateScreenMode = null },
            modifier = Modifier.fillMaxSize()
        )
    }
    editorPortrait?.let { editPortrait ->
        VirtualControlEditorScreen(editPortrait, game.id.takeIf { gameSpecific }) {
            editorPortrait = null
            layoutRevision++
            pauseMenu = true
            pauseMenuPresentationKey++
        }
    }

}

@Composable
private fun GameOverlayControls(
    fastForwardEnabled: Boolean,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (fastForwardEnabled) {
            Icon(
                imageVector = Icons.Filled.FastForward,
                contentDescription = appStringResource(
                    R.string.emulation_fast_forward_description
                ),
                modifier = Modifier.size(26.dp),
                tint = ComposeColor.White.copy(alpha = 0.72f)
            )
        }
        GamePauseButton(onClick = onPause)
    }
}

@Composable
private fun GamePauseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val buttonScale = animateFloatAsState(
        targetValue = if (isPressed) 1.50f else 1.0f,
        animationSpec = tween(
            durationMillis = if (isPressed) 70 else 100,
            easing = FastOutSlowInEasing
        ),
        label = "PauseButtonScale"
    ).value

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Pause,
            contentDescription = appStringResource(R.string.emulation_pause_game),
            modifier = Modifier
                .size(28.dp)
                .scale(buttonScale),
            tint = ComposeColor.White.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun GameScreenshotMessage(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 150,
                easing = LinearEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 120,
                easing = LinearEasing
            )
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.90f),
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 11.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun GameViewport(
    view: BitmapView,
    aspectRatio: AspectRatioMode,
    portrait: Boolean
) {
    BoxWithConstraints(
        Modifier.fillMaxSize().clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val screenRatio = maxWidth / maxHeight
        val modifier = if (portrait) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .offset(
                    x = maxWidth * (PORTRAIT_GAME_X - 0.5f),
                    y = maxHeight * (PORTRAIT_GAME_Y - 0.5f)
                )
                .scale(PORTRAIT_GAME_SCALE)
        } else {
            when (aspectRatio) {
                AspectRatioMode.Stretch -> Modifier.fillMaxSize()
                AspectRatioMode.Original ->
                    if (screenRatio > 4f / 3f) Modifier.fillMaxHeight().aspectRatio(4f / 3f)
                    else Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                AspectRatioMode.Fill ->
                    if (screenRatio > 4f / 3f) Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                    else Modifier.fillMaxHeight().aspectRatio(4f / 3f)
            }
        }
        AndroidView(factory = { view }, modifier = modifier)
    }
}

@Composable
private fun PerformanceOverlay(
    session: EmulationSession,
    settings: AppSettingsState,
    modifier: Modifier = Modifier
) {
    var stats by remember { mutableStateOf(session.stats()) }
    LaunchedEffect(session) {
        while (true) { kotlinx.coroutines.delay(500); stats = session.stats() }
    }

    if (
        !settings.showInformation ||
        (!settings.showEmulationSpeed && !settings.showFps) ||
        stats.first <= 0f
    ) {
        return
    }

    val informationParts = mutableListOf<String>()
    if (settings.showFps) {
        informationParts += String.format(Locale.US, "FPS: %.1f", stats.first)
    }
    if (settings.showEmulationSpeed) {
        informationParts += String.format(Locale.US, "%.0f%%", stats.second)
    }

    Text(
        text = informationParts.joinToString(separator = " | "),
        modifier = modifier,
        color = ComposeColor.White,
        maxLines = 1,
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = FontFamily.Monospace,
            shadow = Shadow(
                color = ComposeColor.Black,
                offset = Offset(2f, 2f),
                blurRadius = 2f
            )
        )
    )
}

private fun handlePhysicalKey(
    event: KeyEvent,
    bindings: InputBindingPreferences,
    onPause: () -> Unit,
    onFastForward: () -> Unit,
    onQuickSave: () -> Unit,
    onQuickLoad: () -> Unit,
    onScreenshot: () -> Unit,
    onReset: () -> Unit
): Boolean {
    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
        when (bindings.hotkeyForKey(event.keyCode)) {
            DingooHotkeyAction.Pause -> onPause()
            DingooHotkeyAction.FastForward -> onFastForward()
            DingooHotkeyAction.QuickSave -> onQuickSave()
            DingooHotkeyAction.QuickLoad -> onQuickLoad()
            DingooHotkeyAction.Screenshot -> onScreenshot()
            DingooHotkeyAction.Reset -> onReset()
            null -> Unit
        }.also {
            if (bindings.hotkeyForKey(event.keyCode) != null) return true
        }
    }
    val action = bindings.actionForKey(event.keyCode) ?: return false
    when (event.action) {
        KeyEvent.ACTION_DOWN -> NativeBridge.nativeSetButton(action.retroButtonId, true)
        KeyEvent.ACTION_UP -> NativeBridge.nativeSetButton(action.retroButtonId, false)
    }
    return true
}

private fun handlePhysicalMotion(
    event: MotionEvent,
    bindings: InputBindingPreferences,
    pressedActions: MutableSet<DingooInputAction>,
    activeHotkeys: MutableSet<DingooHotkeyAction>,
    onPause: () -> Unit,
    onFastForward: () -> Unit,
    onQuickSave: () -> Unit,
    onQuickLoad: () -> Unit,
    onScreenshot: () -> Unit,
    onReset: () -> Unit
): Boolean {
    if (event.action != MotionEvent.ACTION_MOVE ||
        (!event.isFromSource(InputDevice.SOURCE_JOYSTICK) &&
            !event.isFromSource(InputDevice.SOURCE_GAMEPAD))
    ) return false

    var handled = false
    DingooInputAction.entries.forEach { action ->
        val binding = bindings.getBinding(action)
        if (binding?.type != PhysicalInputBindingType.AXIS) return@forEach
        val pressed = event.getAxisValue(binding.code) * binding.direction >= 0.65f
        val wasPressed = action in pressedActions
        if (pressed != wasPressed) {
            if (pressed) pressedActions += action else pressedActions -= action
            NativeBridge.nativeSetButton(action.retroButtonId, pressed)
        }
        handled = handled || pressed || wasPressed
    }

    DingooHotkeyAction.entries.forEach { action ->
        val binding = bindings.getHotkeyBinding(action)
        if (binding?.type != PhysicalInputBindingType.AXIS) return@forEach
        val active = event.getAxisValue(binding.code) * binding.direction >= 0.65f
        val wasActive = action in activeHotkeys
        if (active && !wasActive) {
            activeHotkeys += action
            when (action) {
                DingooHotkeyAction.Pause -> onPause()
                DingooHotkeyAction.FastForward -> onFastForward()
                DingooHotkeyAction.QuickSave -> onQuickSave()
                DingooHotkeyAction.QuickLoad -> onQuickLoad()
                DingooHotkeyAction.Screenshot -> onScreenshot()
                DingooHotkeyAction.Reset -> onReset()
            }
        } else if (!active && wasActive) {
            activeHotkeys -= action
        }
        handled = handled || active || wasActive
    }
    return handled
}

private fun savePreview(bitmap: Bitmap, destination: File) {
    runCatching {
        destination.parentFile?.mkdirs()
        destination.outputStream().use { output ->
            synchronized(bitmap) { bitmap.compress(Bitmap.CompressFormat.PNG, 90, output) }
        }
    }
}

@SuppressLint("ViewConstructor")
private class BitmapView(context: Context, private val bitmap: Bitmap) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = false }
    private val effectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val destination = Rect()
    private var imageFilterMode = ImageFilterMode.DotMatrix

    fun setDisplaySettings(imageFilter: ImageFilterMode) {
        imageFilterMode = imageFilter
        paint.isFilterBitmap = imageFilter == ImageFilterMode.EdgeSmoothing
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        synchronized(bitmap) {
            destination.set(0, 0, width, height)
            canvas.drawBitmap(bitmap, null, destination, paint)
        }
        when (imageFilterMode) {
            ImageFilterMode.DotMatrix -> drawDotMatrix(canvas)
            ImageFilterMode.Scanlines -> drawScanlines(canvas)
            ImageFilterMode.EdgeSmoothing,
            ImageFilterMode.Off -> Unit
        }
    }

    private fun drawDotMatrix(canvas: Canvas) {
        val cellWidth = width / 320f
        val cellHeight = height / 240f
        if (cellWidth < 2f || cellHeight < 2f) return
        effectPaint.color = Color.argb(30, 0, 0, 0)
        effectPaint.strokeWidth = max(1f, minOf(cellWidth, cellHeight) * 0.10f)
        for (x in 1 until 320) {
            val drawX = x * cellWidth
            canvas.drawLine(drawX, 0f, drawX, height.toFloat(), effectPaint)
        }
        for (y in 1 until 240) {
            val drawY = y * cellHeight
            canvas.drawLine(0f, drawY, width.toFloat(), drawY, effectPaint)
        }
    }

    private fun drawScanlines(canvas: Canvas) {
        val lineStep = height / 240f
        if (lineStep < 1.5f) return
        effectPaint.color = Color.argb(54, 0, 0, 0)
        effectPaint.strokeWidth = max(1f, lineStep * 0.18f)
        for (y in 1 until 240) {
            val drawY = y * lineStep
            canvas.drawLine(0f, drawY, width.toFloat(), drawY, effectPaint)
        }
    }

}

private class EmulationSession(
    romData: ByteArray?,
    romName: String,
    saveDirectory: File,
    frameRateEnhancementEnabled: Boolean,
    private val bitmap: Bitmap,
    private val view: View,
    private val onCoreShutdown: () -> Unit
) {
    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var speedMultiplier = 1
    @Volatile private var measuredFps = 0f
    @Volatile private var measuredSpeedPercent = 100f
    @Volatile private var soundSofteningEnabled = false
    @Volatile private var soundSofteningPercent = 60
    private var filteredLeft = 0f
    private var filteredRight = 0f
    private var filterInitialized = false
    private var thread: Thread? = null
    private val audioBuffer = ShortArray(4096)
    private var audioTrack: AudioTrack?
    @Volatile private var outputVolume = 1f
    private var firstAudioBufferLogged = false
    private var firstAudioWriteLogged = false
    private var framesWithoutAudio = 0
    val initialized: Boolean
    val started: Boolean
        get() = running

    init {
        saveDirectory.mkdirs()
        initialized = romData != null && NativeBridge.nativeInitialize(
            romData,
            romName,
            saveDirectory.absolutePath,
            frameRateEnhancementEnabled
        )
        audioTrack = if (initialized) createAudioTrack() else null
    }
    fun start(): Boolean {
        if (!initialized) return false
        if (running) return true
        paused = false
        running = true
        startAudioPlayback("session start")
        thread = Thread({ runLoop() }, "DingooEmulation").apply { start() }
        return true
    }
    fun pause() {
        paused = true
        runCatching { audioTrack?.pause() }
            .onFailure { error ->
                Log.e(AUDIO_LOG_TAG, "AudioTrack pause failed", error)
            }
    }
    fun resume() {
        if (initialized && running) {
            paused = false
            startAudioPlayback("session resume")
        }
    }
    fun setVolume(value: Float) {
        outputVolume = value.coerceIn(0f, 1f)
        val result = audioTrack?.let { track ->
            runCatching { track.setVolume(outputVolume) }
                .onFailure { error ->
                    Log.e(AUDIO_LOG_TAG, "AudioTrack volume update failed", error)
                }
                .getOrNull()
        }
        Log.i(
            AUDIO_LOG_TAG,
            "Output volume applied: volume=$outputVolume result=${result ?: "no-track"}"
        )
    }
    fun setFastForward(multiplier: Int) {
        speedMultiplier = if (multiplier == 0) 0 else multiplier.coerceIn(1, 4)
    }
    fun setSoundSoftening(enabled: Boolean, percent: Int) {
        soundSofteningEnabled = enabled
        soundSofteningPercent = percent.coerceIn(5, 95)
        if (!enabled) filterInitialized = false
    }
    fun stats(): Pair<Float, Float> = measuredFps to measuredSpeedPercent
    fun saveState(file: File): Boolean { file.parentFile?.mkdirs(); return NativeBridge.nativeSaveState(file.absolutePath) }
    fun loadState(file: File): Boolean = file.exists() && NativeBridge.nativeLoadState(file.absolutePath)
    fun reset() = NativeBridge.nativeReset()
    fun stop() {
        if (!initialized) return
        running = false; thread?.join(1200); thread = null
        runCatching { audioTrack?.stop() }; audioTrack?.release(); NativeBridge.nativeDeinitialize()
    }
    private fun runLoop() {
        var deadline = System.nanoTime()
        var measuredAt = deadline
        var frames = 0
        while (running) {
            if (paused) {
                LockSupport.parkNanos(4_000_000L); deadline = System.nanoTime(); measuredAt = deadline; frames = 0
                continue
            }
            val audioSamples = synchronized(bitmap) { NativeBridge.nativeRunFrame(bitmap, audioBuffer) }
            if (audioSamples == NativeBridge.RUN_FRAME_SHUTDOWN) {
                Log.i(CORE_LIFECYCLE_LOG_TAG, "Game requested frontend shutdown")
                running = false
                onCoreShutdown()
                break
            }
            if (audioSamples > 0) {
                framesWithoutAudio = 0
                if (!firstAudioBufferLogged) {
                    firstAudioBufferLogged = true
                    val peak = audioBuffer
                        .take(audioSamples)
                        .maxOfOrNull { kotlin.math.abs(it.toInt()) }
                        ?: 0
                    Log.i(
                        AUDIO_LOG_TAG,
                        "First core audio buffer: samples=$audioSamples peak=$peak"
                    )
                }
                softenAudio(audioSamples)
                writeAudio(audioSamples)
            } else {
                framesWithoutAudio++
                if (framesWithoutAudio == 180) {
                    Log.w(
                        AUDIO_LOG_TAG,
                        "Core returned no audio samples for 180 consecutive frames"
                    )
                }
            }
            view.postInvalidateOnAnimation(); frames++
            val now = System.nanoTime()
            if (now - measuredAt >= 1_000_000_000L) {
                val elapsed = now - measuredAt
                measuredFps = frames.toFloat() * 1_000_000_000f / elapsed.toFloat()
                measuredSpeedPercent = (measuredFps / 60f * 100f).coerceAtLeast(0f)
                frames = 0
                measuredAt = now
            }
            val currentMultiplier = speedMultiplier
            if (currentMultiplier == 0) {
                deadline = System.nanoTime()
            } else {
                val frameNanos = 1_000_000_000L / (60L * currentMultiplier)
                deadline += frameNanos
                val remaining = deadline - System.nanoTime()
                if (remaining > 0) {
                    LockSupport.parkNanos(remaining)
                } else {
                    deadline = System.nanoTime()
                }
            }
        }
    }

    private fun softenAudio(sampleCount: Int) {
        if (!soundSofteningEnabled || sampleCount < 2) return
        val alpha = 1f - (soundSofteningPercent / 100f) * 0.88f
        var index = 0
        if (!filterInitialized) {
            filteredLeft = audioBuffer[0].toFloat()
            filteredRight = audioBuffer[1].toFloat()
            filterInitialized = true
        }
        while (index + 1 < sampleCount) {
            filteredLeft += alpha * (audioBuffer[index] - filteredLeft)
            filteredRight += alpha * (audioBuffer[index + 1] - filteredRight)
            audioBuffer[index] = filteredLeft.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            audioBuffer[index + 1] = filteredRight.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            index += 2
        }
    }
    private fun startAudioPlayback(reason: String) {
        val track = audioTrack
        if (track == null) {
            Log.e(AUDIO_LOG_TAG, "Cannot start audio ($reason): AudioTrack is unavailable")
            return
        }
        runCatching {
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
        }.onSuccess {
            Log.i(
                AUDIO_LOG_TAG,
                "Audio playback ready ($reason): state=${track.state} playState=${track.playState}"
            )
        }.onFailure { error ->
            Log.e(AUDIO_LOG_TAG, "AudioTrack play failed ($reason)", error)
        }
    }

    private fun writeAudio(sampleCount: Int) {
        var track = audioTrack ?: return
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            startAudioPlayback("audio write")
        }
        var written = writeToTrack(
            track = track,
            sampleCount = sampleCount,
            writeMode = if (speedMultiplier == 1) {
                AudioTrack.WRITE_BLOCKING
            } else {
                AudioTrack.WRITE_NON_BLOCKING
            }
        )
        if (
            written == AudioTrack.ERROR_DEAD_OBJECT ||
            written == AudioTrack.ERROR_INVALID_OPERATION
        ) {
            Log.w(
                AUDIO_LOG_TAG,
                "AudioTrack write requires recovery: result=$written; recreating it"
            )
            runCatching { track.release() }
            audioTrack = createAudioTrack()
            track = audioTrack ?: return
            runCatching { track.setVolume(outputVolume) }
            startAudioPlayback("track recovery")
            written = writeToTrack(
                track = track,
                sampleCount = sampleCount,
                writeMode = AudioTrack.WRITE_BLOCKING
            )
        }
        if (written < 0) {
            Log.e(
                AUDIO_LOG_TAG,
                "AudioTrack write failed: result=$written requested=$sampleCount"
            )
        } else if (!firstAudioWriteLogged) {
            firstAudioWriteLogged = true
            Log.i(
                AUDIO_LOG_TAG,
                "First AudioTrack write: requested=$sampleCount written=$written playState=${track.playState}"
            )
        }
    }

    private fun writeToTrack(
        track: AudioTrack,
        sampleCount: Int,
        writeMode: Int
    ): Int = runCatching {
        track.write(
            audioBuffer,
            0,
            sampleCount,
            writeMode
        )
    }.onFailure { error ->
        Log.e(AUDIO_LOG_TAG, "AudioTrack write threw an exception", error)
    }.getOrDefault(AudioTrack.ERROR_INVALID_OPERATION)

    private fun createAudioTrack(): AudioTrack? {
        val minimum = AudioTrack.getMinBufferSize(22_050, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) {
            Log.e(AUDIO_LOG_TAG, "Unsupported AudioTrack format: minBufferSize=$minimum")
            return null
        }
        val bufferSize = max(minimum * 4, 16_384)
        return runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(22_050)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.onSuccess { track ->
            Log.i(
                AUDIO_LOG_TAG,
                "AudioTrack created: state=${track.state} session=${track.audioSessionId} bufferBytes=$bufferSize"
            )
        }.onFailure { error ->
            Log.e(AUDIO_LOG_TAG, "AudioTrack creation failed", error)
        }.getOrNull()
    }

    private companion object {
        const val CORE_LIFECYCLE_LOG_TAG = "DingooLifecycle"
        const val AUDIO_LOG_TAG = "DingooAudio"
    }
}
