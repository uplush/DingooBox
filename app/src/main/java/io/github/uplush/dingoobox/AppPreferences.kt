package io.github.uplush.dingoobox

import android.content.Context
import android.view.KeyEvent
import androidx.core.content.edit

enum class AspectRatioMode(
    val preferenceValue: Int,
    val displayNameResource: Int
) {
    Original(0, R.string.aspect_ratio_original),
    Stretch(1, R.string.aspect_ratio_stretch),
    Fill(2, R.string.aspect_ratio_fill);

    companion object {
        fun fromPreferenceValue(value: Int): AspectRatioMode =
            entries.firstOrNull { it.preferenceValue == value } ?: Original
    }
}

enum class ImageFilterMode(
    val preferenceValue: Int,
    val displayNameResource: Int
) {
    DotMatrix(0, R.string.image_filter_dot_matrix),
    Scanlines(1, R.string.image_filter_scanlines),
    EdgeSmoothing(2, R.string.image_filter_edge_smoothing),
    Off(3, R.string.image_filter_off);

    companion object {
        fun fromPreferenceValue(value: Int): ImageFilterMode =
            entries.firstOrNull { it.preferenceValue == value } ?: DotMatrix
    }
}

data class AppSettingsState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val orientation: ScreenOrientationMode = ScreenOrientationMode.DEVICE,
    val aspectRatio: AspectRatioMode = AspectRatioMode.Original,
    val imageFilter: ImageFilterMode = ImageFilterMode.DotMatrix,
    val immersiveMode: Boolean = true,
    val showInformation: Boolean = true,
    val showEmulationSpeed: Boolean = false,
    val showFps: Boolean = false,
    val gameMuted: Boolean = false,
    val volumePercent: Int = 100,
    val soundSofteningEnabled: Boolean = false,
    val soundSofteningPercent: Int = 60,
    val frameRateEnhancementEnabled: Boolean = false,
    val autoSaveEnabled: Boolean = false,
    val fastForwardMultiplier: Int = 2,
    val virtualControlsVisible: Boolean = true,
    val vibrationEnabled: Boolean = true
)

class AppPreferences(context: Context) {
    private val preferences =
        DingooPreferenceRepository(context).settings("app_settings")

    init {
        migrateSettings()
    }

    fun load(): AppSettingsState = AppSettingsState(
        themeMode = AppThemeMode.fromPreferenceValue(
            preferences.getString("app_theme", null)
        ),
        language = AppLanguage.fromPreferenceValue(
            preferences.getString("app_language", null)
        ),
        orientation = ScreenOrientationMode.fromPreferenceValue(
            preferences.getString("screen_orientation", null)
        ),
        aspectRatio = AspectRatioMode.fromPreferenceValue(
            preferences.getInt("aspect_ratio_mode", 0)
        ),
        imageFilter = ImageFilterMode.fromPreferenceValue(
            preferences.getInt("lcd_filter_mode", 0)
        ),
        immersiveMode = preferences.getBoolean("immersive_mode", true),
        showInformation = preferences.getBoolean("show_information", true),
        showEmulationSpeed = preferences.getBoolean("show_emulation_speed", false),
        showFps = preferences.getBoolean("show_fps", false),
        gameMuted = preferences.getBoolean("game_muted", false),
        volumePercent = preferences.getInt("audio_volume_percent", 100).coerceIn(0, 100),
        soundSofteningEnabled = preferences.getBoolean("sound_softening_enabled", false),
        soundSofteningPercent = preferences
            .getInt("sound_softening_level_percent", 60)
            .coerceIn(5, 95),
        frameRateEnhancementEnabled = preferences.getBoolean(
            "frame_rate_enhancement_enabled",
            false
        ),
        autoSaveEnabled = preferences.getBoolean("auto_save_state_enabled", false),
        fastForwardMultiplier = normalizeFastForwardMultiplier(
            preferences.getInt("fast_forward_multiplier", 2)
        ),
        virtualControlsVisible = preferences.getBoolean("virtual_controls_visible", true),
        vibrationEnabled = preferences.getBoolean("vibration_enabled", true)
    )

