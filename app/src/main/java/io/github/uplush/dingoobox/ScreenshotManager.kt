package io.github.uplush.dingoobox

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screenshot persistence matching MiNiQ's ScreenshotManager behavior.
 */
class ScreenshotManager(
    private val context: Context
) {
    fun save(bitmap: Bitmap): Boolean {
        val fileName =
            "DingooBox_${
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss_SSS",
                    Locale.US
                ).format(Date())
            }.png"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(bitmap, fileName)
        } else {
            saveToLegacyStorage(bitmap, fileName)
        }
    }

    fun saveToUri(bitmap: Bitmap, uri: Uri): Boolean =
        runCatching {
            context.contentResolver
                .openOutputStream(uri, "w")
                ?.use { outputStream ->
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        outputStream
                    )
                } ?: false
        }.getOrDefault(false)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(
        bitmap: Bitmap,
        fileName: String
    ): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/DingooBox"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return false

        return try {
            val saved = resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    outputStream
                )
            } ?: false

            if (saved) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                resolver.delete(uri, null, null)
            }
            saved
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    private fun saveToLegacyStorage(
        bitmap: Bitmap,
        fileName: String
    ): Boolean {
        return runCatching {
            val picturesDirectory =
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: return false
            val screenshotDirectory =
                File(picturesDirectory, "DingooBox").apply { mkdirs() }
            val screenshotFile = File(screenshotDirectory, fileName)
            val saved = FileOutputStream(screenshotFile).use { outputStream ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    outputStream
                )
            }
            if (saved) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(screenshotFile.absolutePath),
                    arrayOf("image/png"),
                    null
                )
            }
            saved
        }.getOrDefault(false)
    }
}
