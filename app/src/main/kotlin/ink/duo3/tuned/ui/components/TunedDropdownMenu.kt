package ink.duo3.tuned.ui.components

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun rememberTunedDropdownMenuState(): TunedDropdownMenuState = remember { TunedDropdownMenuState() }

@Stable
class TunedDropdownMenuState internal constructor() {
    var expanded by mutableStateOf(false)

    internal var anchorBounds: IntRect? by mutableStateOf(null)
    internal var dragPositionInScreen: Offset? by mutableStateOf(null)
        private set
    internal var dragSelectedKey by mutableStateOf<Any?>(null)
        private set
    internal var dragReleasedKey by mutableStateOf<Any?>(null)
        private set

    private val itemRegistrations = mutableStateMapOf<Any, TunedDropdownMenuItemRegistration>()

    internal fun registerItem(
        key: Any,
        bounds: Rect,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        itemRegistrations[key] =
            TunedDropdownMenuItemRegistration(
                bounds = bounds,
                enabled = enabled,
                onClick = onClick,
            )
        dragPositionInScreen?.let(::updateDragSelection)
    }

    internal fun unregisterItem(key: Any) {
        itemRegistrations.remove(key)
        if (dragSelectedKey == key) {
            dragSelectedKey = null
        }
        if (dragReleasedKey == key) {
            dragReleasedKey = null
        }
    }

    internal fun updateDragSelection(positionInScreen: Offset) {
        dragReleasedKey = null
        dragPositionInScreen = positionInScreen
        dragSelectedKey =
            itemRegistrations.entries
                .firstOrNull { (_, item) -> item.enabled && item.bounds.contains(positionInScreen) }
                ?.key
    }

    internal fun endDragSelection() {
        val selectedKey = dragSelectedKey
        val selected = selectedKey?.let(itemRegistrations::get)
        dragPositionInScreen = null
        dragReleasedKey = selectedKey
        dragSelectedKey = null
        expanded = false
        selected?.onClick()
    }

    internal fun cancelDragSelection() {
        dragPositionInScreen = null
        dragSelectedKey = null
        dragReleasedKey = null
    }
}

private data class TunedDropdownMenuItemRegistration(
    val bounds: Rect,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun TunedDropdownMenuBox(
    state: TunedDropdownMenuState,
    anchor: @Composable BoxScope.(Modifier, openMenu: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable TunedDropdownMenuScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier.tunedDropdownMenuAnchor(state)) {
            anchor(Modifier) { state.expanded = true }
        }
        TunedDropdownMenu(
            state = state,
            onDismissRequest = { state.expanded = false },
            content = content,
        )
    }
}

fun Modifier.tunedDropdownMenuAnchor(state: TunedDropdownMenuState): Modifier =
    composed {
        val context = LocalContext.current
        val touchSlop = remember(context) { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }
        val longPressTimeoutMillis = remember { ViewConfiguration.getLongPressTimeout().toLong() }
        val disallowIntercept = remember { RequestDisallowInterceptTouchEvent() }
        val touchState =
            remember(state, touchSlop, disallowIntercept) {
                TunedDropdownForwardingTouchState(state, touchSlop, disallowIntercept)
            }

        LaunchedEffect(touchState.pointerDown) {
            if (!touchState.pointerDown) return@LaunchedEffect

            delay(longPressTimeoutMillis)
            touchState.onLongPressTimeout()
        }

        this
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                state.anchorBounds =
                    IntRect(
                        bounds.left.roundToInt(),
                        bounds.top.roundToInt(),
                        bounds.right.roundToInt(),
                        bounds.bottom.roundToInt(),
                    )
            }.pointerInteropFilter(
                requestDisallowInterceptTouchEvent = disallowIntercept,
                onTouchEvent = { event -> touchState.onTouchEvent(event) },
            )
    }

