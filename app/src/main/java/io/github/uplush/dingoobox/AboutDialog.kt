package io.github.uplush.dingoobox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** MiNiQ AboutDialog layout, with DingooEmu-specific text substituted. */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val unknownVersionName = appStringResource(R.string.about_unknown_version)
    val versionName = remember(context, unknownVersionName) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: unknownVersionName
    }

    MiniQStandardDialog(onDismissRequest = onDismiss) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, top = 28.dp, end = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = appStringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = appStringResource(R.string.about_version, versionName),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = appStringResource(R.string.about_tagline),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        appStringResource(R.string.about_close)
                    )
                }
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AboutSectionTitle(appStringResource(R.string.about_section_project))
                Text(appStringResource(R.string.about_project_description))
                HorizontalDivider()
                AboutSectionTitle(appStringResource(R.string.about_section_open_source))
                Text(appStringResource(R.string.about_dingooemu_author))
                Text(appStringResource(R.string.about_dingooemu_permission))
                Text(
                    text = appStringResource(
                        R.string.about_dingooemu_original_project,
                        DINGOOEMU_PROJECT_URL
                    ),
                    modifier = Modifier.clickable {
                        runCatching {
                            uriHandler.openUri(DINGOOEMU_PROJECT_URL)
                        }
                    },
                    color = MaterialTheme.colorScheme.primary
                )
                Text("DingooEmu / libretro core\nBSD 3-Clause License")
                Text("AndroidX / Jetpack Compose / Material 3")
                Text("Google Material Icons\nApache License 2.0")
                HorizontalDivider()
                AboutSectionTitle(appStringResource(R.string.about_section_acknowledgments))
                Text(appStringResource(R.string.about_thanks_justburn))
                Text(appStringResource(R.string.about_thanks_libretro))
                Text(appStringResource(R.string.about_thanks_duckstation))
                Text(appStringResource(R.string.about_thanks_android))
                HorizontalDivider()
                AboutSectionTitle(appStringResource(R.string.about_section_legal))
                Text(appStringResource(R.string.about_rights_statement))
                Text(appStringResource(R.string.about_unofficial_statement))
                Text(appStringResource(R.string.about_no_roms))
            }
        }
    }
}

private const val DINGOOEMU_PROJECT_URL =
    "https://github.com/AloysHF/DingooEmu"

@Composable
private fun AboutSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}
