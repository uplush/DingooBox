package io.github.uplush.dingoobox

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MiniQStandardDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val configuration =
        LocalConfiguration.current

    val isLandscape =
        configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE

    val widthFraction =
        if (isLandscape) {
            0.78f
        } else {
            0.84f
        }

    val dialogWidth =
        (
            configuration.screenWidthDp *
                widthFraction
        ).dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(onDismissRequest) {
                        detectTapGestures {
                            onDismissRequest()
                        }
                    }
                    .symmetricSafeDrawingPadding()
                    .padding(
                        horizontal = 16.dp
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Surface(
                modifier =
                    Modifier
                        .width(dialogWidth)
                        .heightIn(
                            max = maxHeight
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {}
                            )
                        },
                color =
                    MaterialTheme.colorScheme.surface,
                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(28.dp),
                tonalElevation = 6.dp
            ) {
                content()
            }
        }
    }
}