@Composable
private fun TunedDropdownMenu(
    state: TunedDropdownMenuState,
    onDismissRequest: () -> Unit,
    content: @Composable TunedDropdownMenuScope.() -> Unit,
) {
    val density = LocalDensity.current
    val positionProvider =
        remember(density, state.anchorBounds) {
            TunedDropdownMenuPositionProvider(
                explicitAnchorBounds = state.anchorBounds,
                density = density,
                contentOffset = DpOffset(x = 0.dp, y = 8.dp),
            )
        }
    val revealState = rememberTunedDropdownRevealState(state.expanded)
    val reveal = revealState.reveal

    if (!revealState.visible) return

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties =
            PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        val shape = tunedRoundedCornerShape(MENU_CORNER_RADIUS)
        val scope = remember(state) { TunedDropdownMenuScope(state) }
        scope.resetItemIndex()

        TunedDropdownMenuSurface(
            revealProgress = reveal.value,
            shape = shape,
        ) {
            Column(
                modifier =
                    Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(max = 280.dp)
                        .padding(ITEM_RIPPLE_MARGIN)
                        .clip(tunedRoundedCornerShape(ITEM_RIPPLE_CORNER_RADIUS))
                        .verticalScroll(rememberScrollState()),
            ) {
                scope.content()
            }
        }
    }
}

@Composable
private fun rememberTunedDropdownRevealState(expanded: Boolean): TunedDropdownMenuRevealState {
    val reveal = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) {
            visible = true
            reveal.animateTo(1f, tween(MENU_REVEAL_DURATION_MILLIS, easing = LinearEasing))
        } else if (visible) {
            reveal.animateTo(0f, tween(MENU_DISMISS_DURATION_MILLIS, easing = LinearEasing))
            visible = false
        } else {
            reveal.snapTo(0f)
        }
    }
    return TunedDropdownMenuRevealState(reveal = reveal, visible = visible)
}

private data class TunedDropdownMenuRevealState(
    val reveal: Animatable<Float, *>,
    val visible: Boolean,
)

@Composable
private fun TunedDropdownMenuSurface(
    revealProgress: Float,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    val shadowProgress =
        SHADOW_EASING.transform(
            ((revealProgress - SHADOW_REVEAL_DELAY_FRACTION) / (1f - SHADOW_REVEAL_DELAY_FRACTION))
                .coerceIn(0f, 1f),
        )
    Box(
        modifier =
            Modifier
                .padding(SHADOW_PADDING)
                .dropShadow(
                    shape = shape,
                    shadow =
                        Shadow(
                            radius = 18.dp,
                            color = Color.Black,
                            offset = DpOffset(0.dp, 4.dp),
                            alpha = 0.1f * shadowProgress,
                        ),
                ).clip(shape)
                .tunedDropdownRevealClip(revealProgress)
                .background(MaterialTheme.colorScheme.surfaceBright),
    ) {
        content()
    }
}

private fun Modifier.tunedDropdownRevealClip(progress: Float): Modifier =
    drawWithContent {
        val clippedProgress = progress.coerceIn(0f, 1f)
        val widthProgress =
            MENU_WIDTH_EASING.transform((clippedProgress / 0.84f).coerceIn(0f, 1f))
        val heightProgress =
            MENU_HEIGHT_EASING.transform(((clippedProgress - 0.08f) / 0.92f).coerceIn(0f, 1f))
        val left = size.width * (1f - widthProgress)
        val bottom = size.height * heightProgress
        val cornerRadius =
            MENU_CORNER_RADIUS
                .toPx()
                .coerceAtMost((size.width - left) / 2f)
                .coerceAtMost(bottom / 2f)
        val revealSize = Size(width = size.width - left, height = bottom)
        val revealPath =
            Path().apply {
                addPath(
                    tunedAnimatedRoundedCornerShape(cornerRadius.toDp())
                        .toPath(revealSize, layoutDirection, this@drawWithContent),
                    Offset(left, 0f),
                )
            }
        clipPath(revealPath) {
            this@drawWithContent.drawContent()
        }
    }