    fun save(settings: AppSettingsState) {
        val editor = preferences.edit()
        editor.apply {
            putString("app_theme", settings.themeMode.preferenceValue)
            putString("app_language", settings.language.preferenceValue)
            putString("screen_orientation", settings.orientation.preferenceValue)
            putInt("aspect_ratio_mode", settings.aspectRatio.preferenceValue)
            putInt("lcd_filter_mode", settings.imageFilter.preferenceValue)
            putBoolean("immersive_mode", settings.immersiveMode)
            putBoolean("show_information", settings.showInformation)
            putBoolean("show_emulation_speed", settings.showEmulationSpeed)
            putBoolean("show_fps", settings.showFps)
            putBoolean("game_muted", settings.gameMuted)
            putInt("audio_volume_percent", settings.volumePercent.coerceIn(0, 100))
            putBoolean("sound_softening_enabled", settings.soundSofteningEnabled)
            putInt(
                "sound_softening_level_percent",
                settings.soundSofteningPercent.coerceIn(5, 95)
            )
            putBoolean(
                "frame_rate_enhancement_enabled",
                settings.frameRateEnhancementEnabled
            )
            putBoolean("auto_save_state_enabled", settings.autoSaveEnabled)
            putInt(
                "fast_forward_multiplier",
                normalizeFastForwardMultiplier(settings.fastForwardMultiplier)
            )
            putBoolean("virtual_controls_visible", settings.virtualControlsVisible)
            putBoolean("vibration_enabled", settings.vibrationEnabled)
            putInt(SETTINGS_SCHEMA_VERSION_KEY, SETTINGS_SCHEMA_VERSION)
        }
        editor.apply()
    }

    private fun migrateSettings() {
        val schemaVersion = preferences.getInt(SETTINGS_SCHEMA_VERSION_KEY, 0)
        if (schemaVersion >= SETTINGS_SCHEMA_VERSION) return

        if (schemaVersion < 15) {
            migrateAlpha14Settings()
        } else if (schemaVersion < 19) {
            migrateAlpha19Settings()
        }
        removeUnsupportedPlatformSettings()
    }

    private fun migrateAlpha14Settings() {

        val editor = preferences.edit()

        if (!preferences.contains("app_theme") && preferences.contains("theme")) {
            editor.putString(
                "app_theme",
                when (preferences.getString("theme", null)) {
                    "Light" -> AppThemeMode.LIGHT.preferenceValue
                    "Dark" -> AppThemeMode.DARK.preferenceValue
                    else -> AppThemeMode.SYSTEM.preferenceValue
                }
            )
        }
        if (!preferences.contains("app_language") && preferences.contains("language")) {
            editor.putString(
                "app_language",
                when (preferences.getString("language", null)) {
                    "Chinese" -> AppLanguage.SIMPLIFIED_CHINESE.preferenceValue
                    "English" -> AppLanguage.ENGLISH.preferenceValue
                    else -> AppLanguage.SYSTEM.preferenceValue
                }
            )
        }
        if (!preferences.contains("screen_orientation") && preferences.contains("orientation")) {
            editor.putString(
                "screen_orientation",
                when (preferences.getString("orientation", null)) {
                    "Landscape" -> ScreenOrientationMode.LANDSCAPE.preferenceValue
                    "Portrait" -> ScreenOrientationMode.PORTRAIT.preferenceValue
                    else -> ScreenOrientationMode.DEVICE.preferenceValue
                }
            )
        }
        if (!preferences.contains("aspect_ratio_mode") && preferences.contains("aspect_ratio")) {
            editor.putInt(
                "aspect_ratio_mode",
                when (preferences.getString("aspect_ratio", null)) {
                    "Fit" -> AspectRatioMode.Fill.preferenceValue
                    "Stretch" -> AspectRatioMode.Stretch.preferenceValue
                    else -> AspectRatioMode.Original.preferenceValue
                }
            )
        }
        if (!preferences.contains("lcd_filter_mode") && preferences.contains("image_filter")) {
            editor.putInt(
                "lcd_filter_mode",
                if (preferences.getString("image_filter", null) == "Bilinear") {
                    ImageFilterMode.EdgeSmoothing.preferenceValue
                } else {
                    ImageFilterMode.Off.preferenceValue
                }
            )
        }

        migrateBoolean(editor, "immersive", "immersive_mode", true)
        migrateBoolean(editor, "show_speed", "show_emulation_speed", false)
        migrateBoolean(editor, "muted", "game_muted", false)
        migrateInt(editor, "volume", "audio_volume_percent", 100)
        migrateBoolean(editor, "sound_softening", "sound_softening_enabled", false)
        migrateInt(editor, "softening_level", "sound_softening_level_percent", 60)
        migrateBoolean(editor, "auto_save", "auto_save_state_enabled", true)
        migrateInt(editor, "fast_forward", "fast_forward_multiplier", 2)
        migrateBoolean(editor, "virtual_controls", "virtual_controls_visible", true)
        migrateBoolean(editor, "vibration", "vibration_enabled", true)

        listOf(
            "theme", "language", "orientation", "aspect_ratio", "image_filter",
            "immersive", "show_speed", "muted", "volume", "original_sound",
            "sound_softening", "softening_level", "auto_save", "fast_forward",
            "screen_shake", "virtual_controls", "vibration"
        ).forEach { editor.remove(it) }

        editor.putInt(SETTINGS_SCHEMA_VERSION_KEY, 19).commit()
    }

