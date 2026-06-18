@file:Suppress("TooManyFunctions")

package ink.duo3.tuned.ui.components.html

import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.Html
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import ink.duo3.tuned.core.ShowNotesTimestamps
import ink.duo3.tuned.ui.components.shape.tunedRoundedCornerShape
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.material3.Text as ComposeText

/**
 * Renders feed show notes / descriptions (HTML) as styled Compose text. The HTML is split into
 * blocks (paragraphs, headings, list items, quotes); each block is its own [Text] so the app's
 * typography — the variable Cabin font, line height, theme colours — applies, and so the gap
 * between blocks, heading sizes, etc. are all tunable through [HtmlStyle].
 *
 * When [onTimestampClick] is supplied, `mm:ss` / `h:mm:ss` markers become tappable links reporting
 * their offset in milliseconds so the host can seek playback. `<a>` links open via the URI handler.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: HtmlStyle = defaultHtmlStyle(),
    color: Color = MaterialTheme.colorScheme.onSurface,
    onTimestampClick: ((Long) -> Unit)? = null,
) {
    val currentOnTimestampClick by rememberUpdatedState(onTimestampClick)
    val hasTimestamps = onTimestampClick != null
    val blocks =
        remember(html, style, hasTimestamps) {
            val onClick: ((Long) -> Unit)? =
                if (hasTimestamps) { atMs -> currentOnTimestampClick?.invoke(atMs) } else null
            parseHtmlBlocks(html, style, onClick)
        }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(style.paragraphSpacing)) {
        blocks.forEach { block -> HtmlBlockText(block = block, style = style, color = color) }
    }
}

private enum class BlockKind { Paragraph, Heading1, Heading2, Heading3, ListItem, Quote, Image }

private class HtmlBlock(
    val kind: BlockKind,
    val text: AnnotatedString = AnnotatedString(""),
    val imageUrl: String? = null,
    val aspectRatio: Float? = null,
)

// HtmlCompat needs *some* drawable per <img> to create the ImageSpan (whose source we read);
// the real picture is loaded by Coil in the Image block, so this placeholder is never drawn.
private val transparentImageGetter = Html.ImageGetter { ColorDrawable(0).apply { setBounds(0, 0, 0, 0) } }

@Composable
private fun HtmlBlockText(
    block: HtmlBlock,
    style: HtmlStyle,
    color: Color,
) {
    when (block.kind) {
        BlockKind.Paragraph -> ComposeText(block.text, color = color, style = style.body)
        BlockKind.Heading1 -> ComposeText(block.text, color = color, style = style.heading1)
        BlockKind.Heading2 -> ComposeText(block.text, color = color, style = style.heading2)
        BlockKind.Heading3 -> ComposeText(block.text, color = color, style = style.heading3)
        BlockKind.Quote ->
            ComposeText(block.text, style = style.quote, modifier = Modifier.padding(start = style.listIndent))
        BlockKind.ListItem ->
            Row(Modifier.padding(start = style.listIndent)) {
                ComposeText("•  ", color = color, style = style.listItem)
                ComposeText(block.text, color = color, style = style.listItem, modifier = Modifier.fillMaxWidth())
            }
        BlockKind.Image -> HtmlImage(block)
    }
}

/**
 * Process-wide cache of each image URL's loaded width/height ratio. Once an image has been measured
 * once, every later composition — including the throwaway one a predictive-back preview spins up —
 * can reserve its height immediately, so the content doesn't collapse-then-jump on reload.
 */
private object ImageRatioCache {
    private val ratios = ConcurrentHashMap<String, Float>()

    operator fun get(url: String): Float? = ratios[url]

    fun put(
        url: String,
        ratio: Float,
    ) {
        ratios[url] = ratio
    }
}

private fun AsyncImagePainter.State.loadedRatio(): Float? {
    val size = (this as? AsyncImagePainter.State.Success)?.painter?.intrinsicSize ?: return null
    return if (size.isSpecified && size.height > 0f) size.width / size.height else null
}

