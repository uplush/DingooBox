package io.github.uplush.dingoobox

import android.content.Context
import android.widget.Toast

/**
 * MiNiQ uses the platform short Toast for ordinary transient operation
 * feedback. Keeping the call in one place prevents individual screens from
 * drifting back to Snackbar or custom Compose cards.
 */
internal fun showMiniQToast(
    context: Context,
    message: String
) {
    Toast.makeText(
        context,
        message,
        Toast.LENGTH_SHORT
    ).show()
}