    /**
     * alpha15-alpha18 used Dingoo-specific display encodings. Preserve the
     * selected visual behavior while switching to MiNiQ's source values:
     * aspect 1/2 were Fill/Stretch and filter 0/3 were Off/Smoothing.
     */
    private fun migrateAlpha19Settings() {
        val editor = preferences.edit()

        if (preferences.contains("aspect_ratio_mode")) {
            val migratedAspectRatio = when (
                preferences.getInt("aspect_ratio_mode", 0)
            ) {
                1 -> AspectRatioMode.Fill.preferenceValue
                2 -> AspectRatioMode.Stretch.preferenceValue
                else -> AspectRatioMode.Original.preferenceValue
            }
            editor.putInt("aspect_ratio_mode", migratedAspectRatio)
        }

        if (preferences.contains("lcd_filter_mode")) {
            val migratedFilter = when (
                preferences.getInt("lcd_filter_mode", 0)
            ) {
                3 -> ImageFilterMode.EdgeSmoothing.preferenceValue
                else -> ImageFilterMode.Off.preferenceValue
            }
            editor.putInt("lcd_filter_mode", migratedFilter)
        }

        if (preferences.contains("sound_softening_level_percent")) {
            editor.putInt(
                "sound_softening_level_percent",
                preferences.getInt("sound_softening_level_percent", 60)
                    .coerceIn(5, 95)
            )
        }

        if (preferences.contains("fast_forward_multiplier")) {
            editor.putInt(
                "fast_forward_multiplier",
                normalizeFastForwardMultiplier(
                    preferences.getInt("fast_forward_multiplier", 2)
                )
            )
        }

        editor.putInt(SETTINGS_SCHEMA_VERSION_KEY, 19).commit()
    }

    /**
     * Remove Pokemon Mini-specific settings that have no DingooEmu equivalent.
     */
    private fun removeUnsupportedPlatformSettings() {
        preferences.edit()
            .remove("lcd_mode")
            .remove("screen_shake_level")
            .remove("original_sound")
            .remove("original_sound_enabled")
            .putInt(SETTINGS_SCHEMA_VERSION_KEY, SETTINGS_SCHEMA_VERSION)
            .commit()
    }

    private fun migrateBoolean(
        editor: DingooPreferences.Editor,
        oldKey: String,
        newKey: String,
        defaultValue: Boolean
    ) {
        if (!preferences.contains(newKey) && preferences.contains(oldKey)) {
            editor.putBoolean(newKey, preferences.getBoolean(oldKey, defaultValue))
        }
    }

