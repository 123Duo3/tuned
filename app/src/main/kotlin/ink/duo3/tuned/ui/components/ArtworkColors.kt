package ink.duo3.tuned.ui.components

import android.content.Context
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
import com.materialkolor.ktx.themeColorOrNull
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Derives the tint colours for a home "Subscribed" card from a piece of artwork. The seed colour is
 * extracted off a deliberately tiny decode of the cover (quantising a full-resolution bitmap is
 * needlessly expensive) and expanded into a Material scheme; the specific colours the card paints are
 * then each harmonized toward the app's primary, so the final on-screen colours stay in the theme's
 * family while still reading as "from the cover". Before the seed loads, and as a fallback, the app's
 * own primary seeds the scheme. Extracted seeds are cached process-wide by URL so scrolling never
 * re-decodes.
 */
@Composable
fun rememberArtworkPalette(artworkUrl: String?): ArtworkPalette {
    val theme = MaterialTheme.colorScheme
    // Follow the *applied* theme (app setting / Monet), not the system: a dark surface means dark theme.
    val isDark = theme.surface.luminance() < DARK_SURFACE_THRESHOLD
    val scheme =
        rememberDynamicColorScheme(
            seedColor = rememberArtworkSeed(artworkUrl) ?: theme.primary,
            isDark = isDark,
            style = PaletteStyle.TonalSpot,
        )
    return ArtworkPalette(
        container = theme.harmonizeWithPrimary(scheme.primaryContainer),
        onContainer = theme.harmonizeWithPrimary(scheme.onPrimaryContainer),
        accent = theme.harmonizeWithPrimary(scheme.primary),
        onAccent = theme.harmonizeWithPrimary(scheme.onPrimary),
    )
}

@Composable
private fun rememberArtworkSeed(artworkUrl: String?): Color? {
    val context = LocalContext.current
    var seed by remember(artworkUrl) { mutableStateOf(artworkUrl?.let { seedCache[it] }) }
    LaunchedEffect(artworkUrl) {
        if (artworkUrl == null || seed != null) return@LaunchedEffect
        seed = extractSeed(context, artworkUrl)?.also { seedCache[artworkUrl] = it }
    }
    return seed
}

private suspend fun extractSeed(
    context: Context,
    url: String,
): Color? =
    withContext(Dispatchers.Default) {
        val request =
            ImageRequest
                .Builder(context)
                .data(url)
                .size(Size(EXTRACT_SIZE, EXTRACT_SIZE))
                .allowHardware(false) // the quantiser reads pixels, which hardware bitmaps disallow
                .build()
        val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image
        (image as? BitmapImage)?.bitmap?.asImageBitmap()?.themeColorOrNull()
    }

// A 24px thumbnail is plenty for a stable dominant colour and keeps quantisation near-free.
private const val EXTRACT_SIZE = 24

// Below this surface luminance the applied theme is dark.
private const val DARK_SURFACE_THRESHOLD = 0.5f

private val seedCache = ConcurrentHashMap<String, Color>()
