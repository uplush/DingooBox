package io.github.uplush.dingoobox

import android.content.Context
import java.security.MessageDigest
import java.util.Locale

private const val CONTROL_SCOPE_PREFERENCES = "control_setting_scopes"
private const val CONTROL_PROFILE_REGISTRY = "control_profiles_v2"
private const val PROFILE_NAMES_KEY = "profile_names"
private const val MAX_PROFILE_NAME_LENGTH = 40

data class ControlBehaviorState(
    val virtualControlsVisible: Boolean,
    val vibrationEnabled: Boolean
)

class ControlBehaviorPreferences(
    context: Context,
    gameKey: String? = null
) {
    private val suffix = gameKey?.takeIf { it.isNotBlank() }?.let { "_$it" }.orEmpty()
    private val preferences = context.getSharedPreferences(
        "control_behavior$suffix",
        Context.MODE_PRIVATE
    )

    fun load(fallback: AppSettingsState): ControlBehaviorState = ControlBehaviorState(
        virtualControlsVisible = preferences.getBoolean(
            "virtual_controls_visible",
            fallback.virtualControlsVisible
        ),
        vibrationEnabled = preferences.getBoolean(
            "vibration_enabled",
            fallback.vibrationEnabled
        )
    )

    fun save(state: ControlBehaviorState) {
        preferences.edit()
            .putBoolean("virtual_controls_visible", state.virtualControlsVisible)
            .putBoolean("vibration_enabled", state.vibrationEnabled)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}

class ControlSettingsScopePreferences(
    context: Context
) {
    private val appContext = context.applicationContext
    private val preferences =
        DingooPreferenceRepository(appContext).settings(CONTROL_SCOPE_PREFERENCES)
    private val legacyPreferences =
        appContext.getSharedPreferences("control_profiles", Context.MODE_PRIVATE)

    fun isGameIndependent(gameKey: String?): Boolean {
        if (gameKey.isNullOrBlank()) return false
        val flagKey = independentFlagKey(gameKey)
        if (preferences.contains(flagKey)) {
            return preferences.getBoolean(flagKey, false)
        }
        val legacyEnabled = legacyPreferences.getBoolean(gameKey, false)
        if (legacyEnabled) {
            preferences.edit().putBoolean(flagKey, true).commit()
            legacyPreferences.edit().remove(gameKey).apply()
        }
        return legacyEnabled
    }

    fun setGameIndependent(gameKey: String, enabled: Boolean): Boolean {
        if (gameKey.isBlank()) return false
        legacyPreferences.edit().remove(gameKey).apply()
        return if (enabled) {
            if (!copyGlobalToGame(gameKey)) return false
            preferences.edit()
                .putBoolean(independentFlagKey(gameKey), true)
                .commit()
        } else {
            val disabled = preferences.edit()
                .remove(independentFlagKey(gameKey))
                .commit()
            if (disabled) clearGameSettings(gameKey)
            disabled
        }
    }

    fun copyGlobalToGame(gameKey: String): Boolean = runCatching {
        val globalBindings = InputBindingPreferences(appContext)
        InputBindingPreferences(appContext, gameKey)
            .replaceBindings(globalBindings.snapshotBindings())

        val appSettings = AppPreferences(appContext).load()
        val globalBehavior = ControlBehaviorPreferences(appContext).load(appSettings)
        ControlBehaviorPreferences(appContext, gameKey).save(globalBehavior)

        listOf(false, true).forEach { portrait ->
            val globalLayout = ControlLayoutPreferences(appContext, portrait).load()
            ControlLayoutPreferences(appContext, portrait, gameKey).save(globalLayout)
        }
        true
    }.getOrDefault(false)

    private fun clearGameSettings(gameKey: String) {
        InputBindingPreferences(appContext, gameKey).clearScope()
        ControlBehaviorPreferences(appContext, gameKey).clear()
        ControlLayoutPreferences(appContext, portrait = false, gameKey = gameKey).reset()
        ControlLayoutPreferences(appContext, portrait = true, gameKey = gameKey).reset()
    }

    private fun independentFlagKey(gameKey: String): String =
        "independent_${stableId(gameKey)}"
}

data class LoadedControlProfile(
    val virtualControlsVisible: Boolean,
    val vibrationEnabled: Boolean
)

class ControlProfilePreferences(
    context: Context
) {
    private val appContext = context.applicationContext
    private val repository = DingooPreferenceRepository(appContext)
    private val registry = repository.settings(CONTROL_PROFILE_REGISTRY)

    fun profileNames(): List<String> = registry
        .getStringSet(PROFILE_NAMES_KEY, emptySet())
        .orEmpty()
        .sortedBy { it.lowercase(Locale.ROOT) }

    fun saveProfile(
        name: String,
        gameKey: String?,
        settings: AppSettingsState
    ): String? {
        val normalized = normalizeName(name) ?: return null
        val storedName = profileNames().firstOrNull {
            it.equals(normalized, ignoreCase = true)
        } ?: normalized
        val profile = profilePreferences(storedName)
        val editor = profile.edit().clear()

        val behavior = ControlBehaviorPreferences(appContext, gameKey).load(settings)

        InputBindingPreferences(appContext, gameKey)
            .snapshotBindings()
            .forEach { (action, binding) ->
                writeBinding(editor, action, binding)
            }

        writeLayout(
            editor,
            "landscape",
            ControlLayoutPreferences(appContext, portrait = false, gameKey = gameKey).load()
        )
        writeLayout(
            editor,
            "portrait",
            ControlLayoutPreferences(appContext, portrait = true, gameKey = gameKey).load()
        )
        editor.putBoolean("virtual_controls_visible", behavior.virtualControlsVisible)
        editor.putBoolean("vibration_enabled", behavior.vibrationEnabled)
        if (!editor.commit()) return null

        val names = profileNames().toMutableSet().apply {
            removeAll { it.equals(storedName, ignoreCase = true) }
            add(storedName)
        }
        return if (
            registry.edit().putStringSet(PROFILE_NAMES_KEY, names).commit()
        ) storedName else null
    }

    fun loadProfile(name: String, gameKey: String?): LoadedControlProfile? {
        val storedName = profileNames().firstOrNull {
            it.equals(name.trim(), ignoreCase = true)
        } ?: return null
        val profile = profilePreferences(storedName)

        val bindings = DingooInputAction.entries.associateWith { action ->
            readBinding(profile, action)
        }
        InputBindingPreferences(appContext, gameKey).replaceBindings(bindings)

        ControlLayoutPreferences(appContext, portrait = false, gameKey = gameKey).save(
            readLayout(profile, "landscape", portrait = false)
        )
        ControlLayoutPreferences(appContext, portrait = true, gameKey = gameKey).save(
            readLayout(profile, "portrait", portrait = true)
        )
        val loaded = LoadedControlProfile(
            virtualControlsVisible = profile.getBoolean("virtual_controls_visible", true),
            vibrationEnabled = profile.getBoolean("vibration_enabled", true)
        )
        ControlBehaviorPreferences(appContext, gameKey).save(
            ControlBehaviorState(
                virtualControlsVisible = loaded.virtualControlsVisible,
                vibrationEnabled = loaded.vibrationEnabled
            )
        )
        return loaded
    }

    private fun profilePreferences(name: String): DingooPreferences {
        val id = stableId(name.trim().lowercase(Locale.ROOT))
        return repository.inputProfile(
            profileId = id,
            preferenceName = "controller",
            legacyPreferenceName = "control_profile_${id}_controller"
        )
    }

    private fun writeBinding(
        editor: DingooPreferences.Editor,
        action: DingooInputAction,
        binding: PhysicalInputBinding?
    ) {
        val prefix = "binding_${action.name}"
        editor.putInt("${prefix}_type", binding?.type?.ordinal ?: -1)
        editor.putInt("${prefix}_code", binding?.code ?: -1)
        editor.putInt("${prefix}_direction", binding?.direction ?: 0)
    }

    private fun readBinding(
        preferences: DingooPreferences,
        action: DingooInputAction
    ): PhysicalInputBinding? {
        val prefix = "binding_${action.name}"
        val typeValue = preferences.getInt("${prefix}_type", -1)
        val type = PhysicalInputBindingType.entries.getOrNull(typeValue) ?: return null
        val code = preferences.getInt("${prefix}_code", -1)
        if (code < 0) return null
        val direction = preferences.getInt("${prefix}_direction", 0)
        if (type == PhysicalInputBindingType.AXIS && direction !in setOf(-1, 1)) return null
        return PhysicalInputBinding(
            type = type,
            code = code,
            direction = if (type == PhysicalInputBindingType.KEY) 0 else direction
        )
    }

    private fun writeLayout(
        editor: DingooPreferences.Editor,
        prefix: String,
        layout: ControlLayoutState
    ) {
        editor.putFloat("${prefix}_scale", layout.scale)
        editor.putFloat("${prefix}_opacity", layout.opacity)
        editor.putStringSet("${prefix}_hidden", layout.hidden.map { it.name }.toSet())
        layout.positions.forEach { (id, position) ->
            editor.putFloat("${prefix}_${id.name}_x", position.x)
            editor.putFloat("${prefix}_${id.name}_y", position.y)
            editor.putFloat(
                "${prefix}_${id.name}_scale",
                layout.controlScales[id] ?: layout.scale
            )
        }
    }

    private fun readLayout(
        preferences: DingooPreferences,
        prefix: String,
        portrait: Boolean
    ): ControlLayoutState {
        val defaults = ControlLayoutPreferences.defaultPositions(portrait)
        val scale = preferences.getFloat("${prefix}_scale", 1f).coerceIn(0.7f, 1.4f)
        val positions = ControlId.entries.associateWith { id ->
            val fallback = defaults.getValue(id)
            ControlPosition(
                x = preferences.getFloat("${prefix}_${id.name}_x", fallback.x),
                y = preferences.getFloat("${prefix}_${id.name}_y", fallback.y)
            )
        }
        val hiddenNames = preferences.getStringSet("${prefix}_hidden", emptySet()).orEmpty()
        return ControlLayoutState(
            positions = ControlLayoutPreferences.migrateLegacyDefaultPositions(
                portrait = portrait,
                positions = positions
            ),
            scale = scale,
            opacity = preferences.getFloat(
                "${prefix}_opacity",
                if (portrait) 1f else 0.65f
            ).coerceIn(0f, 1f),
            hidden = ControlId.entries.filterTo(mutableSetOf()) { it.name in hiddenNames },
            controlScales = ControlId.entries.associateWith { id ->
                preferences.getFloat("${prefix}_${id.name}_scale", scale)
                    .coerceIn(0.7f, 1.4f)
            }
        )
    }

    private fun normalizeName(name: String): String? = name.trim().takeIf {
        it.isNotEmpty() && it.length <= MAX_PROFILE_NAME_LENGTH
    }
}

private fun stableId(value: String): String = MessageDigest
    .getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
