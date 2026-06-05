package ink.duo3.tuned.data.repository

import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.data.model.ChapterDto
import ink.duo3.tuned.data.network.ChaptersApi
import ink.duo3.tuned.data.network.FeedHttpException
import ink.duo3.tuned.data.network.Id3Chapter
import ink.duo3.tuned.data.network.Id3ChapterParser
import ink.duo3.tuned.data.network.Id3Image
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.repository.ChaptersRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException

/**
 * Resolves chapters from the Podcasting 2.0 JSON document first, falling back to chapters
 * embedded in the audio file's ID3 tag (TIT2 titles + APIC artwork, written to [imageCacheDir]
 * so Coil can load them by file URL). Results are cached by episode id in a bounded LRU; a
 * definitive result (including a confirmed "no chapters") is cached, but a transport failure is
 * not, so it can be retried. Failures are swallowed into an empty list — chapters are optional.
 */
class ChaptersRepositoryImpl(
    private val api: ChaptersApi,
    private val imageCacheDir: File,
    private val cacheCapacity: Int = DEFAULT_CACHE_CAPACITY,
) : ChaptersRepository {
    private val mutex = Mutex()
    private val cache =
        object : LinkedHashMap<String, List<Chapter>>(0, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, List<Chapter>>): Boolean = size > cacheCapacity
        }

    override suspend fun chapters(episode: Episode): Outcome<List<Chapter>> {
        mutex.withLock { cache[episode.id] }?.let { return Outcome.Success(it) }
        val resolved = resolve(episode)
        if (resolved != null) mutex.withLock { cache[episode.id] = resolved }
        return Outcome.Success(resolved ?: emptyList())
    }

    // Returns a definitive chapter list (possibly empty), or null when every source failed
    // to load (so the result is not cached and a later open retries).
    private suspend fun resolve(episode: Episode): List<Chapter>? {
        val fromJson = episode.chaptersUrl?.let { loadPodcasting20(it) }
        if (fromJson != null && fromJson.isNotEmpty()) return fromJson
        val fromId3 = episode.enclosureUrl?.let { loadEmbedded(episode.id, it) }
        return fromId3 ?: fromJson
    }

    private suspend fun loadPodcasting20(chaptersUrl: String): List<Chapter>? =
        try {
            api
                .fetch(chaptersUrl)
                .chapters
                .filter { it.toc != false }
                .mapNotNull { it.toChapter() }
                .sortedBy { it.startTimeMs }
        } catch (_: FeedHttpException) {
            null
        } catch (_: SerializationException) {
            null
        } catch (_: IOException) {
            null
        }

    private suspend fun loadEmbedded(
        episodeId: String,
        enclosureUrl: String,
    ): List<Chapter>? =
        try {
            val tag = api.fetchAudioId3Tag(enclosureUrl)
            if (tag == null) {
                emptyList()
            } else {
                Id3ChapterParser
                    .parse(tag)
                    .mapIndexed { index, chapter -> chapter.toChapter(episodeId, index) }
            }
        } catch (_: FeedHttpException) {
            null
        } catch (_: IOException) {
            null
        }

    private fun ChapterDto.toChapter(): Chapter? {
        val start = startTime ?: return null
        return Chapter(
            startTimeMs = (start * MILLIS_PER_SECOND).toLong(),
            title = title?.trim()?.ifBlank { null },
            imageUrl = img?.trim()?.ifBlank { null },
            url = url?.trim()?.ifBlank { null },
        )
    }

    private fun Id3Chapter.toChapter(
        episodeId: String,
        index: Int,
    ): Chapter =
        Chapter(
            startTimeMs = startTimeMs,
            title = title,
            imageUrl = image?.let { writeImage(episodeId, index, it) },
        )

    // Persists an embedded picture to the cache dir and returns a file:// URL Coil can load,
    // or null if it can't be written (the chapter then simply shows no art).
    private fun writeImage(
        episodeId: String,
        index: Int,
        image: Id3Image,
    ): String? =
        try {
            val dir = File(imageCacheDir, IMAGE_DIR).apply { mkdirs() }
            val file = File(dir, "${episodeId.sanitized()}_$index.${extensionFor(image.mimeType)}")
            file.writeBytes(image.data)
            "file://${file.absolutePath}"
        } catch (_: IOException) {
            null
        }

    private fun String.sanitized(): String = replace(UNSAFE_FILENAME_CHARS, "_")

    private fun extensionFor(mimeType: String): String =
        when (mimeType.lowercase().substringAfter('/', "")) {
            "jpeg", "jpg" -> "jpg"
            "png" -> "png"
            "gif" -> "gif"
            "webp" -> "webp"
            else -> "img"
        }

    private companion object {
        const val DEFAULT_CACHE_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
        const val MILLIS_PER_SECOND = 1000.0
        const val IMAGE_DIR = "chapter_images"
        val UNSAFE_FILENAME_CHARS = Regex("[^A-Za-z0-9._-]")
    }
}
