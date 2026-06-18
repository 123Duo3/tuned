package ink.duo3.tuned.ui.components.artwork

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.harmonizeWithPrimary
import com.materialkolor.ktx.quantize
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Derives the tint colours for a home "Subscribed" card from a piece of artwork. Theme-suitable colours
 * are extracted from a deliberately tiny decode of the cover (quantising a full-resolution bitmap is
 * needlessly expensive) and expanded into a Material scheme; the specific colours the card paints are
 * then each harmonized toward the app's primary, so the final on-screen colours stay in the theme's
 * family while still reading as "from the cover". Before the seed loads, and as a fallback, the app's
 * own primary seeds the scheme. Extracted seeds are cached process-wide by URL so scrolling never
 * re-decodes.
 */
@Composable
fun rememberArtworkPalette(artworkUrl: String?): ArtworkPalette {
    val theme = MaterialTheme.colorScheme
    val scheme = rememberArtworkColorScheme(artworkUrl)
    return ArtworkPalette(
        container = theme.harmonizeWithPrimary(scheme.primaryContainer),
        onContainer = theme.harmonizeWithPrimary(scheme.onPrimaryContainer),
        accent = theme.harmonizeWithPrimary(scheme.primary),
        onAccent = theme.harmonizeWithPrimary(scheme.onPrimary),
    )
}

@Composable
fun rememberArtworkColorScheme(artworkUrl: String?): ColorScheme {
    val theme = MaterialTheme.colorScheme
    val colors = rememberArtworkColors(artworkUrl)
    // Follow the applied app theme rather than the system appearance directly.
    val isDark = theme.surface.luminance() < DARK_SURFACE_THRESHOLD
    val artworkScheme =
        rememberDynamicColorScheme(
            seedColor = colors?.primary ?: theme.primary,
            primary = colors?.primary.takeIf { colors?.style != PaletteStyle.Monochrome },
            secondary = colors?.secondary.takeIf { colors?.style != PaletteStyle.Monochrome },
            tertiary = colors?.tertiary.takeIf { colors?.style != PaletteStyle.Monochrome },
            isDark = isDark,
            style = colors?.style ?: PaletteStyle.TonalSpot,
        )
    return if (colors == null) theme else artworkScheme
}

@Composable
private fun rememberArtworkColors(artworkUrl: String?): ArtworkSourceColors? {
    val context = LocalContext.current
    var colors by remember { mutableStateOf<ArtworkSourceColors?>(null) }
    LaunchedEffect(artworkUrl) {
        if (artworkUrl == null) {
            colors = null
            return@LaunchedEffect
        }
        colors = colorCache[artworkUrl] ?: extractColors(context, artworkUrl)?.also { colorCache[artworkUrl] = it }
    }
    return colors
}

private suspend fun extractColors(
    context: Context,
    url: String,
): ArtworkSourceColors? =
    withContext(Dispatchers.Default) {
        val request =
            ImageRequest
                .Builder(context)
                .data(url)
                .size(Size(EXTRACT_SIZE, EXTRACT_SIZE))
                .allowHardware(false) // the quantiser reads pixels, which hardware bitmaps disallow
                .build()
        val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image
        val bitmap = (image as? BitmapImage)?.bitmap?.asImageBitmap() ?: return@withContext null
        selectArtworkSourceColors(QuantizerCelebi.quantize(bitmap, QUANTIZE_MAX_COLORS))
    }

internal fun selectArtworkSourceColors(colorPopulation: Map<Int, Int>): ArtworkSourceColors? {
    val colorful = scoreArtworkColors(colorPopulation, filter = true)
    val selected = colorful.ifEmpty { scoreArtworkColors(colorPopulation, filter = false) }
    return selected.firstOrNull()?.let { primary ->
        ArtworkSourceColors(
            primary = Color(primary),
            secondary = selected.getOrNull(1)?.let(::Color),
            tertiary = selected.getOrNull(2)?.let(::Color),
            style = if (colorful.isEmpty()) PaletteStyle.Monochrome else PaletteStyle.TonalSpot,
        )
    }
}

private fun scoreArtworkColors(
    colorPopulation: Map<Int, Int>,
    filter: Boolean,
): List<Int> =
    Score.score(
        colorsToPopulation = colorPopulation,
        desired = SOURCE_COLOR_COUNT,
        fallbackColorArgb = null,
        filter = filter,
    )

// A 24px thumbnail is plenty for a stable dominant colour and keeps quantisation near-free.
private const val EXTRACT_SIZE = 24
private const val QUANTIZE_MAX_COLORS = 128
private const val SOURCE_COLOR_COUNT = 3

// Below this surface luminance the applied theme is dark.
private const val DARK_SURFACE_THRESHOLD = 0.5f

private val colorCache = ConcurrentHashMap<String, ArtworkSourceColors>()

internal data class ArtworkSourceColors(
    val primary: Color,
    val secondary: Color?,
    val tertiary: Color?,
    val style: PaletteStyle,
)
