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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

enum class SaveStateScreenMode {
    SAVE,
    LOAD
}

/**
 * MiNiQ's source layout for both in-game save and load flows.
 * Dingoo state files and native callbacks are supplied by the caller.
 */
@Composable
fun SaveStateScreen(
    slots: List<SaveStateSlotInfo>,
    mode: SaveStateScreenMode = SaveStateScreenMode.SAVE,
    onSave: (SaveStateSlot) -> Unit,
    onLoad: (SaveStateSlot) -> Unit,
    onDelete: (SaveStateSlot) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageFocusRequester = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    val lastFocusableSlotIndex = remember(slots, mode) {
        slots.indexOfLast { info ->
            mode == SaveStateScreenMode.SAVE || info.exists
        }
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(mode) {
        delay(160)
        inputModeManager.requestInputMode(InputMode.Keyboard)
        pageFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
            )
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
                text = when (mode) {
                    SaveStateScreenMode.SAVE ->
                        appStringResource(R.string.save_state_select_save_slot)

                    SaveStateScreenMode.LOAD ->
                        appStringResource(R.string.save_state_select_load_slot)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .symmetricCutoutPadding(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = slots,
                key = { _, info -> info.slot.name }
            ) { index, info ->
                SaveStateSlotItem(
                    info = info,
                    mode = mode,
                    isLastFocusableSlot = index == lastFocusableSlotIndex,
                    onSave = { onSave(info.slot) },
                    onLoad = { onLoad(info.slot) },
                    onDelete = { onDelete(info.slot) }
                )
            }
        }
    }
}

@Composable
private fun SaveStateSlotItem(
    info: SaveStateSlotInfo,
    mode: SaveStateScreenMode,
    isLastFocusableSlot: Boolean,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryActionFocusRequester = remember { FocusRequester() }
    val previewBitmap = remember(info.previewPath, info.lastModified) {
        info.previewPath?.let { path ->
            BitmapFactory.decodeFile(path)
        }
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
                            R.string.save_state_preview_description,
                            appStringResource(info.slot.displayNameResource)
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
                    text = appStringResource(info.slot.displayNameResource),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (info.exists) {
                        appStringResource(R.string.save_state_has_state)
                    } else {
                        appStringResource(R.string.save_state_empty_slot)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (info.exists) {
                        formatStateTime(info.lastModified)
                    } else {
                        when (mode) {
                            SaveStateScreenMode.SAVE ->
                                appStringResource(R.string.save_state_available_to_save)

                            SaveStateScreenMode.LOAD ->
                                appStringResource(R.string.save_state_no_state)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = {
                        when (mode) {
                            SaveStateScreenMode.SAVE -> onSave()
                            SaveStateScreenMode.LOAD -> onLoad()
                        }
                    },
                    enabled = mode == SaveStateScreenMode.SAVE || info.exists,
                    modifier = Modifier
                        .focusRequester(primaryActionFocusRequester)
                        .focusProperties {
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                            if (isLastFocusableSlot && !info.exists) {
                                down = FocusRequester.Cancel
                            }
                        }
                ) {
                    Text(
                        when (mode) {
                            SaveStateScreenMode.SAVE ->
                                appStringResource(R.string.save_state_save)

                            SaveStateScreenMode.LOAD ->
                                appStringResource(R.string.save_state_load)
                        }
                    )
                }

                if (info.exists) {
                    TextButton(
                        onClick = {
                            onDelete()
                            primaryActionFocusRequester.requestFocus()
                        },
                        modifier = Modifier.focusProperties {
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                            up = primaryActionFocusRequester
                            if (isLastFocusableSlot) {
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
}

@Composable
private fun formatStateTime(timestamp: Long?): String {
    val currentAppLocale = appLocale()

    if (timestamp == null) {
        return appStringResource(R.string.save_state_no_state)
    }

    return remember(timestamp, currentAppLocale) {
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            currentAppLocale
        ).format(Date(timestamp))
    }
}