    private fun migrateInt(
        editor: DingooPreferences.Editor,
        oldKey: String,
        newKey: String,
        defaultValue: Int
    ) {
        if (!preferences.contains(newKey) && preferences.contains(oldKey)) {
            editor.putInt(newKey, preferences.getInt(oldKey, defaultValue))
        }
    }

    companion object {
        private const val SETTINGS_SCHEMA_VERSION_KEY = "dingoo_settings_schema_version"
        private const val SETTINGS_SCHEMA_VERSION = 23

        private fun normalizeFastForwardMultiplier(value: Int): Int = when (value) {
            0, 2, 3, 4 -> value
            6, 8 -> 4
            else -> 2
        }
    }
}

enum class DingooInputAction(
    val displayNameResource: Int,
    val retroButtonId: Int,
    val defaultKeyCode: Int
) {
    Up(io.github.uplush.dingoobox.R.string.controller_action_up, 4, KeyEvent.KEYCODE_DPAD_UP),
    Down(io.github.uplush.dingoobox.R.string.controller_action_down, 5, KeyEvent.KEYCODE_DPAD_DOWN),
    Left(io.github.uplush.dingoobox.R.string.controller_action_left, 6, KeyEvent.KEYCODE_DPAD_LEFT),
    Right(io.github.uplush.dingoobox.R.string.controller_action_right, 7, KeyEvent.KEYCODE_DPAD_RIGHT),
    A(io.github.uplush.dingoobox.R.string.controller_action_a, 8, KeyEvent.KEYCODE_BUTTON_A),
    B(io.github.uplush.dingoobox.R.string.controller_action_b, 0, KeyEvent.KEYCODE_BUTTON_B),
    X(io.github.uplush.dingoobox.R.string.controller_action_x, 9, KeyEvent.KEYCODE_BUTTON_X),
    Y(io.github.uplush.dingoobox.R.string.controller_action_y, 1, KeyEvent.KEYCODE_BUTTON_Y),
    L(io.github.uplush.dingoobox.R.string.controller_action_l, 10, KeyEvent.KEYCODE_BUTTON_L1),
    R(io.github.uplush.dingoobox.R.string.controller_action_r, 11, KeyEvent.KEYCODE_BUTTON_R1),
    Start(io.github.uplush.dingoobox.R.string.controller_action_start, 3, KeyEvent.KEYCODE_BUTTON_START),
    Select(io.github.uplush.dingoobox.R.string.controller_action_select, 2, KeyEvent.KEYCODE_BUTTON_SELECT)
}

enum class DingooHotkeyAction(
    val displayNameResource: Int,
    val defaultKeyCode: Int?
) {
    Pause(R.string.controller_hotkey_pause_menu, KeyEvent.KEYCODE_BUTTON_MODE),
    FastForward(R.string.controller_hotkey_fast_forward_toggle, KeyEvent.KEYCODE_BUTTON_R2),
    QuickSave(R.string.controller_hotkey_quick_save, null),
    QuickLoad(R.string.controller_hotkey_quick_load, null),
    Screenshot(R.string.controller_hotkey_screenshot, null),
    Reset(R.string.controller_hotkey_restart_game, null)
}

enum class PhysicalInputBindingType {
    KEY,
    AXIS
}

data class PhysicalInputBinding(
    val type: PhysicalInputBindingType,
    val code: Int,
    val direction: Int = 0
)

class InputBindingPreferences(context: Context, gameKey: String? = null) {
    private val scopedGameKey = gameKey?.takeIf { it.isNotBlank() }
    private val usingGameScope = scopedGameKey != null
    private val suffix = scopedGameKey?.let { "_$it" }.orEmpty()
    private val preferences =
        context.getSharedPreferences("input_bindings$suffix", Context.MODE_PRIVATE)
    private val hotkeyPreferences =
        context.getSharedPreferences("input_bindings", Context.MODE_PRIVATE)

