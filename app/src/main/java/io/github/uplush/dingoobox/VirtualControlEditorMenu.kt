package io.github.uplush.dingoobox

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class VirtualControlEditorDialog {
    Menu,
    Opacity,
    Visibility
}

@Composable
fun VirtualControlEditorMenu(
    opacity: Float,
    visibleControls: Set<ControlId>,
    onOpacityChange: (Float) -> Unit,
    onVisibleControlsChange: (Set<ControlId>) -> Unit,
    onResizeModeChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeDialog by remember { mutableStateOf<VirtualControlEditorDialog?>(null) }

    Button(
        onClick = { activeDialog = VirtualControlEditorDialog.Menu },
        modifier = modifier
            .widthIn(min = 160.dp)
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
    ) {
        Text(
            text = appStringResource(R.string.virtual_control_editor_menu),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }

    when (activeDialog) {
        VirtualControlEditorDialog.Menu -> VirtualControlDialog(
            onDismissRequest = { activeDialog = null }
        ) {
            VirtualControlMenuOption(
                text = appStringResource(R.string.virtual_control_adjust_position),
                onClick = {
                    activeDialog = null
                    onResizeModeChange(false)
                }
            )
            VirtualControlMenuOption(
                text = appStringResource(R.string.virtual_control_adjust_opacity),
                onClick = { activeDialog = VirtualControlEditorDialog.Opacity }
            )
            VirtualControlMenuOption(
                text = appStringResource(R.string.virtual_control_add_remove),
                onClick = { activeDialog = VirtualControlEditorDialog.Visibility }
            )
            VirtualControlMenuOption(
                text = appStringResource(R.string.virtual_control_adjust_size),
                onClick = {
                    activeDialog = null
                    onResizeModeChange(true)
                }
            )
            VirtualControlMenuOption(
                text = appStringResource(R.string.virtual_control_reset_layout),
                onClick = {
                    activeDialog = null
                    onReset()
                }
            )
            VirtualControlMenuOption(
                text = appStringResource(R.string.virtual_control_exit_editing),
                onClick = {
                    activeDialog = null
                    onExit()
                }
            )
        }

        VirtualControlEditorDialog.Opacity -> VirtualControlDialog(
            onDismissRequest = { activeDialog = null }
        ) {
            VirtualControlDialogTitle(
                text = appStringResource(R.string.virtual_control_opacity_title)
            )
            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 8.dp)
            )
            TextButton(
                onClick = { activeDialog = null },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, top = 8.dp)
            ) {
                Text(appStringResource(R.string.virtual_control_done))
            }
        }

        VirtualControlEditorDialog.Visibility -> EditorVirtualControlSelectionDialog(
            visibleControls = visibleControls,
            onDismiss = { activeDialog = null },
            onConfirm = {
                onVisibleControlsChange(it)
                activeDialog = null
            }
        )

        null -> Unit
    }
}

@Composable
private fun VirtualControlDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    MiniQStandardDialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 18.dp),
            content = content
        )
    }
}

@Composable
private fun VirtualControlDialogTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = 28.dp,
            end = 28.dp,
            top = 8.dp,
            bottom = 14.dp
        ),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun VirtualControlMenuOption(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun EditorVirtualControlSelectionDialog(
    visibleControls: Set<ControlId>,
    onDismiss: () -> Unit,
    onConfirm: (Set<ControlId>) -> Unit
) {
    var pendingVisibleControls by remember(visibleControls) {
        mutableStateOf(visibleControls)
    }
    val optionHeight = if (
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) 48.dp else 52.dp

    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = appStringResource(R.string.virtual_control_add_remove),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = 28.dp,
                    bottom = 16.dp
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                ControlId.entries.forEach { id ->
                    val checked = id in pendingVisibleControls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pendingVisibleControls = if (checked) {
                                    pendingVisibleControls - id
                                } else {
                                    pendingVisibleControls + id
                                }
                            }
                            .height(optionHeight)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { selected ->
                                pendingVisibleControls = if (selected) {
                                    pendingVisibleControls + id
                                } else {
                                    pendingVisibleControls - id
                                }
                            }
                        )
                        Text(
                            text = editorControlDisplayName(id),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(appStringResource(R.string.virtual_control_cancel))
                }
                TextButton(onClick = { onConfirm(pendingVisibleControls) }) {
                    Text(appStringResource(R.string.virtual_control_confirm))
                }
            }
        }
    }
}

@Composable
private fun editorControlDisplayName(id: ControlId): String = appStringResource(
    when (id) {
        ControlId.DPad -> R.string.virtual_control_d_pad
        ControlId.A -> R.string.controller_action_a
        ControlId.B -> R.string.controller_action_b
        ControlId.X -> R.string.controller_action_x
        ControlId.Y -> R.string.controller_action_y
        ControlId.L -> R.string.controller_action_l
        ControlId.R -> R.string.controller_action_r
        ControlId.Start -> R.string.controller_action_start
        ControlId.Select -> R.string.controller_action_select
    }
)