@Stable
class TunedDropdownMenuScope internal constructor(
    private val state: TunedDropdownMenuState,
) {
    private var itemIndex = 0

    internal fun resetItemIndex() {
        itemIndex = 0
    }

    @Composable
    fun Item(
        text: @Composable () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        leadingIcon: (@Composable () -> Unit)? = null,
    ) {
        val key = remember { Any() }
        val index = itemIndex++
        val currentOnClick by rememberUpdatedState(onClick)
        val itemAlpha = remember { Animatable(0f) }
        LaunchedEffect(key) {
            delay(ITEM_REVEAL_DELAY_MILLIS + index * ITEM_STAGGER_MILLIS)
            itemAlpha.animateTo(1f, tween(ITEM_REVEAL_DURATION_MILLIS))
        }
        val isDragSelected = state.dragSelectedKey == key
        val interactionSource = remember { MutableInteractionSource() }
        var itemBounds by remember { mutableStateOf<Rect?>(null) }
        val dragPositionInScreen = state.dragPositionInScreen
        val dragPressPosition =
            if (isDragSelected && dragPositionInScreen != null && itemBounds != null) {
                dragPositionInScreen - itemBounds!!.topLeft
            } else {
                null
            }
        val visuals = rememberTunedDropdownItemVisuals(enabled, itemAlpha.value, interactionSource)
        TunedDropdownDragPressInteraction(
            isDragSelected = isDragSelected,
            isDragReleased = state.dragReleasedKey == key,
            enabled = enabled,
            pressPosition = dragPressPosition ?: Offset.Zero,
            interactionSource = interactionSource,
        )
        DisposableEffect(key) { onDispose { state.unregisterItem(key) } }

        TunedDropdownMenuItemRow(
            modifier = modifier,
            visuals = visuals,
            leadingIcon = leadingIcon,
            text = text,
            handlers =
                TunedDropdownMenuItemHandlers(
                    onPositioned = { coordinates ->
                        val bounds = coordinates.boundsInScreen()
                        itemBounds = bounds
                        state.registerItem(
                            key = key,
                            bounds = bounds,
                            enabled = enabled,
                            onClick = currentOnClick,
                        )
                    },
                    onClick = {
                        state.expanded = false
                        currentOnClick()
                    },
                ),
        )
    }

    @Composable
    fun Divider(modifier: Modifier = Modifier) {
        HorizontalDivider(
            modifier =
                modifier
                    .padding(vertical = 8.dp)
                    .height(1.dp),
            color = LocalContentColor.current.copy(alpha = DIVIDER_ALPHA),
        )
    }
}

@Composable
private fun rememberTunedDropdownItemVisuals(
    enabled: Boolean,
    itemAlpha: Float,
    interactionSource: MutableInteractionSource,
): TunedDropdownMenuItemVisuals {
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
        }
    return TunedDropdownMenuItemVisuals(
        enabled = enabled,
        interactionSource = interactionSource,
        colors =
            TunedDropdownMenuItemColors(
                background = Color.Transparent,
                content = contentColor.copy(alpha = contentColor.alpha * itemAlpha),
            ),
    )
}

private data class TunedDropdownMenuItemColors(
    val background: Color,
    val content: Color,
)

private data class TunedDropdownMenuItemVisuals(
    val enabled: Boolean,
    val interactionSource: MutableInteractionSource,
    val colors: TunedDropdownMenuItemColors,
)

private data class TunedDropdownMenuItemHandlers(
    val onPositioned: (LayoutCoordinates) -> Unit,
    val onClick: () -> Unit,
)

