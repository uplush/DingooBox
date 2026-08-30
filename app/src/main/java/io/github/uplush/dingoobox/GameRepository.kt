package io.github.uplush.dingoobox

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.edit
import java.io.File
import java.security.MessageDigest

data class GameEntry(
    val id: String,
    val title: String,
    val uri: Uri,
    val fileName: String,
    val sourceLocation: String,
    val size: Long,
    val modifiedAt: Long,
    val lastPlayedAt: Long = 0L,
    val playTimeMs: Long = 0L,
    val coverFile: File? = null
)

enum class GameSortMode {
    Title,
    LastPlayed,
    PlayTime,
    Size
}

enum class SaveStateSlot(
    val filePart: String,
    val displayNameResource: Int
) {
    Auto("auto", R.string.save_state_slot_auto),
    Quick("quick", R.string.save_state_slot_quick),
    Slot1("slot1", R.string.save_state_slot_1),
    Slot2("slot2", R.string.save_state_slot_2),
    Slot3("slot3", R.string.save_state_slot_3),
    Slot4("slot4", R.string.save_state_slot_4),
    Slot5("slot5", R.string.save_state_slot_5)
}

data class SaveStateSlotInfo(
    val slot: SaveStateSlot,
    val exists: Boolean,
    val lastModified: Long?,
    val previewPath: String? = null
)

data class ManagedSaveStateInfo(
    val game: GameEntry,
    val slot: SaveStateSlot,
    val lastModified: Long,
    val stateSizeBytes: Long,
    val previewPath: String?
)

class GameRepository(private val context: Context) {
    private val contentResolver = context.contentResolver
    private val userDirectory = DingooUserDirectory(context).also {
        check(it.ensureCreated()) { "Unable to create the DingooBox user data directory" }
    }
    private val statesDirectory = userDirectory.saveStatesDirectory
    private val coversDirectory = userDirectory.coversDirectory
    private val metadata = context.getSharedPreferences("game_library", Context.MODE_PRIVATE)
    private val saveStateMetadata =
        DingooPreferenceRepository(context, userDirectory).settings("save_state_metadata")
    private val saveStateKeys = mutableMapOf<String, String>()
    val savesDirectory = userDirectory.savesDirectory

    init {
        migrateMissingFiles(File(context.filesDir, "saves"), savesDirectory)
        migrateMissingFiles(File(context.filesDir, "covers"), coversDirectory)
    }

    fun savedDirectory(): Uri? = metadata
        .getString(GAME_DIRECTORY_KEY, null)
        ?.let(Uri::parse)

    fun setDirectory(uri: Uri): Result<Int> = runCatching {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        metadata.edit { putString(GAME_DIRECTORY_KEY, uri.toString()) }
        scanDirectoryTree(uri).size
    }

    fun games(sort: GameSortMode = GameSortMode.Title): List<GameEntry> {
        val entries = savedDirectory()
            ?.let { treeUri ->
                runCatching { scanDirectoryTree(treeUri) }
                    .getOrDefault(emptyList())
            }
            .orEmpty()
        return when (sort) {
            GameSortMode.Title -> entries.sortedBy { it.title.lowercase() }
            GameSortMode.LastPlayed -> entries.sortedWith(compareByDescending<GameEntry> { it.lastPlayedAt }.thenBy { it.title.lowercase() })
            GameSortMode.PlayTime -> entries.sortedWith(compareByDescending<GameEntry> { it.playTimeMs }.thenBy { it.title.lowercase() })
            GameSortMode.Size -> entries.sortedWith(compareByDescending<GameEntry> { it.size }.thenBy { it.title.lowercase() })
        }
    }

    fun game(uri: Uri): Result<GameEntry> = runCatching {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        queryGame(uri)
            ?: error(context.getString(R.string.main_read_game_file_failed))
    }

    fun readGame(game: GameEntry): Result<ByteArray> = runCatching {
        contentResolver.openInputStream(game.uri).use { input ->
            requireNotNull(input) {
                context.getString(
                    R.string.main_read_named_game_file_failed,
                    game.fileName
                )
            }
            input.readBytes()
        }
    }

    fun rename(game: GameEntry, title: String) {
        metadata.edit {
            val normalized = title.trim()
            if (normalized.isEmpty()) remove("title_${game.id}")
            else putString("title_${game.id}", normalized)
        }
    }

