package ink.duo3.tuned.ui.components

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

/**
 * Renders feed show notes, which are HTML, into a tappable [TextView]. Compose has no
 * native HTML renderer, so we bridge to the platform's [HtmlCompat] parser and keep
 * links clickable via [LinkMovementMethod]. Colors are passed in so the host controls
 * theming — the view itself reads no [androidx.compose.material3.MaterialTheme].
 */
@Composable
fun HtmlText(
    html: String,
    textColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { view ->
            view.setTextColor(textColor.toArgb())
            view.setLinkTextColor(linkColor.toArgb())
            view.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        },
    )
}
