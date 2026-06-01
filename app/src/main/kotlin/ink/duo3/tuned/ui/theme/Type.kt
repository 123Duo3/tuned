package ink.duo3.tuned.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

private val DefaultTypography = Typography()

val Typography =
    Typography(
        displayLarge = DefaultTypography.displayLarge.withCabin(),
        displayMedium = DefaultTypography.displayMedium.withCabin(),
        displaySmall = DefaultTypography.displaySmall.withCabin(),
        headlineLarge = DefaultTypography.headlineLarge.withCabin(),
        headlineMedium = DefaultTypography.headlineMedium.withCabin(),
        headlineSmall = DefaultTypography.headlineSmall.withCabin(),
        titleLarge = DefaultTypography.titleLarge.withCabin(),
        titleMedium = DefaultTypography.titleMedium.withCabin(),
        titleSmall = DefaultTypography.titleSmall.withCabin(),
        bodyLarge = DefaultTypography.bodyLarge.withCabin(),
        bodyMedium = DefaultTypography.bodyMedium.withCabin(),
        bodySmall = DefaultTypography.bodySmall.withCabin(),
        labelLarge = DefaultTypography.labelLarge.withCabin(),
        labelMedium = DefaultTypography.labelMedium.withCabin(),
        labelSmall = DefaultTypography.labelSmall.withCabin(),
    )

private fun TextStyle.withCabin() = copy(fontFamily = CabinFontFamily)