@Composable
private fun HtmlImage(block: HtmlBlock) {
    val url = block.imageUrl ?: return
    // Reserve height from the <img> attributes or the cached loaded ratio; fall back to no reservation
    // only on the very first load of an image we've never measured.
    var ratio by remember(url) { mutableFloatStateOf(block.aspectRatio ?: ImageRatioCache[url] ?: 0f) }
    val sized =
        Modifier
            .fillMaxWidth()
            .let { if (ratio > 0f) it.aspectRatio(ratio) else it }
            .clip(tunedRoundedCornerShape(IMAGE_CORNER))
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = sized,
        onState = { state ->
            state.loadedRatio()?.let { measured ->
                ImageRatioCache.put(url, measured)
                if (ratio <= 0f) ratio = measured
            }
        },
    )
}

private fun parseHtmlBlocks(
    html: String,
    style: HtmlStyle,
    onTimestampClick: ((Long) -> Unit)?,
): List<HtmlBlock> {
    val source = unescapeHtmlTags(html.trim())
    val spanned = HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_COMPACT, transparentImageGetter, null)
    val imageRatios = imageAspectRatios(source)
    // Only link timestamps when the whole notes read like a chapter list (≥2 line-leading times);
    // once that holds, each line is allowed to contribute its single timestamp.
    val linkOnClick = onTimestampClick?.takeIf { ShowNotesTimestamps.find(spanned.toString()).isNotEmpty() }
    val blocks = mutableListOf<HtmlBlock>()
    var start = 0
    while (start < spanned.length) {
        var end = start
        while (end < spanned.length && spanned[end] != '\n') end++
        if (end > start) blocks += buildBlocks(spanned, start, end, style, linkOnClick, imageRatios)
        start = end + 1
    }
    return blocks
}

@Suppress("LongParameterList")
private fun buildBlocks(
    spanned: Spanned,
    start: Int,
    end: Int,
    style: HtmlStyle,
    onTimestampClick: ((Long) -> Unit)?,
    imageRatios: Map<String, Float>,
): List<HtmlBlock> {
    val kind = blockKind(spanned, start, end)
    val inline = buildInline(spanned, start, end, style)
    val blocks = mutableListOf<HtmlBlock>()
    if (inline.text.isNotBlank()) {
        val text =
            if (onTimestampClick != null && kind != BlockKind.ListItem) {
                inline.withTimestampLinks(style.linkColor, onTimestampClick)
            } else {
                inline
            }
        blocks += HtmlBlock(kind, text)
    }
    // Each <img> became an ImageSpan over the (now-stripped) object char; render it as its own block.
    spanned.getSpans(start, end, ImageSpan::class.java).forEach { image ->
        image.source?.let { src ->
            blocks += HtmlBlock(BlockKind.Image, imageUrl = src, aspectRatio = imageRatios[src])
        }
    }
    return blocks
}

/** Maps each `<img src>` to its width/height aspect ratio (when the tag declares both). */
private fun imageAspectRatios(html: String): Map<String, Float> =
    buildMap {
        IMG_TAG.findAll(html).forEach { match ->
            val tag = match.value
            val src = imgAttr(tag, "src") ?: return@forEach
            val width = imgAttr(tag, "width")?.toFloatOrNull()
            val height = imgAttr(tag, "height")?.toFloatOrNull()
            if (width != null && height != null && height > 0f) put(src, width / height)
        }
    }

private fun imgAttr(
    tag: String,
    name: String,
): String? = Regex("""(?i)\b$name\s*=\s*["']([^"']*)["']""").find(tag)?.groupValues?.get(1)

private fun blockKind(
    spanned: Spanned,
    start: Int,
    end: Int,
): BlockKind =
    when {
        spanned.getSpans(start, end, LeadingMarginSpan::class.java).isNotEmpty() ||
            spanned.getSpans(start, end, BulletSpan::class.java).isNotEmpty() -> BlockKind.ListItem
        spanned.getSpans(start, end, QuoteSpan::class.java).isNotEmpty() -> BlockKind.Quote
        else -> {
            val sizeSpans = spanned.getSpans(start, end, RelativeSizeSpan::class.java)
            headingKind(sizeSpans.maxOfOrNull { it.sizeChange } ?: 1f)
        }
    }

private fun headingKind(sizeFactor: Float): BlockKind =
    when {
        sizeFactor >= HEADING_1_FACTOR -> BlockKind.Heading1
        sizeFactor >= HEADING_2_FACTOR -> BlockKind.Heading2
        sizeFactor >= HEADING_3_FACTOR -> BlockKind.Heading3
        else -> BlockKind.Paragraph
    }