    fun setCover(game: GameEntry, uri: Uri): Result<Unit> = runCatching {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val extension = when { mime.contains("png") -> "png"; mime.contains("webp") -> "webp"; else -> "jpg" }
        val imageData = context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { context.getString(R.string.main_read_cover_failed) }
            input.readBytes()
        }
        val coverId = gameCoverId(game.uri)
        val contentId = sha256Hex(imageData).take(COVER_CONTENT_HASH_LENGTH)
        deleteManagedCovers(game, keepFile = null)
        val destination = File(coversDirectory, "${coverId}_${contentId}.$extension")
        destination.writeBytes(imageData)
    }

    fun setDownloadedCover(game: GameEntry, imageData: ByteArray): Boolean {
        if (imageData.isEmpty() || imageData.size > MAX_DOWNLOADED_COVER_SIZE_BYTES) {
            return false
        }

        val coverId = gameCoverId(game.uri)
        val contentId = sha256Hex(imageData).take(COVER_CONTENT_HASH_LENGTH)
        val temporaryFile = File(
            coversDirectory,
            ".$coverId.${System.nanoTime()}.cover-download"
        )
        val destination = File(coversDirectory, "${coverId}_${contentId}.png")
        val backup = File(coversDirectory, ".$coverId.cover-backup")

        return try {
            temporaryFile.outputStream().buffered().use { output ->
                output.write(imageData)
            }
            if (!isValidCoverFile(temporaryFile)) {
                false
            } else {
                val existingCovers = managedCovers(game)

                backup.delete()
                val destinationBackedUp = !destination.exists() || destination.renameTo(backup)
                if (!destinationBackedUp) {
                    false
                } else if (!temporaryFile.renameTo(destination)) {
                    if (backup.isFile) backup.renameTo(destination)
                    false
                } else {
                    backup.delete()
                    existingCovers
                        .filter { it != destination }
                        .forEach(File::delete)
                    true
                }
            }
        } catch (_: Exception) {
            false
        } finally {
            temporaryFile.delete()
        }
    }

    fun recordStarted(game: GameEntry) = metadata.edit { putLong("last_${game.id}", System.currentTimeMillis()) }
    fun addPlayTime(game: GameEntry, elapsedMs: Long) = metadata.edit {
        putLong("time_${game.id}", metadata.getLong("time_${game.id}", 0L) + elapsedMs.coerceAtLeast(0L))
    }

    fun saveStateSlots(game: GameEntry): List<SaveStateSlotInfo> =
        SaveStateSlot.entries
            .filter { slot -> slot != SaveStateSlot.Auto }
            .map { slot ->
                val stateFile = stateFile(game, slot)
                val previewFile = previewFile(game, slot)
                SaveStateSlotInfo(
                    slot = slot,
                    exists = stateFile.isFile,
                    lastModified = stateFile.takeIf { it.isFile }?.lastModified(),
                    previewPath = previewFile
                        .takeIf { stateFile.isFile && it.isFile }
                        ?.absolutePath
                )
            }

    fun managedSaveStates(
        games: List<GameEntry>,
        gameId: String? = null
    ): List<ManagedSaveStateInfo> = games
        .asSequence()
        .filter { game -> gameId == null || game.id == gameId }
        .flatMap { game ->
            saveStateSlots(game)
                .asSequence()
                .filter(SaveStateSlotInfo::exists)
                .map { info ->
                    val stateFile = stateFile(game, info.slot)
                    ManagedSaveStateInfo(
                        game = game,
                        slot = info.slot,
                        lastModified = info.lastModified ?: stateFile.lastModified(),
                        stateSizeBytes = stateFile.length(),
                        previewPath = info.previewPath
                    )
                }
        }
        .sortedByDescending(ManagedSaveStateInfo::lastModified)
        .toList()

    fun stateFile(game: GameEntry, slot: SaveStateSlot): File =
        File(saveStateGameDirectory(game), "${slot.filePart}.state")
    fun previewFile(game: GameEntry, slot: SaveStateSlot): File =
        File(saveStateGameDirectory(game), "${slot.filePart}.png")
    fun deleteState(game: GameEntry, slot: SaveStateSlot): Boolean {
        val stateFile = stateFile(game, slot)
        val previewFile = previewFile(game, slot)
        val stateDeleted = !stateFile.exists() || stateFile.delete()
        val previewDeleted = !previewFile.exists() || previewFile.delete()
        return stateDeleted && previewDeleted
    }
    fun quickState(game: GameEntry): File = stateFile(game, SaveStateSlot.Quick)

    private fun findCover(id: String, uri: Uri? = null): File? {
        val prefix = uri?.let { "${gameCoverId(it)}_" }
        return coversDirectory.listFiles { file ->
            file.isFile &&
                (file.nameWithoutExtension == id || (prefix != null && file.name.startsWith(prefix)))
        }.orEmpty().firstOrNull()
    }

    private fun managedCovers(game: GameEntry): List<File> {
        val prefix = "${gameCoverId(game.uri)}_"
        return coversDirectory.listFiles { file ->
            file.isFile &&
                (file.nameWithoutExtension == game.id || file.name.startsWith(prefix))
        }.orEmpty().toList()
    }

    private fun deleteManagedCovers(game: GameEntry, keepFile: File?) {
        managedCovers(game).filter { it != keepFile }.forEach(File::delete)
    }
    private fun isValidCoverFile(file: File): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        return runCatching {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 &&
                options.outHeight > 0 &&
                options.outMimeType?.startsWith("image/") == true
        }.getOrDefault(false)
    }
    private fun scanDirectoryTree(treeUri: Uri): List<GameEntry> {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val result = mutableListOf<GameEntry>()
        scanDirectory(
            treeUri = treeUri,
            parentDocumentId = rootDocumentId,
            relativeDirectory = "",
            result = result,
            visitedDirectories = mutableSetOf()
        )
        return result
    }

    private fun scanDirectory(
        treeUri: Uri,
        parentDocumentId: String,
        relativeDirectory: String,
        result: MutableList<GameEntry>,
        visitedDirectories: MutableSet<String>
    ) {
        if (!visitedDirectories.add(parentDocumentId)) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            parentDocumentId
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            )
            val nameIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
            val typeIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            val sizeIndex = cursor.getColumnIndex(
                DocumentsContract.Document.COLUMN_SIZE
            )
            val modifiedIndex = cursor.getColumnIndex(
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(idIndex) ?: continue
                val name = cursor.getString(nameIndex) ?: continue
                val mimeType = cursor.getString(typeIndex)
                val relativePath = if (relativeDirectory.isEmpty()) {
                    name
                } else {
                    "$relativeDirectory/$name"
                }

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    scanDirectory(
                        treeUri = treeUri,
                        parentDocumentId = documentId,
                        relativeDirectory = relativePath,
                        result = result,
                        visitedDirectories = visitedDirectories
                    )
                } else if (name.endsWith(".app", ignoreCase = true)) {
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        documentId
                    )
                    result += createGameEntry(
                        uri = documentUri,
                        fileName = name,
                        sourceLocation = relativePath,
                        size = cursor.longOrZero(sizeIndex),
                        modifiedAt = cursor.longOrZero(modifiedIndex)
                    )
                }
            }
        }
    }

    private fun queryGame(uri: Uri): GameEntry? {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val file = uri.path?.let(::File) ?: return null
            if (!file.isFile) return null
            if (!file.name.endsWith(".app", ignoreCase = true)) {
                error(context.getString(R.string.main_select_min_game_file))
            }
            return createGameEntry(
                uri = uri,
                fileName = file.name,
                sourceLocation = file.absolutePath,
                size = file.length(),
                modifiedAt = file.lastModified()
            )
        }

        val projection = arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val name = nameIndex
                .takeIf { it >= 0 && !cursor.isNull(it) }
                ?.let(cursor::getString)
                ?: uri.lastPathSegment
                ?: return@use null
            if (!name.endsWith(".app", ignoreCase = true)) {
                error(context.getString(R.string.main_select_min_game_file))
            }
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val modifiedIndex = cursor.getColumnIndex(
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            createGameEntry(
                uri = uri,
                fileName = name,
                sourceLocation = documentLocation(uri),
                size = cursor.longOrZero(sizeIndex),
                modifiedAt = cursor.longOrZero(modifiedIndex)
            )
        }
    }

    private fun createGameEntry(
        uri: Uri,
        fileName: String,
        sourceLocation: String,
        size: Long,
        modifiedAt: Long
    ): GameEntry {
        val id = gameId(uri)
        val defaultTitle = fileName.substringBeforeLast('.', fileName)
        return GameEntry(
            id = id,
            title = metadata.getString("title_$id", defaultTitle) ?: defaultTitle,
            uri = uri,
            fileName = fileName,
            sourceLocation = sourceLocation,
            size = size,
            modifiedAt = modifiedAt,
            lastPlayedAt = metadata.getLong("last_$id", 0L),
            playTimeMs = metadata.getLong("time_$id", 0L),
            coverFile = findCover(id, uri)
        )
    }

    private fun documentLocation(uri: Uri): String = runCatching {
        DocumentsContract.getDocumentId(uri)
    }.getOrNull().orEmpty().ifBlank { uri.toString() }

    private fun android.database.Cursor.longOrZero(index: Int): Long =
        if (index >= 0 && !isNull(index)) getLong(index) else 0L

    private fun gameId(uri: Uri): String {
        val identity = runCatching {
            "${uri.authority}:${DocumentsContract.getDocumentId(uri)}"
        }.getOrElse {
            uri.toString()
        }
        return MessageDigest.getInstance("SHA-256")
        .digest(identity.toByteArray(Charsets.UTF_8))
        .take(8)
        .joinToString("") { "%02x".format(it) }
    }

    private fun gameCoverId(uri: Uri): String =
        sha256Hex(uri.toString().toByteArray(Charsets.UTF_8))

    private fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(data)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    /** MiNiQ identifies save-state directories by the full SHA-256 of ROM data. */
    private fun saveStateGameKey(game: GameEntry): String =
        synchronized(saveStateKeys) {
            saveStateKeys.getOrPut(game.uri.toString()) {
                val digest = MessageDigest.getInstance("SHA-256")
                contentResolver.openInputStream(game.uri).use { input ->
                    requireNotNull(input) {
                        context.getString(R.string.main_read_named_game_file_failed, game.fileName)
                    }
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                digest.digest().joinToString("") { byte ->
                    "%02x".format(byte.toInt() and 0xFF)
                }
            }
        }

    private fun saveStateGameDirectory(game: GameEntry): File {
        val gameKey = saveStateGameKey(game)
        val target = File(statesDirectory, gameKey).apply { mkdirs() }

        // Import both pre-unified and DingooBox 1.0.3 state locations. The
        // originals remain as rollback copies and existing MiNiQ-format files win.
        listOf(
            File(File(context.filesDir, "states"), game.id),
            context.getExternalFilesDir(null)?.let { File(File(it, "states"), game.id) }
        ).filterNotNull().forEach { legacy ->
            migrateMissingFiles(legacy, target)
        }

        saveStateMetadata.edit()
            .putString("game_${gameKey}_name", game.title)
            .putString("game_${gameKey}_uri", game.uri.toString())
            .apply()
        return target
    }

    /**
     * Copies legacy private-storage data into the app-specific external
     * directory without replacing files already created there. The source is
     * deliberately retained as a rollback backup. Missing files are retried
     * on the next repository creation if a copy is interrupted.
     */
    private fun migrateMissingFiles(source: File, destination: File) {
        if (!source.isDirectory || source == destination) return
        source.walkTopDown().forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(source).path
            if (relativePath.isEmpty()) return@forEach
            val destinationFile = File(destination, relativePath)
            runCatching {
                if (sourceFile.isDirectory) {
                    destinationFile.mkdirs()
                } else if (!destinationFile.exists()) {
                    destinationFile.parentFile?.mkdirs()
                    sourceFile.copyTo(destinationFile, overwrite = false)
                    destinationFile.setLastModified(sourceFile.lastModified())
                }
            }.onFailure { error ->
                Log.w(
                    STORAGE_LOG_TAG,
                    "Unable to migrate ${sourceFile.absolutePath}",
                    error
                )
            }
        }
    }

    private companion object {
        const val GAME_DIRECTORY_KEY = "game_directory"
        const val MAX_DOWNLOADED_COVER_SIZE_BYTES = 8 * 1024 * 1024
        const val COVER_CONTENT_HASH_LENGTH = 16
        const val STORAGE_LOG_TAG = "DingooStorage"
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
