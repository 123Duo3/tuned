@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package ink.duo3.tuned.ui.components.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/** Keeps the experimental Material button-group API behind Tuned's shared UI boundary. */
@Composable
fun TunedButtonGroup(
    modifier: Modifier = Modifier,
    expandedRatio: Float = ButtonGroupDefaults.ExpandedRatio,
    horizontalArrangement: Arrangement.Horizontal = ButtonGroupDefaults.HorizontalArrangement,
    content: TunedButtonGroupScope.() -> Unit,
) {
    ButtonGroup(
        overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
        modifier = modifier,
        expandedRatio = expandedRatio,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TunedButtonGroupScope(this).content()
    }
}

@Stable
class TunedButtonGroupScope internal constructor(
    private val delegate: ButtonGroupScope,
) {
    fun item(
        weight: Float,
        content: @Composable (TunedButtonGroupItem) -> Unit,
    ) {
        delegate.customItem(
            buttonGroupContent = {
                val interactionSource = remember { MutableInteractionSource() }
                val itemModifier =
                    with(delegate) {
                        Modifier.weight(weight).animateWidth(interactionSource)
                    }
                content(TunedButtonGroupItem(itemModifier, interactionSource))
            },
            menuContent = {},
        )
    }
}

@Stable
class TunedButtonGroupItem internal constructor(
    val modifier: Modifier,
    val interactionSource: MutableInteractionSource,
)

@Immutable
data class TunedButtonGroupButtonStyle(
    val colors: ButtonColors,
    val shape: Shape,
    val pressedShape: Shape,
    val contentPadding: PaddingValues,
)

@Composable
fun TunedButtonGroupButton(
    onClick: () -> Unit,
    item: TunedButtonGroupItem,
    style: TunedButtonGroupButtonStyle,
    modifier: Modifier = item.modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(shape = style.shape, pressedShape = style.pressedShape),
        modifier = modifier,
        colors = style.colors,
        contentPadding = style.contentPadding,
        interactionSource = item.interactionSource,
        content = content,
    )
}
