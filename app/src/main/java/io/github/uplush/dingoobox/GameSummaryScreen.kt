package io.github.uplush.dingoobox

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun GameSummaryScreen(
    game: GameEntry,
    embedded: Boolean = false,
    onBack: () -> Unit = {},
    onRename: (String) -> Unit = {}
) {
    if (!embedded) {
        BackHandler(onBack = onBack)
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        GameSummaryContent(game = game, onRename = onRename, modifier = modifier)
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
            MiniQPageHeader(game.title, onBack)
            HorizontalDivider()
            content(Modifier.weight(1f))
        }
    }
}

/** Direct layout port of MiNiQ's GameSummaryContent, adapted to Dingoo's GameEntry. */
@Composable
fun GameSummaryContent(
    game: GameEntry,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var renameDialogVisible by remember(game.id) { mutableStateOf(false) }
    var renameText by remember(game.id) { mutableStateOf(game.title) }
    val pauseMenuPageFocusRequester = LocalPauseMenuPageFocusRequester.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        SummaryField(
            label = appStringResource(R.string.game_summary_game_title),
            value = game.title,
            modifier = Modifier
                .focusProperties {
                    if (pauseMenuPageFocusRequester != null) {
                        up = pauseMenuPageFocusRequester
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                    }
                }
                .clickable {
                    renameText = game.title
                    renameDialogVisible = true
                }
        )
        SummaryField(
            appStringResource(R.string.game_summary_play_time),
            localizedPlayTime(game.playTimeMs)
        )
        SummaryField(
            appStringResource(R.string.game_summary_last_played),
            localizedLastPlayed(game.lastPlayedAt)
        )
        SummaryField(
            appStringResource(R.string.game_summary_game_path),
            formatGamePath(game.uri)
        )
        SummaryField(
            appStringResource(R.string.game_summary_file_size),
            formatFileSize(game.size)
        )
    }

    if (renameDialogVisible) {
        MiniQStandardDialog(onDismissRequest = { renameDialogVisible = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 8.dp)
            ) {
                Text(
                    text = appStringResource(R.string.game_summary_custom_title),
                    modifier = Modifier.padding(horizontal = 28.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    placeholder = {
                        Text(appStringResource(R.string.game_summary_game_title))
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { renameText = "" }) {
                        Text(appStringResource(R.string.game_summary_clear))
                    }
                    Row {
                        TextButton(onClick = { renameDialogVisible = false }) {
                            Text(appStringResource(R.string.game_summary_cancel))
                        }
                        TextButton(onClick = {
                            onRename(renameText)
                            renameDialogVisible = false
                        }) {
                            Text(appStringResource(R.string.game_summary_done))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatGamePath(uri: Uri): String {
    val documentId = runCatching {
        DocumentsContract.getDocumentId(uri)
    }.getOrNull() ?: return uri.toString()

    val separatorIndex = documentId.indexOf(':')
    if (separatorIndex < 0) return documentId

    val storageId = documentId.substring(0, separatorIndex)
    val relativePath = documentId.substring(separatorIndex + 1)
    val storageName = if (storageId.equals("primary", ignoreCase = true)) {
        appStringResource(R.string.game_summary_internal_storage)
    } else {
        storageId
    }

    return if (relativePath.isBlank()) {
        storageName
    } else {
        "$storageName/$relativePath"
    }
}

@Composable
private fun localizedPlayTime(totalPlayTimeMs: Long): String {
    if (totalPlayTimeMs <= 0L) {
        return appStringResource(R.string.game_summary_not_played)
    }

    val totalMinutes = totalPlayTimeMs / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L

    return when {
        hours > 0L && minutes > 0L -> appStringResource(
            R.string.game_summary_time_hours_minutes,
            hours,
            minutes
        )
        hours > 0L -> appStringResource(R.string.game_summary_time_hours, hours)
        minutes > 0L -> appStringResource(R.string.game_summary_time_minutes, minutes)
        else -> appStringResource(R.string.game_summary_time_less_than_minute)
    }
}

@Composable
private fun localizedLastPlayed(lastPlayedAt: Long): String {
    if (lastPlayedAt <= 0L) {
        return appStringResource(R.string.game_summary_never_played)
    }

    val locale = appLocale()
    val pattern = appStringResource(R.string.game_summary_date_pattern)
    return remember(lastPlayedAt, locale, pattern) {
        SimpleDateFormat(pattern, locale).format(Date(lastPlayedAt))
    }
}

@Composable
private fun SummaryField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
