package ink.duo3.tuned.data.network

import ink.duo3.tuned.data.model.ChaptersDocumentDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

/**
 * Fetches chapter data. Two sources:
 * - [fetch] pulls a Podcasting 2.0 chapters JSON document.
 * - [fetchAudioId3Tag] range-requests an episode's leading ID3v2 tag (where embedded CHAP
 *   chapters live) without downloading the whole audio file.
 *
 * A non-2xx status raises [FeedHttpException]; a malformed JSON body surfaces as a
 * [kotlinx.serialization.SerializationException] — both mapped to typed errors upstream.
 */
class ChaptersApi(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetch(url: String): ChaptersDocumentDto {
        val response = httpClient.get(url)
        if (response.status.value !in SUCCESS_RANGE) throw FeedHttpException(response.status.value)
        return json.decodeFromString(response.bodyAsText())
    }

    /**
     * Returns the leading ID3v2 tag bytes of the audio at [url], or null when the file has no
     * ID3v2 header (nothing to parse). Reads the 10-byte header first to learn the tag size,
     * then fetches exactly that many bytes (capped), so a feed with embedded chapter art costs
     * a few MB rather than the whole episode. Requires the host to honour Range requests.
     */
    suspend fun fetchAudioId3Tag(url: String): ByteArray? {
        val header = readRange(url, HEADER_SIZE - 1)
        if (header.size < HEADER_SIZE || !header.startsWithId3()) return null
        val total = (HEADER_SIZE + synchsafeSize(header)).coerceAtMost(MAX_ID3_BYTES)
        return readRange(url, total - 1)
    }

    private suspend fun readRange(
        url: String,
        lastByte: Int,
    ): ByteArray {
        val response = httpClient.get(url) { header(HttpHeaders.Range, "bytes=0-$lastByte") }
        if (response.status.value !in SUCCESS_RANGE) throw FeedHttpException(response.status.value)
        return response.readUpTo(lastByte + 1)
    }

    // Reads at most [max] bytes then stops — so a server that ignores Range and replies 200
    // with the full file still only streams the leading bytes we asked for.
    private suspend fun HttpResponse.readUpTo(max: Int): ByteArray {
        val channel = bodyAsChannel()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(READ_CHUNK)
        while (out.size() < max) {
            val read = channel.readAvailable(buffer, 0, minOf(buffer.size, max - out.size()))
            if (read == -1) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun ByteArray.startsWithId3(): Boolean =
        this[0] == 'I'.code.toByte() && this[1] == 'D'.code.toByte() && this[2] == '3'.code.toByte()

    // ID3v2 tag size is a 4-byte synchsafe integer at offset 6 (7 significant bits per byte).
    private fun synchsafeSize(header: ByteArray): Int =
        ((header[6].toInt() and 0x7F) shl 21) or
            ((header[7].toInt() and 0x7F) shl 14) or
            ((header[8].toInt() and 0x7F) shl 7) or
            (header[9].toInt() and 0x7F)

    private companion object {
        val SUCCESS_RANGE = 200..299
        const val HEADER_SIZE = 10
        const val READ_CHUNK = 8 * 1024
        const val MAX_ID3_BYTES = 12 * 1024 * 1024
    }
}
