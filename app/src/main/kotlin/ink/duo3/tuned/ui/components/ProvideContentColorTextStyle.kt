package ink.duo3.tuned.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides LocalTextStyle.current.merge(textStyle),
        content = content,
    )
}
