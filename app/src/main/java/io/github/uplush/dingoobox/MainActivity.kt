package io.github.uplush.dingoobox

import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.uplush.dingoobox.ui.theme.DingooTheme

class MainActivity : ComponentActivity() {
    private var themeMode by mutableStateOf(AppThemeMode.SYSTEM)
    private var appLanguage by mutableStateOf(AppLanguage.SYSTEM)
    private var pendingShortcutLaunch by mutableStateOf<ShortcutLaunchRequest?>(null)
    private var shortcutLaunchSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Match MiNiQ's edge-to-edge window contract. Without the cutout mode
        // the platform offsets a full-size Compose Dialog on landscape phones,
        // while the dialog content still measures against the full display.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val preferences = AppPreferences(this)
        val settings = preferences.load()
        themeMode = settings.themeMode
        appLanguage = settings.language
        requestedOrientation = settings.orientation.requestedOrientation
        handleShortcutIntent(intent)

        setContent {
            AppLanguageProvider(appLanguage) {
                DingooTheme(themeMode) {
                    DingooApp(
                        shortcutLaunchRequest = pendingShortcutLaunch,
                        onShortcutLaunchConsumed = {
                            pendingShortcutLaunch = null
                        },
                        onThemeChanged = { mode -> themeMode = mode },
                        onLanguageChanged = { language -> appLanguage = language }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(sourceIntent: Intent?) {
        sourceIntent ?: return

        val externalPath = EXTERNAL_GAME_PATH_EXTRAS
            .firstNotNullOfOrNull { key ->
                sourceIntent.getStringExtra(key)?.takeIf(String::isNotBlank)
            }
        if (sourceIntent.action != Intent.ACTION_VIEW && externalPath == null) return

        val shortcutUri = sourceIntent.data
        val legacyShortcutUri = shortcutUri?.scheme == SHORTCUT_SCHEME &&
            shortcutUri.host == SHORTCUT_GAME_AUTHORITY
        val gameUri = shortcutUri?.takeUnless { legacyShortcutUri }
            ?: externalPath?.toGameUri()
        val gameId = when {
            legacyShortcutUri ->
                shortcutUri?.lastPathSegment
            else -> sourceIntent.getStringExtra(EXTRA_GAME_ID)
        }
        val legacyGamePath = sourceIntent.getStringExtra(EXTRA_GAME_PATH)

        // Consume the shortcut payload once. Keeping ACTION_VIEW on the
        // activity Intent would replay the same launch after recreation.
        sourceIntent.action = null
        sourceIntent.data = null
        sourceIntent.removeExtra(EXTRA_GAME_ID)
        sourceIntent.removeExtra(EXTRA_GAME_PATH)
        EXTERNAL_GAME_PATH_EXTRAS.forEach(sourceIntent::removeExtra)

        if (
            gameUri == null &&
            gameId.isNullOrBlank() &&
            legacyGamePath.isNullOrBlank()
        ) return

        shortcutLaunchSequence += 1L
        pendingShortcutLaunch = ShortcutLaunchRequest(
            gameUri = gameUri,
            gameId = gameId,
            legacyGamePath = legacyGamePath,
            sequence = shortcutLaunchSequence
        )
    }

    companion object {
        const val EXTRA_GAME_PATH = "game_path"
        const val EXTRA_GAME_ID = "game_id"
        const val SHORTCUT_SCHEME = "dingooemu"
        const val SHORTCUT_GAME_AUTHORITY = "game"

        private val EXTERNAL_GAME_PATH_EXTRAS = listOf(
            "ROM",
            "rom",
            "path",
            EXTRA_GAME_PATH
        )
    }
}

private fun String.toGameUri(): Uri {
    val parsed = Uri.parse(this)
    return if (parsed.scheme.isNullOrBlank()) Uri.fromFile(java.io.File(this)) else parsed
}

data class ShortcutLaunchRequest(
    val gameUri: Uri?,
    val gameId: String?,
    val legacyGamePath: String?,
    val sequence: Long
)
