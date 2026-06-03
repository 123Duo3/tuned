package ink.duo3.tuned.ui.components.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.ui.components.ProvideContentColorTextStyle

@DslMarker
annotation class SettingItemGroupScopeMarker

@SettingItemGroupScopeMarker
interface SettingItemGroupScope {
    @Composable
    fun AnimatedVisibility(
        visible: Boolean,
        modifier: Modifier = Modifier,
        enter: EnterTransition = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit: ExitTransition = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        content: @Composable AnimatedVisibilityScope.() -> Unit,
    )
}

private object SettingItemGroupScopeInstance : SettingItemGroupScope {
    @Composable
    override fun AnimatedVisibility(
        visible: Boolean,
        modifier: Modifier,
        enter: EnterTransition,
        exit: ExitTransition,
        content: @Composable AnimatedVisibilityScope.() -> Unit,
    ) {
        var hasComposed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            hasComposed = true
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = if (hasComposed) enter else EnterTransition.None,
            exit = exit,
            content = content,
        )
    }
}

@Composable
fun SettingItemGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable SettingItemGroupScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .then(modifier),
    ) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Layout(
            content = { SettingItemGroupScopeInstance.content() },
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val spacing = 2.dp.roundToPx()
            val baseItemHeight = 48.dp.toPx()

            var yPosition = 0
            var hasVisibleBefore = false
            val positions =
                placeables.map { placeable ->
                    val scale = (placeable.height / baseItemHeight).coerceIn(0f, 1f)
                    if (hasVisibleBefore && scale > 0f) {
                        yPosition += (spacing * scale).toInt()
                    }
                    val position = yPosition
                    yPosition += placeable.height
                    if (scale > 0f) {
                        hasVisibleBefore = true
                    }
                    position
                }

            layout(constraints.maxWidth, yPosition) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(0, positions[index])
                }
            }
        }

        if (footer != null) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                ProvideContentColorTextStyle(
                    textStyle = MaterialTheme.typography.bodyMedium,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    content = footer,
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
fun SettingItemWithSwitch(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    SettingItem(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        onClick = { onCheckedChange(!checked) },
        endAction = {
            SwitchWithIcon(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
        enabled = enabled,
    )
}

@Composable
fun SwitchWithIcon(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Clear,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
        enabled = enabled,
        colors =
            SwitchDefaults.colors(
                uncheckedIconColor = uncheckedTrackColor,
            ),
    )
}

@Composable
@Suppress("LongMethod", "LongParameterList")
fun SettingItem(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(4.dp),
    colors: CardColors =
        CardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            disabledContentColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    enabled: Boolean = true,
) {
    Surface(
        modifier =
            modifier
                .clip(shape)
                .let { clipped ->
                    if (onClick != null) {
                        clipped.clickable(enabled = enabled, onClick = onClick)
                    } else {
                        clipped
                    }
                },
        shape = shape,
        color = colors.containerColor,
        contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
    ) {
        Column(contentModifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            color = Color.Transparent,
                            contentColor =
                                if (enabled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            content = icon,
                        )
                    }
                }
                Column(
                    modifier =
                        Modifier
                            .heightIn(min = 48.dp)
                            .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    ProvideContentColorTextStyle(
                        textStyle = MaterialTheme.typography.titleMedium,
                        contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
                        content = title,
                    )
                    if (description != null) {
                        ProvideContentColorTextStyle(
                            textStyle = MaterialTheme.typography.bodyMedium,
                            contentColor =
                                if (enabled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    colors.disabledContentColor
                                },
                            content = description,
                        )
                    }
                }
                endAction?.invoke()
            }
            bottomAction?.invoke()
        }
    }
}