    fun getBinding(action: DingooInputAction): PhysicalInputBinding? {
        val typeKey = "binding_type_${action.name}"
        if (!preferences.contains(typeKey)) {
            val legacyCode = preferences.getInt(
                "button_${action.name}",
                action.defaultKeyCode
            )
            return legacyCode.takeUnless { it == UNBOUND }?.let {
                PhysicalInputBinding(PhysicalInputBindingType.KEY, it)
            }
        }

        val typeValue = preferences.getInt(typeKey, UNBOUND)
        if (typeValue == UNBOUND) return null
        val type = PhysicalInputBindingType.entries.getOrNull(typeValue) ?: return null
        val code = preferences.getInt("binding_code_${action.name}", UNBOUND)
        if (code == UNBOUND) return null
        val direction = preferences.getInt("binding_direction_${action.name}", 0)
        if (type == PhysicalInputBindingType.AXIS && direction !in setOf(-1, 1)) return null
        return PhysicalInputBinding(type, code, if (type == PhysicalInputBindingType.KEY) 0 else direction)
    }

    fun setBinding(action: DingooInputAction, binding: PhysicalInputBinding?) {
        val editor = preferences.edit()
        if (binding != null) {
            DingooInputAction.entries
                .filter { it != action && getBinding(it) == binding }
                .forEach { writeBinding(editor, it, null) }

            /*
             * Match MiNiQ's scope boundary: a game-specific button mapping
             * must never alter the global hotkey preferences.
             */
            if (!usingGameScope) {
                val hotkeyEditor = hotkeyPreferences.edit()
                DingooHotkeyAction.entries
                    .filter { getHotkeyBinding(it) == binding }
                    .forEach {
                        hotkeyEditor.remove("hotkey_binding_${it.name}")
                        hotkeyEditor.putInt("hotkey_${it.name}", UNBOUND)
                    }
                hotkeyEditor.apply()
            }
        }
        writeBinding(editor, action, binding)
        editor.apply()
    }

    fun getHotkeyBinding(action: DingooHotkeyAction): PhysicalInputBinding? {
        val stored = hotkeyPreferences.getString("hotkey_binding_${action.name}", null)
        if (stored != null) {
            val parts = stored.split(':')
            if (parts.size != 3) return null
            val type = runCatching { PhysicalInputBindingType.valueOf(parts[0]) }.getOrNull()
                ?: return null
            val code = parts[1].toIntOrNull() ?: return null
            val direction = parts[2].toIntOrNull() ?: return null
            if (type == PhysicalInputBindingType.AXIS && direction !in setOf(-1, 1)) return null
            return PhysicalInputBinding(type, code, if (type == PhysicalInputBindingType.KEY) 0 else direction)
        }
        val legacyCode = hotkeyPreferences.getInt(
            "hotkey_${action.name}",
            action.defaultKeyCode ?: UNBOUND
        )
        return legacyCode.takeUnless { it == UNBOUND }?.let {
            PhysicalInputBinding(PhysicalInputBindingType.KEY, it)
        }
    }

    fun setHotkeyBinding(action: DingooHotkeyAction, binding: PhysicalInputBinding?) {
        val editor = hotkeyPreferences.edit()
        if (binding != null) {
            DingooHotkeyAction.entries
                .filter { it != action && getHotkeyBinding(it) == binding }
                .forEach {
                    editor.remove("hotkey_binding_${it.name}")
                    editor.putInt("hotkey_${it.name}", UNBOUND)
                }

            /*
             * Hotkeys are global. When this object is displaying a game's
             * independent profile, editing a hotkey must not erase any
             * button mapping stored in that game profile.
             */
            if (!usingGameScope) {
                val bindingEditor = preferences.edit()
                DingooInputAction.entries
                    .filter { getBinding(it) == binding }
                    .forEach { writeBinding(bindingEditor, it, null) }
                bindingEditor.apply()
            }
        }
        if (binding == null) {
            editor.remove("hotkey_binding_${action.name}")
            editor.putInt("hotkey_${action.name}", UNBOUND)
        } else {
            val direction = if (binding.type == PhysicalInputBindingType.KEY) 0 else binding.direction
            editor.putString(
                "hotkey_binding_${action.name}",
                "${binding.type.name}:${binding.code}:$direction"
            )
            editor.remove("hotkey_${action.name}")
        }
        editor.apply()
    }

