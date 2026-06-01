@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package ink.duo3.tuned.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/**
 * Cabin is loaded from assets instead of res/font because some Xiaomi ROMs replace
 * packaged font resources and lose variable-font weight settings.
 */
internal val CabinFontFamily =
    FontFamily(
        cabinAssetFont(FontWeight.Normal, FontStyle.Normal),
        cabinAssetFont(FontWeight.Normal, FontStyle.Italic),
        cabinAssetFont(FontWeight.Medium, FontStyle.Normal),
        cabinAssetFont(FontWeight.Medium, FontStyle.Italic),
        cabinAssetFont(FontWeight.SemiBold, FontStyle.Normal),
        cabinAssetFont(FontWeight.SemiBold, FontStyle.Italic),
        cabinAssetFont(FontWeight.Bold, FontStyle.Normal),
        cabinAssetFont(FontWeight.Bold, FontStyle.Italic),
    )

private fun cabinAssetFont(
    weight: FontWeight,
    style: FontStyle,
) = CabinAssetFont(
    path = if (style == FontStyle.Italic) CABIN_ITALIC_ASSET_PATH else CABIN_ASSET_PATH,
    weight = weight,
    style = style,
    variationSettingsString = "'wdth' $CABIN_DEFAULT_WIDTH, 'wght' ${weight.weight}",
)

private class CabinAssetFont(
    val path: String,
    override val weight: FontWeight,
    override val style: FontStyle,
    val variationSettingsString: String,
) : AndroidFont(
        loadingStrategy = FontLoadingStrategy.Blocking,
        typefaceLoader = CabinAssetTypefaceLoader,
        variationSettings = FontVariation.Settings(),
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CabinAssetFont) return false

        return path == other.path &&
            weight == other.weight &&
            style == other.style &&
            variationSettingsString == other.variationSettingsString
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + variationSettingsString.hashCode()
        return result
    }
}

private object CabinAssetTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(
        context: Context,
        font: AndroidFont,
    ): Typeface {
        font as CabinAssetFont
        return Typeface
            .Builder(context.assets, font.path)
            .setFontVariationSettings(font.variationSettingsString)
            .build()
    }

    override suspend fun awaitLoad(
        context: Context,
        font: AndroidFont,
    ): Typeface = loadBlocking(context, font)
}

private const val CABIN_DEFAULT_WIDTH = 100
private const val CABIN_ASSET_PATH = "fonts/cabin_variable.ttf"
private const val CABIN_ITALIC_ASSET_PATH = "fonts/cabin_variable_italic.ttf"
