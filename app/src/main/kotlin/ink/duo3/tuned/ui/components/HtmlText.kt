package ink.duo3.tuned.ui.components

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import ink.duo3.tuned.core.ShowNotesTimestamps

/**
 * Renders feed show notes, which are HTML, into a tappable [TextView]. Compose has no
 * native HTML renderer, so we bridge to the platform's [HtmlCompat] parser and keep
 * links clickable via [LinkMovementMethod]. Colors are passed in so the host controls
 * theming — the view itself reads no [androidx.compose.material3.MaterialTheme].
 *
 * When [onTimestampClick] is supplied, `mm:ss` / `h:mm:ss` markers in the notes become
 * tappable and report their offset in milliseconds, so the host can seek playback.
 */
@Composable
fun HtmlText(
    html: String,
    textColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier,
    onTimestampClick: ((Long) -> Unit)? = null,
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
            val parsed = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            view.text =
                if (onTimestampClick != null) {
                    parsed.withTimestampLinks(linkColor.toArgb(), onTimestampClick)
                } else {
                    parsed
                }
        },
    )
}

private fun CharSequence.withTimestampLinks(
    color: Int,
    onClick: (Long) -> Unit,
): CharSequence {
    val markers = ShowNotesTimestamps.find(this)
    if (markers.isEmpty()) return this
    val builder = SpannableStringBuilder(this)
    markers.forEach { marker ->
        builder.setSpan(
            timestampSpan(marker.atMs, color, onClick),
            marker.range.first,
            marker.range.last + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    return builder
}

private fun timestampSpan(
    atMs: Long,
    color: Int,
    onClick: (Long) -> Unit,
): ClickableSpan =
    object : ClickableSpan() {
        override fun onClick(widget: View) = onClick(atMs)

        override fun updateDrawState(ds: TextPaint) {
            ds.color = color
            ds.isUnderlineText = false
        }
    }