    fun actionForKey(keyCode: Int): DingooInputAction? =
        DingooInputAction.entries.firstOrNull {
            getBinding(it) == PhysicalInputBinding(PhysicalInputBindingType.KEY, keyCode)
        }

    fun hotkeyForKey(keyCode: Int): DingooHotkeyAction? =
        DingooHotkeyAction.entries.firstOrNull {
            getHotkeyBinding(it) == PhysicalInputBinding(PhysicalInputBindingType.KEY, keyCode)
        }

    fun snapshotBindings(): Map<DingooInputAction, PhysicalInputBinding?> =
        DingooInputAction.entries.associateWith(::getBinding)

    fun replaceBindings(bindings: Map<DingooInputAction, PhysicalInputBinding?>) {
        val editor = preferences.edit()
        DingooInputAction.entries.forEach { action ->
            writeBinding(editor, action, bindings[action])
        }
        editor.apply()
    }

    fun clearAllBindings() {
        val editor = preferences.edit()
        DingooInputAction.entries.forEach { writeBinding(editor, it, null) }
        if (!usingGameScope) {
            DingooHotkeyAction.entries.forEach {
                editor.remove("hotkey_binding_${it.name}")
                editor.putInt("hotkey_${it.name}", UNBOUND)
            }
        }
        editor.apply()
    }

    fun resetToDefaults() = preferences.edit { clear() }

    fun clearScope() = preferences.edit { clear() }

    private fun writeBinding(
        editor: android.content.SharedPreferences.Editor,
        action: DingooInputAction,
        binding: PhysicalInputBinding?
    ) {
        editor.remove("button_${action.name}")
        if (binding == null) {
            editor.putInt("binding_type_${action.name}", UNBOUND)
            editor.putInt("binding_code_${action.name}", UNBOUND)
            editor.putInt("binding_direction_${action.name}", UNBOUND)
        } else {
            editor.putInt("binding_type_${action.name}", binding.type.ordinal)
            editor.putInt("binding_code_${action.name}", binding.code)
            editor.putInt(
                "binding_direction_${action.name}",
                if (binding.type == PhysicalInputBindingType.KEY) 0 else binding.direction
            )
        }
    }

    companion object {
        private const val UNBOUND = -1
    }
}

enum class ControlId(
    val displayNameResource: Int,
    val inputAction: DingooInputAction?
) {
    DPad(io.github.uplush.dingoobox.R.string.virtual_control_d_pad, null),
    A(io.github.uplush.dingoobox.R.string.controller_action_a, DingooInputAction.A),
    B(io.github.uplush.dingoobox.R.string.controller_action_b, DingooInputAction.B),
    X(io.github.uplush.dingoobox.R.string.controller_action_x, DingooInputAction.X),
    Y(io.github.uplush.dingoobox.R.string.controller_action_y, DingooInputAction.Y),
    L(io.github.uplush.dingoobox.R.string.controller_action_l, DingooInputAction.L),
    R(io.github.uplush.dingoobox.R.string.controller_action_r, DingooInputAction.R),
    Start(io.github.uplush.dingoobox.R.string.controller_action_start, DingooInputAction.Start),
    Select(io.github.uplush.dingoobox.R.string.controller_action_select, DingooInputAction.Select)
}

data class ControlPosition(val x: Float, val y: Float)

data class ControlLayoutState(
    val positions: Map<ControlId, ControlPosition>,
    val scale: Float = 1f,
    val opacity: Float = 0.65f,
    val hidden: Set<ControlId> = emptySet(),
    val controlScales: Map<ControlId, Float> = emptyMap()
)

