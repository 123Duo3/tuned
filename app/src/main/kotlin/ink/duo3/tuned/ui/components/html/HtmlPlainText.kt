package ink.duo3.tuned.ui.components.html

import androidx.core.text.HtmlCompat

/** Strips HTML to readable plain text for previews and teasers. */
fun htmlToPlainText(html: String): String =
    HtmlCompat
        .fromHtml(unescapeHtmlTags(html), HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace(OBJECT_REPLACEMENT, "")
        .replace(Regex("""\n{2,}"""), "\n")
        .trim()

internal fun unescapeHtmlTags(html: String): String =
    if (!html.contains("&lt;")) {
        html
    } else {
        ESCAPED_TAG.replace(html) { match -> match.value.replace("&lt;", "<").replace("&gt;", ">") }
    }

internal const val OBJECT_REPLACEMENT = "￼"

private val ESCAPED_TAG =
    Regex(
        "&lt;\\s*/?\\s*(?:b|strong|i|em|u|s|strike|del|ins|mark|small|sub|sup|code|pre|" +
            "p|br|hr|div|span|section|blockquote|q|cite|a|img|ul|ol|li|dl|dt|dd|h[1-6])" +
            "\\b(?:(?!&gt;).)*?&gt;",
        RegexOption.IGNORE_CASE,
    )
