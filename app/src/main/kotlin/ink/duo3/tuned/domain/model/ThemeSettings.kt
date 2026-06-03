package ink.duo3.tuned.domain.model

/**
 * User-chosen appearance preferences. Backed by DataStore and consumed by the app theme.
 *
 * - [followSystemAppearance]: when true, light/dark tracks the system; otherwise [useDarkMode] wins.
 * - [useMonet]: when true, the color scheme is generated (dynamic wallpaper on Android 12+, or a
 *   seed-derived scheme); when false, Tuned's hand-tuned brand scheme is used.
 * - [monetSeed]: the ARGB seed color for a custom theme. `0` means "follow the system wallpaper"
 *   on Android 12+, falling back to the brand seed on older versions.
 */
data class ThemeSettings(
    val followSystemAppearance: Boolean = true,
    val useDarkMode: Boolean = false,
    val useMonet: Boolean = false,
    val monetSeed: Int = MONET_SEED_SYSTEM,
) {
    companion object {
        /** Sentinel seed meaning "use the system wallpaper palette" (Android 12+). */
        const val MONET_SEED_SYSTEM = 0
    }
}
