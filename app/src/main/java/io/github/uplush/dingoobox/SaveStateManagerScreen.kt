package io.github.uplush.dingoobox

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** MiNiQ's shared manager used by both the drawer and a game's load action. */
@Composable
fun SaveStateManagerScreen(
    states: List<ManagedSaveStateInfo>,
    onBack: () -> Unit,
    onLoad: (ManagedSaveStateInfo) -> Unit,
    onDelete: (ManagedSaveStateInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageFocusRequester = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    var pageFocusRevision by remember { mutableIntStateOf(0) }
    var pendingDeleteState by remember {
        mutableStateOf<ManagedSaveStateInfo?>(null)
    }

    fun closeDeleteDialog() {
        pendingDeleteState = null
        pageFocusRevision++
    }

    LaunchedEffect(pageFocusRevision) {
        delay(160)
        inputModeManager.requestInputMode(InputMode.Keyboard)
        pageFocusRequester.requestFocus()
    }

    BackHandler {
        if (pendingDeleteState != null) {
            closeDeleteDialog()
        } else {
            onBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .symmetricCutoutPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .focusRequester(pageFocusRequester)
                    .focusProperties {
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                        up = FocusRequester.Cancel
                    }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = appStringResource(R.string.save_state_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = appStringResource(R.string.save_state_manager_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        HorizontalDivider()

        if (states.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appStringResource(R.string.save_state_manager_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .symmetricCutoutPadding(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = states,
                    key = { _, state -> "${state.game.id}_${state.slot.name}" }
                ) { index, state ->
                    ManagedSaveStateRow(
                        state = state,
                        isLastState = index == states.lastIndex,
                        onLoad = { onLoad(state) },
                        onDelete = { pendingDeleteState = state }
                    )
                }
            }
        }
    }

    pendingDeleteState?.let { state ->
        val dismissFocusRequester = remember(state.game.id, state.slot) {
            FocusRequester()
        }
        val confirmFocusRequester = remember(state.game.id, state.slot) {
            FocusRequester()
        }

        LaunchedEffect(state.game.id, state.slot) {
            delay(100)
            inputModeManager.requestInputMode(InputMode.Keyboard)
            dismissFocusRequester.requestFocus()
        }

        MiniQStandardDialog(
            onDismissRequest = ::closeDeleteDialog
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 28.dp,
                        top = 28.dp,
                        end = 28.dp,
                        bottom = 16.dp
                    )
            ) {
                Text(
                    text = appStringResource(R.string.save_state_confirm_delete_title),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = appStringResource(R.string.save_state_confirm_delete_message),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = ::closeDeleteDialog,
                        modifier = Modifier
                            .focusRequester(dismissFocusRequester)
                            .focusProperties {
                                left = FocusRequester.Cancel
                                right = confirmFocusRequester
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            }
                    ) {
                        Text(appStringResource(R.string.save_state_no))
                    }

                    TextButton(
                        onClick = {
                            onDelete(state)
                            closeDeleteDialog()
                        },
                        modifier = Modifier
                            .focusRequester(confirmFocusRequester)
                            .focusProperties {
                                left = dismissFocusRequester
                                right = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            }
                    ) {
                        Text(appStringResource(R.string.save_state_yes))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagedSaveStateRow(
    state: ManagedSaveStateInfo,
    isLastState: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val loadFocusRequester = remember { FocusRequester() }
    val previewBitmap = remember(state.previewPath, state.lastModified) {
        state.previewPath?.let { path ->
            BitmapFactory.decodeFile(path)
        }
    }

    val currentAppLocale = appLocale()
    val timeText = remember(state.lastModified, currentAppLocale) {
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            currentAppLocale
        ).format(Date(state.lastModified))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .aspectRatio(1.5f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = appStringResource(
                            R.string.save_state_screenshot_description
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None
                    )
                } else {
                    Text(
                        text = appStringResource(R.string.save_state_no_preview),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (state.game.title.isBlank()) {
                        appStringResource(R.string.save_state_unknown_game)
                    } else {
                        state.game.title
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = appStringResource(
                        R.string.save_state_slot_and_size,
                        appStringResource(state.slot.displayNameResource),
                        formatStateSize(state.stateSizeBytes, currentAppLocale)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = onLoad,
                    modifier = Modifier
                        .focusRequester(loadFocusRequester)
                        .focusProperties {
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                        }
                ) {
                    Text(appStringResource(R.string.save_state_load))
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.focusProperties {
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                        up = loadFocusRequester
                        if (isLastState) {
                            down = FocusRequester.Cancel
                        }
                    }
                ) {
                    Text(
                        text = appStringResource(R.string.save_state_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatStateSize(bytes: Long, locale: Locale): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format(
        locale,
        "%.1f MB",
        bytes.toDouble() / (1024.0 * 1024.0)
    )
}
