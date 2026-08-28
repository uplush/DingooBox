package io.github.uplush.dingoobox

import android.content.Context
import java.io.File

internal class DingooPreferenceRepository(
    context: Context,
    private val userDirectory:
        DingooUserDirectory =
        DingooUserDirectory(context)
) {
    private val appContext =
        context.applicationContext

    init {
        check(
            userDirectory.ensureCreated()
        ) {
            "Unable to create the DingooBox user data directory"
        }
    }

    fun settings(
        preferenceName: String
    ): DingooPreferences {
        validateFileComponent(
            preferenceName
        )

        return open(
            file =
                File(
                    userDirectory
                        .settingsDirectory,
                    "$preferenceName.json"
                ),
            legacyPreferenceName =
                preferenceName
        )
    }

    fun controlSettings(
        preferenceName: String
    ): DingooPreferences {
        val gameScopedMatch =
            GAME_SCOPED_PREFERENCES_PATTERN
                .matchEntire(
                    preferenceName
                )

        if (gameScopedMatch == null) {
            return settings(
                preferenceName
            )
        }

        val baseName =
            gameScopedMatch
                .groupValues[1]

        val gameScopeId =
            gameScopedMatch
                .groupValues[2]

        return gameSettings(
            gameScopeId =
                gameScopeId,
            preferenceName =
                baseName,
            legacyPreferenceName =
                preferenceName
        )
    }

    fun gameSettings(
        gameScopeId: String,
        preferenceName: String,
        legacyPreferenceName: String
    ): DingooPreferences {
        validateFileComponent(
            gameScopeId
        )

        validateFileComponent(
            preferenceName
        )

        validateFileComponent(
            legacyPreferenceName
        )

        val gameDirectory =
            File(
                userDirectory
                    .gameSettingsDirectory,
                gameScopeId
            )

        return open(
            file =
                File(
                    gameDirectory,
                    "$preferenceName.json"
                ),
            legacyPreferenceName =
                legacyPreferenceName
        )
    }

    fun inputProfile(
        profileId: String,
        preferenceName: String,
        legacyPreferenceName: String
    ): DingooPreferences {
        validateFileComponent(
            profileId
        )

        validateFileComponent(
            preferenceName
        )

        validateFileComponent(
            legacyPreferenceName
        )

        val profileDirectory =
            File(
                userDirectory
                    .inputProfilesDirectory,
                profileId
            )

        return open(
            file =
                File(
                    profileDirectory,
                    "$preferenceName.json"
                ),
            legacyPreferenceName =
                legacyPreferenceName
        )
    }

    fun settingsFile(
        preferenceName: String
    ): File {
        validateFileComponent(
            preferenceName
        )

        return File(
            userDirectory.settingsDirectory,
            "$preferenceName.json"
        )
    }

    fun controlSettingsFile(
        preferenceName: String
    ): File {
        val gameScopedMatch =
            GAME_SCOPED_PREFERENCES_PATTERN
                .matchEntire(
                    preferenceName
                )

        if (gameScopedMatch == null) {
            return settingsFile(
                preferenceName
            )
        }

        val baseName =
            gameScopedMatch
                .groupValues[1]

        val gameScopeId =
            gameScopedMatch
                .groupValues[2]

        validateFileComponent(
            baseName
        )

        validateFileComponent(
            gameScopeId
        )

        return File(
            File(
                userDirectory
                    .gameSettingsDirectory,
                gameScopeId
            ),
            "$baseName.json"
        )
    }

    fun inputProfileFile(
        profileId: String,
        preferenceName: String
    ): File {
        validateFileComponent(
            profileId
        )

        validateFileComponent(
            preferenceName
        )

        return File(
            File(
                userDirectory
                    .inputProfilesDirectory,
                profileId
            ),
            "$preferenceName.json"
        )
    }

    fun resetAllSettings(): Boolean {
        val gridViewEnabled =
            runCatching {
                settings("home_sort")
                    .getBoolean(
                        "grid_view_enabled",
                        false
                    )
            }.getOrDefault(false)

        val globalSettingsReset =
            resetGlobalSettings()

        val gameSettingsReset =
            resetGameSettings()

        val inputProfilesReset =
            resetInputProfiles()

        val legacyPreferencesReset =
            resetLegacyPreferences()

        val homeDisplayPreferenceRestored =
            runCatching {
                settings("home_sort")
                    .edit()
                    .putBoolean(
                        "grid_view_enabled",
                        gridViewEnabled
                    )
                    .commit()
            }.getOrDefault(false)

        return globalSettingsReset &&
            gameSettingsReset &&
            inputProfilesReset &&
            legacyPreferencesReset &&
            homeDisplayPreferenceRestored
    }

    private fun resetGlobalSettings(): Boolean {
        var resetSuccessful = true

        userDirectory
            .settingsDirectory
            .listFiles()
            .orEmpty()
            .forEach { file ->
                val preferenceName =
                    file.nameWithoutExtension

                if (
                    !isPreferenceFile(file) ||
                    preferenceName in
                        preservedSettingsPreferenceNames
                ) {
                    return@forEach
                }

                val cleared =
                    runCatching {
                        settings(
                            preferenceName
                        ).edit()
                            .clear()
                            .commit()
                    }.getOrDefault(false)

                if (!cleared) {
                    resetSuccessful = false
                }
            }

        return resetSuccessful
    }

    private fun resetGameSettings(): Boolean {
        var resetSuccessful = true

        userDirectory
            .gameSettingsDirectory
            .listFiles()
            .orEmpty()
            .filter { directory ->
                directory.isDirectory &&
                    FILE_COMPONENT_PATTERN
                        .matches(directory.name)
            }
            .forEach { gameDirectory ->
                gameDirectory
                    .listFiles()
                    .orEmpty()
                    .forEach fileLoop@ { file ->
                        if (!isPreferenceFile(file)) {
                            return@fileLoop
                        }

                        val preferenceName =
                            file.nameWithoutExtension

                        val legacyPreferenceName =
                            "${preferenceName}_game_${
                                gameDirectory.name
                            }"

                        val cleared =
                            runCatching {
                                gameSettings(
                                    gameScopeId =
                                        gameDirectory.name,
                                    preferenceName =
                                        preferenceName,
                                    legacyPreferenceName =
                                        legacyPreferenceName
                                ).edit()
                                    .clear()
                                    .commit()
                            }.getOrDefault(false)

                        if (!cleared) {
                            resetSuccessful = false
                        }
                    }
            }

        return resetSuccessful
    }

    private fun resetInputProfiles(): Boolean {
        var resetSuccessful = true

        userDirectory
            .inputProfilesDirectory
            .listFiles()
            .orEmpty()
            .filter { directory ->
                directory.isDirectory &&
                    FILE_COMPONENT_PATTERN
                        .matches(directory.name)
            }
            .forEach { profileDirectory ->
                profileDirectory
                    .listFiles()
                    .orEmpty()
                    .forEach fileLoop@ { file ->
                        if (!isPreferenceFile(file)) {
                            return@fileLoop
                        }

                        val preferenceName =
                            file.nameWithoutExtension

                        val resetLegacyName =
                            "reset_profile_${
                                profileDirectory.name
                            }_$preferenceName"

                        val cleared =
                            runCatching {
                                inputProfile(
                                    profileId =
                                        profileDirectory.name,
                                    preferenceName =
                                        preferenceName,
                                    legacyPreferenceName =
                                        resetLegacyName
                                ).edit()
                                    .clear()
                                    .commit()
                            }.getOrDefault(false)

                        if (!cleared) {
                            resetSuccessful = false
                        }
                    }
            }

        return resetSuccessful
    }

    private fun resetLegacyPreferences(): Boolean {
        val sharedPreferencesDirectory =
            File(
                appContext.applicationInfo.dataDir,
                "shared_prefs"
            )

        var resetSuccessful = true

        sharedPreferencesDirectory
            .listFiles()
            .orEmpty()
            .filter { file ->
                file.isFile &&
                    file.extension.equals(
                        "xml",
                        ignoreCase = true
                    )
            }
            .map { file ->
                file.nameWithoutExtension
            }
            .filter { preferenceName ->
                shouldResetLegacyPreference(
                    preferenceName
                )
            }
            .forEach { preferenceName ->
                val deleted =
                    runCatching {
                        appContext
                            .deleteSharedPreferences(
                                preferenceName
                            )
                    }.getOrDefault(false)

                if (!deleted) {
                    resetSuccessful = false
                }
            }

        return resetSuccessful
    }

    private fun shouldResetLegacyPreference(
        preferenceName: String
    ): Boolean {
        return preferenceName in
            resettableLegacyPreferenceNames ||
            GAME_SCOPED_PREFERENCES_PATTERN
                .matches(preferenceName) ||
            dingooGameControlPreferencesPattern
                .matches(preferenceName) ||
            controlProfilePreferencesPattern
                .matches(preferenceName)
    }

    private fun isPreferenceFile(
        file: File
    ): Boolean {
        return file.isFile &&
            file.extension.equals(
                "json",
                ignoreCase = true
            ) &&
            FILE_COMPONENT_PATTERN.matches(
                file.nameWithoutExtension
            )
    }

    private val preservedSettingsPreferenceNames =
        setOf(
            "home_sort",
            "game_library",
            "save_state_metadata"
        )

    private val resettableLegacyPreferenceNames =
        setOf(
            "app_settings",
            "landscape_control_layout",
            "portrait_control_layout",
            "input_bindings",
            "control_behavior",
            "control_rumble",
            "control_setting_scopes",
            "control_profiles"
        )

    private val controlProfilePreferencesPattern =
        Regex(
            "^control_profile_[0-9a-f]{64}_.+$"
        )

    private val dingooGameControlPreferencesPattern =
        Regex(
            "^(input_bindings|control_behavior|control_layout_(portrait|landscape))_[A-Za-z0-9_.-]+$"
        )
    private fun open(
        file: File,
        legacyPreferenceName: String
    ): DingooPreferences {
        val legacyPreferences =
            appContext.getSharedPreferences(
                legacyPreferenceName,
                Context.MODE_PRIVATE
            )

        return DingooFilePreferences
            .open(
                file = file,
                legacyPreferences =
                    legacyPreferences
            )
    }

    private fun validateFileComponent(
        value: String
    ) {
        require(
            FILE_COMPONENT_PATTERN
                .matches(value)
        ) {
            "Invalid settings file name: $value"
        }
    }

    private companion object {
        val FILE_COMPONENT_PATTERN =
            Regex(
                "^[A-Za-z0-9_.-]+$"
            )

        val GAME_SCOPED_PREFERENCES_PATTERN =
            Regex(
                "^(.+)_game_([0-9a-f]{64})$"
            )
    }
}
