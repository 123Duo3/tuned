package ink.duo3.tuned.data.model

import kotlinx.serialization.Serializable

/**
 * Wire shape of a Podcasting 2.0 chapters JSON document
 * (https://github.com/Podcastindex-org/podcast-namespace/blob/main/chapters/jsonChapters.md).
 * Only the fields the app renders are declared; the rest are ignored by the
 * `ignoreUnknownKeys` [kotlinx.serialization.json.Json] instance.
 */
@Serializable
data class ChaptersDocumentDto(
    val version: String? = null,
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
data class ChapterDto(
    // Seconds (fractional allowed) from the episode start. Required by the spec; an entry
    // without it can't be placed on the timeline and is dropped.
    val startTime: Double? = null,
    val title: String? = null,
    // Chapter artwork URL.
    val img: String? = null,
    // Optional related web link for the chapter.
    val url: String? = null,
    // false hides the entry from the table of contents (e.g. a mid-chapter image cue).
    val toc: Boolean? = null,
)
