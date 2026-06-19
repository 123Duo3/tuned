package ink.duo3.tuned.ui.components.text

import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import ink.duo3.tuned.ui.theme.CabinFontFamily
import ink.duo3.tuned.ui.theme.cabinTypeface
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import androidx.compose.material3.Text as ComposeText

/**
 * Drop-in replacement for Material 3 [androidx.compose.material3.Text] (the plain-`String` overload)
 * that fixes CJK vertical alignment.
 *
 * Source Han / Noto CJK raise their ascent to reserve room for tone marks, so when CJK glyphs come
 * from the system fallback font (the app font, Cabin, is Latin-only) Compose lays each line out using
 * those tall fallback metrics and the text sits low / off-centre in its box. The only reliable way to
 * disable Android's fallback line spacing is a real [TextView] (`setFallbackLineSpacing(false)`,
 * API 28+) — Compose hard-codes it on with no seam — so CJK-containing strings render through one,
 * keeping the Cabin/Default typeface, size, weight, colour, line height and alignment. Everything else
 * (Latin-only text, older API levels, `onTextLayout` callers) falls through to Compose [Text]
 * unchanged, so non-CJK scripts keep their fallback metrics and there's no per-widget cost where the
 * fix isn't needed.
 */
@Composable
@Suppress("LongParameterList")
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val mergedStyle =
        style.merge(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            letterSpacing = letterSpacing,
        )
    val canDisableFallback =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            onTextLayout == null &&
            mergedStyle.fontSize.isSpecified &&
            text.containsCjk()
    if (!canDisableFallback) {
        ComposeText(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style,
        )
        return
    }
    NoFallbackText(text, mergedStyle, color, overflow, softWrap, maxLines, minLines, modifier)
}

@Composable
@Suppress("LongParameterList")
private fun NoFallbackText(
    text: String,
    style: TextStyle,
    colorOverride: Color,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val contentColor = LocalContentColor.current
    val resolvedColor = colorOverride.takeOrElse { style.color.takeOrElse { contentColor } }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                includeFontPadding = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setFallbackLineSpacing(false)
            }
        },
        update = { view ->
            view.applyTextStyle(text, style, resolvedColor, density, overflow, softWrap, maxLines, minLines)
        },
    )
}

@Suppress("LongParameterList")
private fun TextView.applyTextStyle(
    value: String,
    style: TextStyle,
    color: Color,
    density: Density,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
) {
    text = value
    includeFontPadding = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setFallbackLineSpacing(false)
    setTextColor(color.toArgb())
    setTextSize(TypedValue.COMPLEX_UNIT_PX, with(density) { style.fontSize.toPx() })
    if (style.lineHeight.isSpecified && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        setLineHeight(with(density) { style.lineHeight.toPx() }.roundToInt())
    }
    fontFeatureSettings = style.fontFeatureSettings
    letterSpacing = style.letterSpacingEm(density)
    typeface =
        resolveTypeface(
            style.fontFamily,
            style.fontWeight ?: FontWeight.Normal,
            style.fontStyle == FontStyle.Italic,
        )
    this.maxLines = maxLines
    this.minLines = minLines
    setHorizontallyScrolling(!softWrap)
    ellipsize = if (overflow == TextOverflow.Ellipsis) TextUtils.TruncateAt.END else null
    paint.isUnderlineText = style.textDecoration == TextDecoration.Underline
    paint.isStrikeThruText = style.textDecoration == TextDecoration.LineThrough
    gravity = style.textAlign.toGravity()
    textAlignment = style.textAlign.toViewTextAlignment()
}

// TextView letter spacing is measured in ems; Compose styles usually express it in sp.
private fun TextStyle.letterSpacingEm(density: Density): Float {
    val spacing = letterSpacing
    if (!spacing.isSpecified) return 0f
    return when (spacing.type) {
        TextUnitType.Em -> spacing.value
        TextUnitType.Sp ->
            if (fontSize.isSpecified) {
                with(density) { spacing.toPx() / fontSize.toPx() }
            } else {
                0f
            }
        else -> 0f
    }
}

private fun TextView.resolveTypeface(
    fontFamily: FontFamily?,
    weight: FontWeight,
    italic: Boolean,
): Typeface {
    val cabin = fontFamily == CabinFontFamily
    val key = "${if (cabin) "cabin" else "default"}:${weight.weight}:$italic"
    return typefaceCache.getOrPut(key) {
        when {
            cabin -> cabinTypeface(context, weight, italic)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ->
                Typeface.create(Typeface.DEFAULT, weight.weight, italic)
            else -> Typeface.create(Typeface.DEFAULT, legacyStyle(weight, italic))
        }
    }
}

private fun legacyStyle(
    weight: FontWeight,
    italic: Boolean,
): Int =
    when {
        italic && weight >= FontWeight.Bold -> Typeface.BOLD_ITALIC
        italic -> Typeface.ITALIC
        weight >= FontWeight.Bold -> Typeface.BOLD
        else -> Typeface.NORMAL
    }

private fun TextAlign.toGravity(): Int =
    when (this) {
        TextAlign.Center -> Gravity.CENTER
        TextAlign.Right, TextAlign.End -> Gravity.CENTER_VERTICAL or Gravity.END
        else -> Gravity.CENTER_VERTICAL or Gravity.START
    }

private fun TextAlign.toViewTextAlignment(): Int =
    when (this) {
        TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
        TextAlign.Right, TextAlign.End -> View.TEXT_ALIGNMENT_TEXT_END
        else -> View.TEXT_ALIGNMENT_TEXT_START
    }

private fun CharSequence.containsCjk(): Boolean = any { ch -> Character.UnicodeBlock.of(ch) in CJK_BLOCKS }

private val typefaceCache = ConcurrentHashMap<String, Typeface>()

private val CJK_BLOCKS =
    setOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
        Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS,
    )
