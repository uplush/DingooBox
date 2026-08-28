package io.github.uplush.dingoobox

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class PausePage(val titleResource: Int) {
    Menu(R.string.pause_menu_page_pause),
    Summary(R.string.pause_menu_page_game_summary),
    AppSettings(R.string.pause_menu_page_app_settings),
    ControllerSettings(R.string.pause_menu_page_controller_settings)
}

/**
 * The header focus target for the page currently hosted by PauseMenu.
 *
 * MiNiQ keeps inactive pages out of composition and starts keyboard/gamepad
 * focus on the close button.  The adapted pages use this local only to return
 * from their top edge to the matching visible page button, so focus cannot
 * escape to a control belonging to another page.
 */
internal val LocalPauseMenuPageFocusRequester =
    compositionLocalOf<FocusRequester?> { null }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PauseMenu(
    game: GameEntry,
    settings: AppSettingsState,
    bindings: InputBindingPreferences,
    gameSpecificEnabled: Boolean,
    openControllerSettingsInitially: Boolean = false,
    onGameSpecificEnabled: (Boolean) -> Unit,
    onSettingsChanged: (AppSettingsState) -> Unit,
    onResume: () -> Unit,
    onLoadState: () -> Unit,
    onSaveState: () -> Unit,
    onToggleFastForward: () -> Unit,
    onExit: () -> Unit,
    onScreenshot: () -> Unit,
    onReset: () -> Unit,
    onRename: (String) -> Unit,
    onEditLayout: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    var page by remember(openControllerSettingsInitially) {
        mutableStateOf(
            if (openControllerSettingsInitially) {
                PausePage.ControllerSettings
            } else {
                PausePage.Menu
            }
        )
    }
    val menuPageFocus = remember { FocusRequester() }
    val summaryPageFocus = remember { FocusRequester() }
    val appSettingsPageFocus = remember { FocusRequester() }
    val controllerSettingsPageFocus = remember { FocusRequester() }
    val closeFocus = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    BackHandler(onBack = onResume)
    LaunchedEffect(Unit) {
        delay(160)
        inputModeManager.requestInputMode(InputMode.Keyboard)
        closeFocus.requestFocus()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { composeEvent ->
                val event = composeEvent.nativeKeyEvent
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                        onResume()
                    }
                    true
                } else {
                    false
                }
            }
            .focusProperties { canFocus = false }
            .clickable(onClick = {}),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().symmetricCutoutPadding()
                .padding(
                    horizontal = if (page == PausePage.AppSettings || page == PausePage.ControllerSettings) 0.dp else 16.dp,
                    vertical = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(
                    horizontal = if (page == PausePage.AppSettings || page == PausePage.ControllerSettings) 16.dp else 0.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (game.coverFile != null) {
                    GameCover(game.coverFile, Modifier.size(48.dp), 6.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        game.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isPortrait) 2 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        appStringResource(page.titleResource),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PausePageButton(
                    Icons.Default.Menu,
                    appStringResource(R.string.pause_menu_page_pause),
                    page == PausePage.Menu,
                    Modifier
                        .focusRequester(menuPageFocus)
                        .focusProperties {
                            left = FocusRequester.Cancel
                            right = summaryPageFocus
                            up = FocusRequester.Cancel
                        }
                ) { page = PausePage.Menu }
                PausePageButton(
                    Icons.Default.Info,
                    appStringResource(R.string.pause_menu_page_game_summary),
                    page == PausePage.Summary,
                    Modifier
                        .focusRequester(summaryPageFocus)
                        .focusProperties {
                            left = menuPageFocus
                            right = appSettingsPageFocus
                            up = FocusRequester.Cancel
                        }
                ) { page = PausePage.Summary }
                PausePageButton(
                    Icons.Default.Settings,
                    appStringResource(R.string.pause_menu_page_app_settings),
                    page == PausePage.AppSettings,
                    Modifier
                        .focusRequester(appSettingsPageFocus)
                        .focusProperties {
                            left = summaryPageFocus
                            right = controllerSettingsPageFocus
                            up = FocusRequester.Cancel
                        }
                ) { page = PausePage.AppSettings }
                PausePageButton(
                    Icons.Default.SportsEsports,
                    appStringResource(R.string.pause_menu_page_controller_settings),
                    page == PausePage.ControllerSettings,
                    Modifier
                        .focusRequester(controllerSettingsPageFocus)
                        .focusProperties {
                            left = appSettingsPageFocus
                            right = closeFocus
                            up = FocusRequester.Cancel
                        }
                ) {
                    page = PausePage.ControllerSettings
                }
                IconButton(
                    onClick = onResume,
                    modifier = Modifier
                        .size(40.dp)
                        .focusRequester(closeFocus)
                        .focusProperties {
                            left = controllerSettingsPageFocus
                            right = FocusRequester.Cancel
                            up = FocusRequester.Cancel
                        }
                ) {
                    Icon(
                        Icons.Default.Close,
                        appStringResource(R.string.pause_menu_resume_game)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            ) {
                val pageFocusRequester = when (page) {
                    PausePage.Menu -> menuPageFocus
                    PausePage.Summary -> summaryPageFocus
                    PausePage.AppSettings -> appSettingsPageFocus
                    PausePage.ControllerSettings -> controllerSettingsPageFocus
                }
                CompositionLocalProvider(
                    LocalPauseMenuPageFocusRequester provides pageFocusRequester
                ) {
                    when (page) {
                        PausePage.Menu -> PauseActions(
                            pageFocusRequester,
                            onLoadState,
                            onSaveState,
                            onToggleFastForward,
                            onExit,
                            onScreenshot,
                            onReset
                        )
                        PausePage.Summary -> GameSummaryScreen(
                            game = game,
                            embedded = true,
                            onRename = onRename
                        )
                        PausePage.AppSettings -> SettingsScreen(
                            settings = settings, inGame = true, embedded = true,
                            onSettingsChanged = onSettingsChanged
                        )
                        PausePage.ControllerSettings -> ControllerSettingsScreen(
                            settings = settings, bindings = bindings, inGame = true, embedded = true,
                            openTouchControlsInitially = openControllerSettingsInitially,
                            gameKey = game.id,
                            gameSpecificEnabled = gameSpecificEnabled,
                            onGameSpecificEnabled = onGameSpecificEnabled,
                            onSettingsChanged = onSettingsChanged, onEditLayout = onEditLayout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PausePageButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        shape = androidx.compose.foundation.shape.CircleShape
    ) {
        IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
            Icon(
                icon,
                label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun PauseActions(
    pageFocusRequester: FocusRequester,
    onLoad: () -> Unit,
    onSave: () -> Unit,
    onToggleFastForward: () -> Unit,
    onExit: () -> Unit,
    onScreenshot: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)
    ) {
        PauseMenuItem(
            appStringResource(R.string.pause_menu_load_state),
            Icons.Default.FolderOpen,
            blockUp = true,
            upFocusRequester = pageFocusRequester,
            onClick = onLoad
        )
        PauseMenuItem(
            appStringResource(R.string.pause_menu_save_state),
            Icons.Default.Save,
            onSave
        )
        PauseMenuItem(
            appStringResource(R.string.pause_menu_toggle_fast_forward),
            Icons.Default.FastForward,
            onToggleFastForward
        )
        PauseMenuItem(
            appStringResource(R.string.pause_menu_exit_game),
            Icons.Default.PowerSettingsNew,
            onExit
        )
        PauseMenuItem(
            appStringResource(R.string.pause_menu_save_screenshot),
            Icons.Default.PhotoCamera,
            onScreenshot
        )
        PauseMenuItem(
            appStringResource(R.string.pause_menu_restart_game),
            Icons.Default.Refresh,
            blockDown = true,
            onClick = onReset
        )
    }
}

@Composable
private fun PauseMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    blockUp: Boolean = false,
    blockDown: Boolean = false,
    upFocusRequester: FocusRequester? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
                if (blockUp) {
                    up = upFocusRequester ?: FocusRequester.Cancel
                }
                if (blockDown) down = FocusRequester.Cancel
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(22.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
