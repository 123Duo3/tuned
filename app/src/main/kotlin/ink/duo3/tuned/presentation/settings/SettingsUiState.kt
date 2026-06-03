package ink.duo3.tuned.presentation.settings

import ink.duo3.tuned.domain.model.ThemeSettings

data class SettingsUiState(
    val themeSettings: ThemeSettings? = null,
    val isOpmlBusy: Boolean = false,
    val opmlEvent: OpmlEvent? = null,
)

/** One-shot OPML result, surfaced as a snackbar then consumed by the screen. */
sealed interface OpmlEvent {
    data class Imported(
        val imported: Int,
        val failed: Int,
    ) : OpmlEvent

    /** The picked document could not be parsed as OPML or could not be read. */
    data object ImportFailed : OpmlEvent

    /** Export produced [content]; the screen writes it to the chosen document. */
    data class ExportReady(
        val content: String,
    ) : OpmlEvent

    data object ExportFailed : OpmlEvent
}
