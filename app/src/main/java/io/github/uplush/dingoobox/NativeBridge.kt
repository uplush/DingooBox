package io.github.uplush.dingoobox

import android.graphics.Bitmap

object NativeBridge {
    init {
        System.loadLibrary("dingoo_jni_live")
    }

    external fun nativeInitialize(
        romData: ByteArray,
        romName: String,
        saveDirectory: String
    ): Boolean
    external fun nativeRunFrame(bitmap: Bitmap, audioBuffer: ShortArray): Int
    external fun nativeSetButton(buttonId: Int, pressed: Boolean)
    external fun nativeSaveState(path: String): Boolean
    external fun nativeLoadState(path: String): Boolean
    external fun nativeReset()
    external fun nativeDeinitialize()
}
