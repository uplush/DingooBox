package io.github.uplush.dingoobox

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

private data class ControlDimensions(
    val width: Dp,
    val height: Dp
)

private val D_PAD_SIZE = 170.dp
private val STANDARD_BUTTON_SIZE = 56.dp
private val FACE_BUTTON_CENTER_OFFSET = 57.dp
private val PORTRAIT_SYSTEM_ROW_OFFSET = 38.dp
private val FACE_BUTTON_IDS = setOf(
    ControlId.A,
    ControlId.B,
    ControlId.X,
    ControlId.Y
)
private val PORTRAIT_SYSTEM_BUTTON_IDS = setOf(
    ControlId.L,
    ControlId.R,
    ControlId.Start,
    ControlId.Select
)

@Composable
fun VirtualControls(
    layout: ControlLayoutState,
    vibration: Boolean,
    portrait: Boolean,
    onButton: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var area by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val faceButtonCenterOffsetPx = with(density) { FACE_BUTTON_CENTER_OFFSET.toPx() }
    val portraitSystemRowOffsetPx = with(density) { PORTRAIT_SYSTEM_ROW_OFFSET.toPx() }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { area = it }
    ) {
        ControlId.entries.filterNot { it in layout.hidden }.forEach { id ->
            val storedPosition = layout.positions[id] ?: return@forEach
            val position = resolveDefaultControlPosition(
                id = id,
                storedPosition = storedPosition,
                portrait = portrait,
                areaWidthPx = area.width.toFloat(),
                areaHeightPx = area.height.toFloat(),
                faceButtonCenterOffsetPx = faceButtonCenterOffsetPx,
                portraitSystemRowOffsetPx = portraitSystemRowOffsetPx
            )
            val scale = layout.controlScales[id] ?: layout.scale
            val dimensions = controlDimensions(id, scale)
            val offset = offsetFor(
                position = position,
                area = area,
                widthPx = with(density) { dimensions.width.toPx() },
                heightPx = with(density) { dimensions.height.toPx() }
            )

            when (id) {
                ControlId.DPad -> RuntimeDPad(
                    offset = offset,
                    scale = scale,
                    opacity = layout.opacity,
                    vibration = vibration,
                    onButton = onButton
                )

                else -> RuntimeButton(
                    id = id,
                    offset = offset,
                    scale = scale,
                    opacity = layout.opacity,
                    vibration = vibration,
                    onButton = onButton
                )
            }
        }
    }
}

@Composable
private fun RuntimeDPad(
    offset: IntOffset,
    scale: Float,
    opacity: Float,
    vibration: Boolean,
    onButton: (Int, Boolean) -> Unit
) {
    Box(
        Modifier
            .offset { offset }
            .size(D_PAD_SIZE * scale)
    ) {
        RuntimeKey("▲", DingooInputAction.Up, Modifier.align(Alignment.TopCenter), STANDARD_BUTTON_SIZE * scale, opacity, vibration, onButton)
        RuntimeKey("▼", DingooInputAction.Down, Modifier.align(Alignment.BottomCenter), STANDARD_BUTTON_SIZE * scale, opacity, vibration, onButton)
        RuntimeKey("◀", DingooInputAction.Left, Modifier.align(Alignment.CenterStart), STANDARD_BUTTON_SIZE * scale, opacity, vibration, onButton)
        RuntimeKey("▶", DingooInputAction.Right, Modifier.align(Alignment.CenterEnd), STANDARD_BUTTON_SIZE * scale, opacity, vibration, onButton)
    }
}

@Composable
private fun RuntimeButton(
    id: ControlId,
    offset: IntOffset,
    scale: Float,
    opacity: Float,
    vibration: Boolean,
    onButton: (Int, Boolean) -> Unit
) {
    val action = id.inputAction ?: return
    RuntimeKey(
        label = appStringResource(id.displayNameResource),
        action = action,
        modifier = Modifier.offset { offset },
        size = buttonBaseSize(id) * scale,
        opacity = opacity,
        vibration = vibration,
        onButton = onButton,
        color = buttonColor(id),
        pill = isPillButton(id)
    )
}

