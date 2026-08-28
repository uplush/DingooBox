package io.github.uplush.dingoobox

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/** MiNiQ's game-audio focus lifecycle with only the package adapted. */
internal class GameAudioFocus(
    context: Context
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val focusListener = AudioManager.OnAudioFocusChangeListener {
        // MiNiQ currently reserves focus-change behavior for a later update.
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var focusRequest: AudioFocusRequest? = null

    fun request(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = focusRequest
                ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(focusListener)
                    .build()
                    .also { focusRequest = it }

            audioManager.requestAudioFocus(request) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        Log.i(AUDIO_LOG_TAG, "Audio focus request: granted=$granted")
        return granted
    }

    fun abandon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        Log.i(AUDIO_LOG_TAG, "Audio focus abandoned")
    }

    private companion object {
        const val AUDIO_LOG_TAG = "DingooAudio"
    }
}
