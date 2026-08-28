package io.github.uplush.dingoobox

enum class AppThemeMode(
    val preferenceValue: String,

    val displayNameResource: Int
) {
    SYSTEM(
        preferenceValue = "system",

        displayNameResource =
            R.string.theme_system
    ),
    LIGHT(
        preferenceValue = "light",

        displayNameResource =
            R.string.theme_light
    ),
    DARK(
        preferenceValue = "dark",

        displayNameResource =
            R.string.theme_dark
    );

    companion object {
        fun fromPreferenceValue(
            value: String?
        ): AppThemeMode {
            return values()
                .firstOrNull { mode ->
                    mode.preferenceValue == value
                }
                ?: SYSTEM
        }
    }
}