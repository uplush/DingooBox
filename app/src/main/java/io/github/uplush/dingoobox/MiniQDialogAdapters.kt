package io.github.uplush.dingoobox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Dingoo-specific content adapters hosted inside MiNiQ's unmodified dialog surface. */
@Composable
fun MiniQConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val messageScrollState = rememberScrollState()

    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(
                start = 28.dp,
                top = 28.dp,
                end = 28.dp,
                bottom = 16.dp
            )
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(18.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(messageScrollState)
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissLabel != null) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = onConfirm) {
                    Text(confirmLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun MiniQSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    options: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(
                    start = 28.dp,
                    top = 28.dp,
                    end = 28.dp,
                    bottom = 16.dp
                )
            )
            Column(
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(scrollState),
                content = options
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        appStringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