@Composable
private fun TunedDropdownMenuItemRow(
    visuals: TunedDropdownMenuItemVisuals,
    leadingIcon: (@Composable () -> Unit)?,
    text: @Composable () -> Unit,
    handlers: TunedDropdownMenuItemHandlers,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding =
        if (leadingIcon != null) {
            ITEM_ICON_START_PADDING
        } else {
            ITEM_CONTENT_HORIZONTAL_PADDING
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = 112.dp, minHeight = 48.dp)
                .onGloballyPositioned(handlers.onPositioned)
                .background(visuals.colors.background)
                .clickable(
                    interactionSource = visuals.interactionSource,
                    indication = LocalIndication.current,
                    enabled = visuals.enabled,
                    role = Role.Button,
                    onClick = handlers.onClick,
                ).padding(
                    start = horizontalPadding,
                    top = ITEM_CONTENT_VERTICAL_PADDING,
                    end = ITEM_CONTENT_HORIZONTAL_PADDING,
                    bottom = ITEM_CONTENT_VERTICAL_PADDING,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvideContentColorTextStyle(
            contentColor = visuals.colors.content,
            textStyle = MaterialTheme.typography.labelLarge,
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier.size(ITEM_ICON_SIZE),
                    contentAlignment = Alignment.Center,
                ) {
                    leadingIcon()
                }
                Spacer(modifier = Modifier.width(ITEM_ICON_TEXT_SPACING))
            }
            text()
        }
    }
}

private class TunedDropdownMenuPositionProvider(
    private val explicitAnchorBounds: IntRect?,
    private val density: Density,
    private val contentOffset: DpOffset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = explicitAnchorBounds ?: anchorBounds
        val horizontalOffset = with(density) { contentOffset.x.roundToPx() }
        val verticalOffset = with(density) { contentOffset.y.roundToPx() }
        val shadowPadding = with(density) { SHADOW_PADDING.roundToPx() }
        val margin = with(density) { 8.dp.roundToPx() }
        val panelWidth = popupContentSize.width - shadowPadding * 2
        val panelHeight = popupContentSize.height - shadowPadding * 2

        val preferredPanelLeft =
            if (layoutDirection == LayoutDirection.Ltr) {
                anchor.right - panelWidth + horizontalOffset
            } else {
                anchor.left - horizontalOffset
            }
        val maxPanelLeft = windowSize.width - margin - panelWidth
        val panelLeft =
            if (maxPanelLeft >= margin) {
                preferredPanelLeft.coerceIn(margin, maxPanelLeft)
            } else {
                margin
            }

        val belowPanelTop = anchor.bottom + verticalOffset
        val abovePanelTop = anchor.top - panelHeight - verticalOffset
        val maxPanelTop = windowSize.height - margin - panelHeight
        val panelTop =
            when {
                belowPanelTop + panelHeight <= windowSize.height - margin -> belowPanelTop
                abovePanelTop >= margin -> abovePanelTop
                else -> maxPanelTop.coerceAtLeast(margin)
            }

        return IntOffset(
            x = panelLeft - shadowPadding,
            y = panelTop - shadowPadding,
        )
    }
}

private const val MENU_REVEAL_DURATION_MILLIS = 300
private const val MENU_DISMISS_DURATION_MILLIS = 150
private const val ITEM_REVEAL_DELAY_MILLIS = 15L
private const val ITEM_REVEAL_DURATION_MILLIS = 300
private const val ITEM_STAGGER_MILLIS = 30L
private const val DISABLED_ALPHA = 0.38f
private const val DIVIDER_ALPHA = 0.12f
private val MENU_WIDTH_EASING = CubicBezierEasing(0.0f, 1f, 0f, 1f)
private val MENU_HEIGHT_EASING = CubicBezierEasing(0.0f, 0.5f, 0f, 1f)
private val MENU_CORNER_RADIUS = 24.dp
private val ITEM_RIPPLE_MARGIN = 8.dp
private val ITEM_RIPPLE_CORNER_RADIUS = 16.dp
private val ITEM_CONTENT_HORIZONTAL_PADDING = 16.dp
private val ITEM_CONTENT_VERTICAL_PADDING = 12.dp
private val ITEM_ICON_SIZE = 24.dp
private val ITEM_ICON_START_PADDING = 12.dp
private val ITEM_ICON_TEXT_SPACING = 12.dp
private val SHADOW_PADDING = 16.dp
private const val SHADOW_REVEAL_DELAY_FRACTION = 0.15f
private val SHADOW_EASING = CubicBezierEasing(0.6f, 0f, 0.6f, 1f)
