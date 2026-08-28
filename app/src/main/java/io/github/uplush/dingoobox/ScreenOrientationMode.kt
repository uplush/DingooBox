package io.github.uplush.dingoobox

import android.content.pm.ActivityInfo

enum class ScreenOrientationMode(
    val preferenceValue: String,

    val displayNameResource: Int,
    val requestedOrientation: Int
) {
    DEVICE(
        preferenceValue = "device",

        displayNameResource =
            R.string.orientation_device,
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    ),
    PORTRAIT(
        preferenceValue = "portrait",

        displayNameResource =
            R.string.orientation_portrait,
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    ),
    LANDSCAPE(
        preferenceValue = "landscape",

        displayNameResource =
            R.string.orientation_landscape,
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    ),
    SENSOR(
        preferenceValue = "sensor",

        displayNameResource =
            R.string.orientation_sensor,
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
    );

    companion object {
        fun fromPreferenceValue(
            value: String?
        ): ScreenOrientationMode {
            return values()
                .firstOrNull { mode ->
                    mode.preferenceValue == value
                }
                ?: DEVICE
        }
    }
}