package io.github.uplush.dingoobox

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun GameCover(file: File?, modifier: Modifier = Modifier, cornerRadius: Dp = 12.dp) {
    val bitmap = remember(file?.absolutePath, file?.lastModified()) {
        file?.takeIf(File::exists)?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = appStringResource(R.string.game_cover_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.SportsEsports, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize(0.42f)
            )
        }
    }
}
