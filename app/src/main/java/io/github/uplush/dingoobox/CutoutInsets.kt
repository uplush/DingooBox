package io.github.uplush.dingoobox

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
fun Modifier.symmetricCutoutPadding(
    fraction: Float = 1f
): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val cutoutInsets = WindowInsets.displayCutout

    val leftInset =
        cutoutInsets.getLeft(
            density,
            layoutDirection
        )

    val rightInset =
        cutoutInsets.getRight(
            density,
            layoutDirection
        )

    val symmetricInset =
        maxOf(
            leftInset,
            rightInset
        )

	val symmetricInsetDp =
		with(density) {
			symmetricInset.toDp()
		} * fraction.coerceIn(0f, 1f)

    return this.padding(
        horizontal = symmetricInsetDp
    )
}

@Composable
fun Modifier.symmetricSafeDrawingPadding(): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safeInsets = WindowInsets.safeDrawing

    val leftInset =
        safeInsets.getLeft(
            density,
            layoutDirection
        )

    val rightInset =
        safeInsets.getRight(
            density,
            layoutDirection
        )

    val topInset =
        safeInsets.getTop(density)

    val bottomInset =
        safeInsets.getBottom(density)

    val symmetricHorizontalInset =
        maxOf(
            leftInset,
            rightInset
        )

    val symmetricVerticalInset =
        maxOf(
            topInset,
            bottomInset
        )

    val horizontalPadding =
        with(density) {
            symmetricHorizontalInset.toDp()
        }

    val verticalPadding =
        with(density) {
            symmetricVerticalInset.toDp()
        }

    return this.padding(
        horizontal = horizontalPadding,
        vertical = verticalPadding
    )
}
