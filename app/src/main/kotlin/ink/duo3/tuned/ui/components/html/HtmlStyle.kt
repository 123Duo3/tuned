package ink.duo3.tuned.ui.components.html

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tunable styling for [HtmlText]. Every knob the renderer uses — body line height, the gap between
 * blocks, the three heading levels, list indent, link colour — lives here so it can be adjusted in
 * one place. Build the default (and edit the numbers below) via [defaultHtmlStyle].
 */
@Immutable
data class HtmlStyle(
    val body: TextStyle,
    val paragraphSpacing: Dp,
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val listItem: TextStyle,
    val quote: TextStyle,
    val bold: FontWeight,
    val linkColor: Color,
    val linkUnderline: Boolean,
    val listIndent: Dp,
)

/**
 * Default HTML rendering styles — tweak the constants at the bottom (and the per-level [TextStyle]s
 * here) to taste. Line spacing is the body [TextStyle]'s `lineHeight`; paragraph spacing is the gap
 * between blocks; `<h1>/<h2>/<h3+>` map to [HtmlStyle.heading1] / [HtmlStyle.heading2] /
 * [HtmlStyle.heading3].
 */
@Composable
fun defaultHtmlStyle(
    body: TextStyle = MaterialTheme.typography.bodyMedium,
    linkColor: Color = MaterialTheme.colorScheme.primary,
): HtmlStyle {
    val type = MaterialTheme.typography
    val bodyWithLineHeight = body.copy(lineHeight = body.fontSize * HTML_LINE_HEIGHT_RATIO)
    return HtmlStyle(
        body = bodyWithLineHeight,
        paragraphSpacing = HTML_PARAGRAPH_SPACING,
        heading1 = type.headlineMedium,
        heading2 = type.headlineSmall,
        heading3 = type.titleLarge,
        listItem = bodyWithLineHeight,
        quote = bodyWithLineHeight.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        bold = FontWeight.Bold,
        linkColor = linkColor,
        linkUnderline = true,
        listIndent = HTML_LIST_INDENT,
    )
}

// ── Tunables ──────────────────────────────────────────────────────────────────────────────────
// Line spacing = body font size × this ratio.
private const val HTML_LINE_HEIGHT_RATIO = 1.5f

// Vertical gap between blocks (paragraphs, headings, list items).
private val HTML_PARAGRAPH_SPACING = 7.5.dp

// Left indent for list items and block quotes.
private val HTML_LIST_INDENT = 17.dp