class ControlLayoutPreferences(
    context: Context,
    private val portrait: Boolean,
    gameKey: String? = null
) {
    private val suffix = buildString {
        append(if (portrait) "portrait" else "landscape")
        gameKey?.takeIf { it.isNotBlank() }?.let { append("_$it") }
    }
    private val preferences = context.getSharedPreferences("control_layout_$suffix", Context.MODE_PRIVATE)

    fun load(): ControlLayoutState {
        migrateLegacyDefaultLayout()
        val defaults = defaultPositions(portrait)
        val defaultOpacity = if (portrait) 1f else 0.65f
        val scale = preferences.getFloat("scale", 1f).coerceIn(0.7f, 1.4f)
        val positions = ControlId.entries.associateWith { id ->
            val fallback = defaults.getValue(id)
            ControlPosition(
                preferences.getFloat("${id.name}_x", fallback.x).coerceIn(0f, 1f),
                preferences.getFloat("${id.name}_y", fallback.y).coerceIn(0f, 1f)
            )
        }
        val hidden = ControlId.entries.filterTo(mutableSetOf()) {
            !preferences.getBoolean("${it.name}_visible", true)
        }
        return ControlLayoutState(
            positions = positions,
            scale = scale,
            opacity = preferences.getFloat("opacity", defaultOpacity).coerceIn(0f, 1f),
            hidden = hidden,
            controlScales = ControlId.entries.associateWith { id ->
                preferences.getFloat("${id.name}_size_scale", scale)
                    .coerceIn(0.7f, 1.4f)
            }
        )
    }

    fun save(layout: ControlLayoutState) {
        preferences.edit {
            layout.positions.forEach { (id, position) ->
                putFloat("${id.name}_x", position.x.coerceIn(0f, 1f))
                putFloat("${id.name}_y", position.y.coerceIn(0f, 1f))
            }
            ControlId.entries.forEach { putBoolean("${it.name}_visible", it !in layout.hidden) }
            ControlId.entries.forEach { id ->
                putFloat(
                    "${id.name}_size_scale",
                    (layout.controlScales[id] ?: layout.scale).coerceIn(0.7f, 1.4f)
                )
            }
            putFloat("scale", layout.scale.coerceIn(0.7f, 1.4f))
            putFloat("opacity", layout.opacity.coerceIn(0f, 1f))
            putInt(DEFAULT_LAYOUT_VERSION_KEY, DEFAULT_LAYOUT_VERSION)
        }
    }

    fun reset() = preferences.edit { clear() }

    private fun migrateLegacyDefaultLayout() {
        if (preferences.getInt(DEFAULT_LAYOUT_VERSION_KEY, 0) >= DEFAULT_LAYOUT_VERSION) return

        val hasStoredPositions = ControlId.entries.any { id ->
            preferences.contains("${id.name}_x") || preferences.contains("${id.name}_y")
        }
        val isUnmodifiedLegacyLayout = hasStoredPositions &&
            legacyDefaultPositionSets(portrait).any { legacyDefaults ->
                ControlId.entries.all { id ->
                    val legacy = legacyDefaults.getValue(id)
                    preferences.getFloat("${id.name}_x", legacy.x) == legacy.x &&
                        preferences.getFloat("${id.name}_y", legacy.y) == legacy.y
                }
        }

        preferences.edit {
            if (isUnmodifiedLegacyLayout) {
                ControlId.entries.forEach { id ->
                    remove("${id.name}_x")
                    remove("${id.name}_y")
                }
            }
            putInt(DEFAULT_LAYOUT_VERSION_KEY, DEFAULT_LAYOUT_VERSION)
        }
    }

    companion object {
        fun defaultPositions(portrait: Boolean): Map<ControlId, ControlPosition> =
            if (portrait) {
                mapOf(
                    ControlId.DPad to ControlPosition(0.27f, 0.82f),
                    ControlId.A to ControlPosition(0.88f, 0.82f),
                    ControlId.B to ControlPosition(0.73f, 0.88f),
                    ControlId.X to ControlPosition(0.73f, 0.76f),
                    ControlId.Y to ControlPosition(0.58f, 0.82f),
                    ControlId.L to ControlPosition(0.15f, 0.62f),
                    ControlId.R to ControlPosition(0.85f, 0.62f),
                    ControlId.Start to ControlPosition(0.58f, 0.62f),
                    ControlId.Select to ControlPosition(0.42f, 0.62f)
                )
            } else {
                mapOf(
                    ControlId.DPad to ControlPosition(0.16f, 0.74f),
                    ControlId.A to ControlPosition(0.91f, 0.74f),
                    ControlId.B to ControlPosition(0.84f, 0.88f),
                    ControlId.X to ControlPosition(0.84f, 0.60f),
                    ControlId.Y to ControlPosition(0.77f, 0.74f),
                    ControlId.L to ControlPosition(0.10f, 0.18f),
                    ControlId.R to ControlPosition(0.90f, 0.18f),
                    ControlId.Start to ControlPosition(0.58f, 0.88f),
                    ControlId.Select to ControlPosition(0.42f, 0.88f)
                )
            }

        fun migrateLegacyDefaultPositions(
            portrait: Boolean,
            positions: Map<ControlId, ControlPosition>
        ): Map<ControlId, ControlPosition> =
            if (positions in legacyDefaultPositionSets(portrait)) defaultPositions(portrait) else positions

        private fun legacyDefaultPositionSets(
            portrait: Boolean
        ): List<Map<ControlId, ControlPosition>> = listOf(
            legacyDefaultPositions(portrait),
            fix05DefaultPositions(portrait)
        )

        private fun fix05DefaultPositions(portrait: Boolean): Map<ControlId, ControlPosition> =
            if (portrait) {
                mapOf(
                    ControlId.DPad to ControlPosition(0.27f, 0.82f),
                    ControlId.A to ControlPosition(0.88f, 0.82f),
                    ControlId.B to ControlPosition(0.73f, 0.88f),
                    ControlId.X to ControlPosition(0.73f, 0.76f),
                    ControlId.Y to ControlPosition(0.58f, 0.82f),
                    ControlId.L to ControlPosition(0.15f, 0.48f),
                    ControlId.R to ControlPosition(0.85f, 0.48f),
                    ControlId.Start to ControlPosition(0.58f, 0.90f),
                    ControlId.Select to ControlPosition(0.39f, 0.90f)
                )
            } else {
                mapOf(
                    ControlId.DPad to ControlPosition(0.16f, 0.74f),
                    ControlId.A to ControlPosition(0.91f, 0.74f),
                    ControlId.B to ControlPosition(0.84f, 0.88f),
                    ControlId.X to ControlPosition(0.84f, 0.60f),
                    ControlId.Y to ControlPosition(0.77f, 0.74f),
                    ControlId.L to ControlPosition(0.10f, 0.18f),
                    ControlId.R to ControlPosition(0.90f, 0.18f),
                    ControlId.Start to ControlPosition(0.58f, 0.88f),
                    ControlId.Select to ControlPosition(0.42f, 0.88f)
                )
            }

        private fun legacyDefaultPositions(portrait: Boolean): Map<ControlId, ControlPosition> =
            if (portrait) {
                mapOf(
                    ControlId.DPad to ControlPosition(0.22f, 0.74f),
                    ControlId.A to ControlPosition(0.86f, 0.69f),
                    ControlId.B to ControlPosition(0.73f, 0.78f),
                    ControlId.X to ControlPosition(0.73f, 0.60f),
                    ControlId.Y to ControlPosition(0.60f, 0.69f),
                    ControlId.L to ControlPosition(0.15f, 0.48f),
                    ControlId.R to ControlPosition(0.85f, 0.48f),
                    ControlId.Start to ControlPosition(0.58f, 0.90f),
                    ControlId.Select to ControlPosition(0.39f, 0.90f)
                )
            } else {
                mapOf(
                    ControlId.DPad to ControlPosition(0.16f, 0.72f),
                    ControlId.A to ControlPosition(0.91f, 0.69f),
                    ControlId.B to ControlPosition(0.84f, 0.82f),
                    ControlId.X to ControlPosition(0.84f, 0.56f),
                    ControlId.Y to ControlPosition(0.77f, 0.69f),
                    ControlId.L to ControlPosition(0.10f, 0.18f),
                    ControlId.R to ControlPosition(0.90f, 0.18f),
                    ControlId.Start to ControlPosition(0.58f, 0.88f),
                    ControlId.Select to ControlPosition(0.42f, 0.88f)
                )
            }

        private const val DEFAULT_LAYOUT_VERSION_KEY = "default_layout_version"
        private const val DEFAULT_LAYOUT_VERSION = 3
    }
}
