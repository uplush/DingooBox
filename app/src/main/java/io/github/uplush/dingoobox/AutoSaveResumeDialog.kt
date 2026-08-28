package io.github.uplush.dingoobox

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date

/** MiNiQ's source auto-save resume dialog with Dingoo state callbacks. */
@Composable
fun AutoSaveResumeDialog(
    lastModified: Long,
    previewPath: String?,
    onDismiss: () -> Unit,
    onDeleteState: () -> Boolean,
    onCleanBoot: () -> Unit,
    onLoadState: () -> Unit
) {
    val autoSaveStateAvailable = remember(lastModified) {
        mutableStateOf(true)
    }
    val configuration = LocalConfiguration.current
    val currentAppLocale = appLocale()
    val dateTimePattern = appStringResource(R.string.auto_save_resume_date_pattern)
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val previewWidth = if (isLandscape) 240.dp else 210.dp

    val previewBitmap = remember(previewPath, lastModified) {
        previewPath?.let { path ->
            BitmapFactory.decodeFile(path)
        }
    }

    val formattedTime = remember(lastModified, currentAppLocale, dateTimePattern) {
        SimpleDateFormat(dateTimePattern, currentAppLocale).format(Date(lastModified))
    }

    val loadButtonFocusRequester = remember { FocusRequester() }

    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = appStringResource(R.string.auto_save_resume_prompt),
                modifier = Modifier.padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = 28.dp,
                    bottom = 16.dp
                ),
                style = MaterialTheme.typography.headlineSmall
            )

            Box(
                modifier = Modifier
                    .width(previewWidth)
                    .aspectRatio(1.5f)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = appStringResource(
                            R.string.auto_save_resume_preview_description
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None
                    )
                } else {
                    Text(
                        text = appStringResource(R.string.auto_save_resume_no_preview),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appStringResource(R.string.auto_save_resume_time, formattedTime),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (onDeleteState()) {
                            autoSaveStateAvailable.value = false
                        }
                    },
                    enabled = autoSaveStateAvailable.value
                ) {
                    Text(
                        text = appStringResource(R.string.auto_save_resume_delete),
                        color = if (autoSaveStateAvailable.value) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onCleanBoot) {
                    Text(
                        text = appStringResource(R.string.auto_save_resume_clean_boot),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                TextButton(
                    onClick = onLoadState,
                    enabled = autoSaveStateAvailable.value,
                    modifier = Modifier.focusRequester(loadButtonFocusRequester)
                ) {
                    Text(
                        text = appStringResource(R.string.auto_save_resume_load),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadButtonFocusRequester.requestFocus()
    }
}