@Composable
private fun RuntimeKey(
    label: String,
    action: DingooInputAction,
    modifier: Modifier,
    size: Dp,
    opacity: Float,
    vibration: Boolean,
    onButton: (Int, Boolean) -> Unit,
    color: Color = Color(0xFF454A52),
    pill: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box(
        modifier
            .size(if (pill) size * 1.35f else size, size)
            .alpha(if (pressed) 1f else opacity)
            .clip(if (pill) RoundedCornerShape(size / 2) else CircleShape)
            .background(color)
            .pointerInput(action, vibration) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (vibration) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onButton(action.retroButtonId, true)
                    waitForUpOrCancellation()
                    onButton(action.retroButtonId, false)
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditorControl(
    id: ControlId,
    scale: Float,
    modifier: Modifier
) {
    when (id) {
        ControlId.DPad -> Box(
            modifier = modifier.size(D_PAD_SIZE * scale)
        ) {
            EditorKey("▲", Modifier.align(Alignment.TopCenter), STANDARD_BUTTON_SIZE * scale)
            EditorKey("▼", Modifier.align(Alignment.BottomCenter), STANDARD_BUTTON_SIZE * scale)
            EditorKey("◀", Modifier.align(Alignment.CenterStart), STANDARD_BUTTON_SIZE * scale)
            EditorKey("▶", Modifier.align(Alignment.CenterEnd), STANDARD_BUTTON_SIZE * scale)
        }

        else -> {
            val size = buttonBaseSize(id) * scale
            Box(
                modifier = modifier
                    .size(if (isPillButton(id)) size * 1.35f else size, size)
                    .clip(if (isPillButton(id)) RoundedCornerShape(size / 2) else CircleShape)
                    .background(buttonColor(id)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    appStringResource(id.displayNameResource),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EditorKey(
    label: String,
    modifier: Modifier,
    size: Dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF454A52)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun offsetFor(
    position: ControlPosition,
    area: IntSize,
    widthPx: Float,
    heightPx: Float
): IntOffset = IntOffset(
    x = (position.x * area.width - widthPx / 2f).roundToInt(),
    y = (position.y * area.height - heightPx / 2f).roundToInt()
)

private fun resolveDefaultControlPosition(
    id: ControlId,
    storedPosition: ControlPosition,
    portrait: Boolean,
    areaWidthPx: Float,
    areaHeightPx: Float,
    faceButtonCenterOffsetPx: Float,
    portraitSystemRowOffsetPx: Float
): ControlPosition {
    if (
        areaWidthPx <= 0f ||
        areaHeightPx <= 0f
    ) {
        return storedPosition
    }

    val defaults = ControlLayoutPreferences.defaultPositions(portrait)
    if (storedPosition != defaults[id]) return storedPosition

    if (id in FACE_BUTTON_IDS) {
        val dPadCenter = defaults.getValue(ControlId.DPad)
        val faceButtonCenterX = 1f - dPadCenter.x
        val faceButtonCenterY = dPadCenter.y
        val horizontalOffset = faceButtonCenterOffsetPx / areaWidthPx
        val verticalOffset = faceButtonCenterOffsetPx / areaHeightPx

        return when (id) {
            ControlId.A -> ControlPosition(faceButtonCenterX + horizontalOffset, faceButtonCenterY)
            ControlId.B -> ControlPosition(faceButtonCenterX, faceButtonCenterY + verticalOffset)
            ControlId.X -> ControlPosition(faceButtonCenterX, faceButtonCenterY - verticalOffset)
            ControlId.Y -> ControlPosition(faceButtonCenterX - horizontalOffset, faceButtonCenterY)
            else -> storedPosition
        }
    }

    if (portrait && id in PORTRAIT_SYSTEM_BUTTON_IDS) {
        val gameBottomPx = areaHeightPx * 0.36f + areaWidthPx * 3f / 8f
        return storedPosition.copy(
            y = ((gameBottomPx + portraitSystemRowOffsetPx) / areaHeightPx)
                .coerceIn(0f, 1f)
        )
    }

    return storedPosition
}

private fun controlDimensions(id: ControlId, scale: Float): ControlDimensions = when (id) {
    ControlId.DPad -> ControlDimensions(D_PAD_SIZE * scale, D_PAD_SIZE * scale)
    else -> {
        val height = buttonBaseSize(id) * scale
        ControlDimensions(
            width = if (isPillButton(id)) height * 1.35f else height,
            height = height
        )
    }
}

private fun buttonBaseSize(id: ControlId): Dp = when (id) {
    ControlId.L, ControlId.R -> 44.dp
    ControlId.Start, ControlId.Select -> 40.dp
    else -> STANDARD_BUTTON_SIZE
}

private fun isPillButton(id: ControlId): Boolean = id in setOf(
    ControlId.L,
    ControlId.R,
    ControlId.Start,
    ControlId.Select
)

private fun buttonColor(id: ControlId): Color = when (id) {
    ControlId.A -> Color(0xFF946190)
    ControlId.B -> Color(0xFF7B9ACB)
    ControlId.X -> Color(0xFF4B6B6C)
    ControlId.Y -> Color(0xFF6F2835)
    else -> Color(0xFF454A52)
}

@Composable
fun VirtualControlEditorScreen(
    initialPortrait: Boolean,
    gameKey: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val orientation = LocalConfiguration.current.orientation
    val editorPortrait = when (orientation) {
        Configuration.ORIENTATION_PORTRAIT -> true
        Configuration.ORIENTATION_LANDSCAPE -> false
        else -> initialPortrait
    }
    val preferences = remember(context, editorPortrait, gameKey) {
        ControlLayoutPreferences(context, editorPortrait, gameKey)
    }
    var layout by remember(preferences) { mutableStateOf(preferences.load()) }
    var resizingControls by remember(preferences) { mutableStateOf(false) }

    fun saveControlsAndExit() {
        preferences.save(layout)
        onBack()
    }

    BackHandler(onBack = ::saveControlsAndExit)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val dragGridSizePx = with(density) { 8.dp.toPx() }
        val resizeStepDistancePx = with(density) { 24.dp.toPx() }
        val faceButtonCenterOffsetPx = with(density) { FACE_BUTTON_CENTER_OFFSET.toPx() }
        val portraitSystemRowOffsetPx = with(density) { PORTRAIT_SYSTEM_ROW_OFFSET.toPx() }
        val visibleControls = ControlId.entries.filterNot { it in layout.hidden }.toSet()

        fun editorModifier(
            id: ControlId,
            dimensions: ControlDimensions
        ): Modifier {
            val storedPosition = layout.positions[id] ?: ControlPosition(0.5f, 0.5f)
            val position = resolveDefaultControlPosition(
                id = id,
                storedPosition = storedPosition,
                portrait = editorPortrait,
                areaWidthPx = widthPx,
                areaHeightPx = heightPx,
                faceButtonCenterOffsetPx = faceButtonCenterOffsetPx,
                portraitSystemRowOffsetPx = portraitSystemRowOffsetPx
            )
            val controlWidthPx = with(density) { dimensions.width.toPx() }
            val controlHeightPx = with(density) { dimensions.height.toPx() }
            val halfWidthFraction = (controlWidthPx / 2f / widthPx).coerceIn(0f, 0.49f)
            val halfHeightFraction = (controlHeightPx / 2f / heightPx).coerceIn(0f, 0.49f)
            var result = Modifier
                .offset(
                    x = maxWidth * position.x - dimensions.width / 2,
                    y = maxHeight * position.y - dimensions.height / 2
                )
                .alpha(layout.opacity)

            result = if (resizingControls) {
                result.pointerInput(id, resizingControls, resizeStepDistancePx) {
                    var accumulatedHorizontalDrag = 0f
                    detectDragGestures(
                        onDragStart = { accumulatedHorizontalDrag = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedHorizontalDrag += dragAmount.x
                            while (abs(accumulatedHorizontalDrag) >= resizeStepDistancePx) {
                                val direction = if (accumulatedHorizontalDrag > 0f) 1 else -1
                                val currentScale = layout.controlScales[id] ?: layout.scale
                                val currentStep = round(currentScale * 10f).toInt()
                                val nextScale = (currentStep + direction).coerceIn(7, 14) / 10f
                                layout = layout.copy(
                                    controlScales = layout.controlScales + (id to nextScale)
                                )
                                accumulatedHorizontalDrag -= direction * resizeStepDistancePx
                            }
                        }
                    )
                }
            } else {
                result.pointerInput(
                    id,
                    widthPx,
                    heightPx,
                    dragGridSizePx,
                    dimensions.width,
                    dimensions.height
                ) {
                    var dragCenterXPx = 0f
                    var dragCenterYPx = 0f
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragCenterXPx = position.x * widthPx
                            dragCenterYPx = position.y * heightPx
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val minimumCenterXPx = halfWidthFraction * widthPx
                            val maximumCenterXPx = (1f - halfWidthFraction) * widthPx
                            val minimumCenterYPx = halfHeightFraction * heightPx
                            val maximumCenterYPx = (1f - halfHeightFraction) * heightPx

                            dragCenterXPx = (dragCenterXPx + dragAmount.x)
                                .coerceIn(minimumCenterXPx, maximumCenterXPx)
                            dragCenterYPx = (dragCenterYPx + dragAmount.y)
                                .coerceIn(minimumCenterYPx, maximumCenterYPx)

                            val snappedCenterXPx = (
                                round(dragCenterXPx / dragGridSizePx) * dragGridSizePx
                            ).coerceIn(minimumCenterXPx, maximumCenterXPx)
                            val snappedCenterYPx = (
                                round(dragCenterYPx / dragGridSizePx) * dragGridSizePx
                            ).coerceIn(minimumCenterYPx, maximumCenterYPx)

                            layout = layout.copy(
                                positions = layout.positions + (
                                    id to ControlPosition(
                                        x = (snappedCenterXPx / widthPx).coerceIn(
                                            halfWidthFraction,
                                            1f - halfWidthFraction
                                        ),
                                        y = (snappedCenterYPx / heightPx).coerceIn(
                                            halfHeightFraction,
                                            1f - halfHeightFraction
                                        )
                                    )
                                )
                            )
                        }
                    )
                }
            }
            return result
        }

        visibleControls.forEach { id ->
            val scale = layout.controlScales[id] ?: layout.scale
            val dimensions = controlDimensions(id, scale)
            EditorControl(
                id = id,
                scale = scale,
                modifier = editorModifier(id, dimensions)
            )
        }

        VirtualControlEditorMenu(
            opacity = layout.opacity,
            visibleControls = visibleControls,
            onOpacityChange = { layout = layout.copy(opacity = it) },
            onVisibleControlsChange = { controls ->
                layout = layout.copy(
                    hidden = ControlId.entries.filterNotTo(mutableSetOf()) {
                        it in controls
                    }
                )
            },
            onResizeModeChange = { resizingControls = it },
            onReset = {
                layout = ControlLayoutState(
                    positions = ControlLayoutPreferences.defaultPositions(editorPortrait),
                    scale = 1f,
                    opacity = if (editorPortrait) 1f else 0.65f,
                    hidden = emptySet(),
                    controlScales = ControlId.entries.associateWith { 1f }
                )
                resizingControls = false
            },
            onExit = ::saveControlsAndExit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (editorPortrait) 28.dp else 16.dp)
        )
    }
}
