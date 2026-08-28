package io.github.uplush.dingoobox

import android.content.Context
import java.io.File

internal class DingooUserDirectory(
    context: Context
) {
    private val appContext =
        context.applicationContext

    private val externalRootDirectory =
        appContext.getExternalFilesDir(null)

    val usesExternalStorage: Boolean =
        externalRootDirectory != null

    val rootDirectory: File =
        externalRootDirectory
            ?: File(
                appContext.filesDir,
                INTERNAL_FALLBACK_DIRECTORY
            )

    val settingsDirectory =
        childDirectory(
            SETTINGS_DIRECTORY
        )

    val gameSettingsDirectory =
        childDirectory(
            GAME_SETTINGS_DIRECTORY
        )

    val inputProfilesDirectory =
        childDirectory(
            INPUT_PROFILES_DIRECTORY
        )

    val savesDirectory =
        childDirectory(
            SAVES_DIRECTORY
        )

    val saveStatesDirectory =
        childDirectory(
            SAVE_STATES_DIRECTORY
        )

    val screenshotsDirectory =
        childDirectory(
            SCREENSHOTS_DIRECTORY
        )

    val coversDirectory =
        childDirectory(
            COVERS_DIRECTORY
        )

    val logsDirectory =
        childDirectory(
            LOGS_DIRECTORY
        )

    val systemDirectory =
        childDirectory(
            SYSTEM_DIRECTORY
        )

    val cacheDirectory =
        childDirectory(
            CACHE_DIRECTORY
        )

    private val managedDirectories =
        listOf(
            settingsDirectory,
            gameSettingsDirectory,
            inputProfilesDirectory,
            savesDirectory,
            saveStatesDirectory,
            screenshotsDirectory,
            coversDirectory,
            logsDirectory,
            systemDirectory,
            cacheDirectory
        )

    fun ensureCreated(): Boolean {
        if (!ensureDirectory(rootDirectory)) {
            return false
        }

        return managedDirectories.all { directory ->
            ensureDirectory(directory)
        }
    }

    private fun childDirectory(
        name: String
    ): File {
        return File(
            rootDirectory,
            name
        )
    }

    private fun ensureDirectory(
        directory: File
    ): Boolean {
        return directory.isDirectory ||
            directory.mkdirs()
    }

    private companion object {
        const val INTERNAL_FALLBACK_DIRECTORY =
            "userdata"

        const val SETTINGS_DIRECTORY =
            "settings"

        const val GAME_SETTINGS_DIRECTORY =
            "gamesettings"

        const val INPUT_PROFILES_DIRECTORY =
            "inputprofiles"

        const val SAVES_DIRECTORY =
            "saves"

        const val SAVE_STATES_DIRECTORY =
            "savestates"

        const val SCREENSHOTS_DIRECTORY =
            "screenshots"

        const val COVERS_DIRECTORY =
            "covers"

        const val LOGS_DIRECTORY =
            "logs"

        const val SYSTEM_DIRECTORY =
            "system"

        const val CACHE_DIRECTORY =
            "cache"
    }
}