private fun buildInline(
    spanned: Spanned,
    start: Int,
    end: Int,
    style: HtmlStyle,
): AnnotatedString {
    val segment = spanned.subSequence(start, end).toString().replace(OBJECT_REPLACEMENT, "")
    return buildAnnotatedString {
        append(segment)
        val linked = mutableListOf<IntRange>()
        spanned.getSpans(start, end, Any::class.java).forEach { span ->
            val from = (spanned.getSpanStart(span) - start).coerceIn(0, segment.length)
            val to = (spanned.getSpanEnd(span) - start).coerceIn(0, segment.length)
            if (to > from) {
                applyInlineSpan(span, from, to, style)
                if (span is URLSpan) linked += from until to
            }
        }
        addAutoLinks(segment, linked, style)
    }
}

/**
 * Linkifies bare URLs (`https://…`, `www.…`) that the author didn't wrap in an `<a>` tag, skipping
 * any that already fall inside an existing link span so a marked-up link isn't double-annotated.
 */
private fun AnnotatedString.Builder.addAutoLinks(
    segment: String,
    linkedRanges: List<IntRange>,
    style: HtmlStyle,
) {
    AUTOLINK.findAll(segment).forEach { match ->
        val first = match.range.first
        var last = match.range.last
        while (last > first && segment[last] in URL_TRAILERS) last--
        if (linkedRanges.any { first <= it.last && it.first <= last }) return@forEach
        val raw = segment.substring(first, last + 1)
        val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
        val styles = TextLinkStyles(SpanStyle(color = style.linkColor, textDecoration = linkDecoration(style)))
        addLink(LinkAnnotation.Url(url, styles), first, last + 1)
    }
}

private fun AnnotatedString.Builder.applyInlineSpan(
    span: Any,
    from: Int,
    to: Int,
    style: HtmlStyle,
) {
    when (span) {
        is StyleSpan ->
            when (span.style) {
                Typeface.BOLD -> addStyle(SpanStyle(fontWeight = style.bold), from, to)
                Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), from, to)
                Typeface.BOLD_ITALIC ->
                    addStyle(SpanStyle(fontWeight = style.bold, fontStyle = FontStyle.Italic), from, to)
            }
        is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), from, to)
        is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), from, to)
        is URLSpan ->
            addLink(
                LinkAnnotation.Url(
                    url = span.url,
                    styles = TextLinkStyles(SpanStyle(color = style.linkColor, textDecoration = linkDecoration(style))),
                ),
                from,
                to,
            )
    }
}

private fun linkDecoration(style: HtmlStyle) = TextDecoration.Underline.takeIf { style.linkUnderline }

/** Overlays tappable links on the `mm:ss` timestamp markers in an already-parsed notes string. */
private fun AnnotatedString.withTimestampLinks(
    linkColor: Color,
    onClick: (Long) -> Unit,
): AnnotatedString {
    val markers = ShowNotesTimestamps.find(text, minMarkers = 1)
    if (markers.isEmpty()) return this
    val styles = TextLinkStyles(SpanStyle(color = linkColor))
    return buildAnnotatedString {
        append(this@withTimestampLinks)
        markers.forEach { marker ->
            val link =
                LinkAnnotation.Clickable(
                    tag = marker.atMs.toString(),
                    styles = styles,
                    linkInteractionListener = { onClick(marker.atMs) },
                )
            addLink(link, marker.range.first, marker.range.last + 1)
        }
    }
}

// HtmlCompat applies these RelativeSizeSpan factors to <h1>..<h3>; map them back to heading levels.
private const val HEADING_1_FACTOR = 1.45f
private const val HEADING_2_FACTOR = 1.35f
private const val HEADING_3_FACTOR = 1.15f

private val IMAGE_CORNER = 8.dp
private val IMG_TAG = Regex("""(?i)<img\b[^>]*>""")

// Bare URLs the author didn't wrap in <a>. Allow only ASCII URL characters, so any CJK/full-width
// punctuation (）；，。 …) naturally ends the match; sentence-final ASCII punctuation that a URL can
// also legitimately contain is trimmed from the tail via URL_TRAILERS at the use site.
private val AUTOLINK = Regex("(?i)\\b(?:https?://|www\\.)[A-Za-z0-9\\-._~:/?#@!\$&'()*+,;=%]+")
private const val URL_TRAILERS = ".,;:!?'\")]}"
