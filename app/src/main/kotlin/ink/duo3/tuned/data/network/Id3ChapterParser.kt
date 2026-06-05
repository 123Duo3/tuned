package ink.duo3.tuned.data.network

/** A picture extracted from an APIC sub-frame. Plain class: a [ByteArray] in a data class trips detekt. */
internal class Id3Image(
    val mimeType: String,
    val data: ByteArray,
)

internal data class Id3Chapter(
    val startTimeMs: Long,
    val title: String?,
    val image: Id3Image?,
)

/**
 * Minimal ID3v2.3 / 2.4 reader that pulls CHAP chapter frames — start time, TIT2 title, and
 * APIC artwork — out of a complete tag (bytes starting at the "ID3" header). Best-effort: a
 * malformed or unsupported tag yields an empty list rather than throwing. CTOC ordering is
 * ignored; chapters are sorted by start time to match the Podcasting 2.0 path. ID3v2.2 (with
 * its 3-byte frame ids) is not supported — chapters predate it in practice.
 */
@Suppress("TooManyFunctions")
internal object Id3ChapterParser {
    fun parse(tag: ByteArray): List<Id3Chapter> {
        // 2.2 uses 3-byte frame ids and is unsupported; a missing header reads back as -1.
        val major = if (hasId3Header(tag)) tag[VERSION_OFFSET].toInt() and 0xFF else -1
        if (major != 3 && major != 4) return emptyList()

        val flags = tag[FLAGS_OFFSET].toInt() and 0xFF
        val declaredSize = synchsafe(tag, SIZE_OFFSET)
        val end = minOf(HEADER_SIZE + declaredSize, tag.size)
        var frames = tag.copyOfRange(HEADER_SIZE, end)
        if (flags and UNSYNC_FLAG != 0) frames = removeUnsynchronization(frames)

        val start = if (flags and EXT_HEADER_FLAG != 0) extendedHeaderSize(frames, major) else 0
        return readFrames(frames, start, frames.size, major)
            .sortedBy { it.startTimeMs }
    }

    private fun hasId3Header(tag: ByteArray): Boolean =
        tag.size >= HEADER_SIZE &&
            tag[0] == 'I'.code.toByte() &&
            tag[1] == 'D'.code.toByte() &&
            tag[2] == '3'.code.toByte()

    private data class Frame(
        val id: String,
        val dataStart: Int,
        val dataEnd: Int,
    )

    /** Next frame at [pos], or null at padding / a truncated frame (i.e. stop scanning). */
    private fun nextFrame(
        buf: ByteArray,
        pos: Int,
        end: Int,
        major: Int,
    ): Frame? {
        if (pos + FRAME_HEADER_SIZE > end) return null
        val id = frameId(buf, pos) // null = zero id = padding
        val size = frameSize(buf, pos + ID_SIZE, major)
        val dataStart = pos + FRAME_HEADER_SIZE
        return if (id != null && size > 0 && dataStart + size <= end) Frame(id, dataStart, dataStart + size) else null
    }

    /** Walks the frame list in [start, end), collecting one [Id3Chapter] per CHAP frame. */
    private fun readFrames(
        buf: ByteArray,
        start: Int,
        end: Int,
        major: Int,
    ): List<Id3Chapter> {
        val chapters = mutableListOf<Id3Chapter>()
        var pos = start
        while (true) {
            val frame = nextFrame(buf, pos, end, major) ?: break
            if (frame.id == "CHAP") parseChapter(buf, frame.dataStart, frame.dataEnd, major)?.let(chapters::add)
            pos = frame.dataEnd
        }
        return chapters
    }

    /** CHAP body: element-id\0, 4×uint32 (start/end ms, start/end byte offset), then sub-frames. */
    private fun parseChapter(
        buf: ByteArray,
        start: Int,
        end: Int,
        major: Int,
    ): Id3Chapter? {
        var pos = skipPast(buf, start, end, NUL) // element id
        if (pos + CHAP_TIMES_SIZE > end) return null
        val startMs = beUInt32(buf, pos)
        pos += CHAP_TIMES_SIZE // skip endMs + start/end byte offsets (4 × uint32)

        var title: String? = null
        var image: Id3Image? = null
        while (true) {
            val frame = nextFrame(buf, pos, end, major) ?: break
            when (frame.id) {
                "TIT2" -> title = decodeTextFrame(buf, frame.dataStart, frame.dataEnd)
                "APIC" -> image = parseApic(buf, frame.dataStart, frame.dataEnd)
            }
            pos = frame.dataEnd
        }
        return Id3Chapter(startTimeMs = startMs, title = title, image = image)
    }

    /** APIC: encoding, mime\0 (always latin-1), picture-type, description (encoded)\0, image bytes. */
    private fun parseApic(
        buf: ByteArray,
        start: Int,
        end: Int,
    ): Id3Image? {
        val mimeEnd = if (start < end) indexOf(buf, start + 1, end, NUL) else -1
        if (mimeEnd < 0) return null
        val encoding = buf[start].toInt() and 0xFF
        val mime = String(buf, start + 1, mimeEnd - (start + 1), Charsets.ISO_8859_1).ifBlank { DEFAULT_MIME }
        val afterPictureType = mimeEnd + 1 + 1 // skip mime NUL + 1-byte picture type
        val dataStart = skipPastTerminator(buf, afterPictureType, end, encoding) // description
        return if (dataStart < end) Id3Image(mimeType = mime, data = buf.copyOfRange(dataStart, end)) else null
    }

    private fun decodeTextFrame(
        buf: ByteArray,
        start: Int,
        end: Int,
    ): String? {
        if (start >= end) return null
        val encoding = buf[start].toInt() and 0xFF
        return String(buf, start + 1, end - (start + 1), charsetFor(encoding))
            .trimEnd(NUL.toInt().toChar())
            .trim()
            .ifBlank { null }
    }

    private fun charsetFor(encoding: Int) =
        when (encoding) {
            ENCODING_UTF16_BOM -> Charsets.UTF_16
            ENCODING_UTF16_BE -> Charsets.UTF_16BE
            ENCODING_UTF8 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }

    private fun frameId(
        buf: ByteArray,
        pos: Int,
    ): String? {
        if (buf[pos].toInt() == 0) return null
        return String(buf, pos, ID_SIZE, Charsets.ISO_8859_1)
    }

    private fun frameSize(
        buf: ByteArray,
        pos: Int,
        major: Int,
    ): Int = if (major == 4) synchsafe(buf, pos) else beUInt32(buf, pos).toInt()

    private fun extendedHeaderSize(
        buf: ByteArray,
        major: Int,
    ): Int {
        if (buf.size < ID_SIZE) return 0
        val size = if (major == 4) synchsafe(buf, 0) else beUInt32(buf, 0).toInt()
        // v2.4 counts the 4 size bytes in its total; v2.3 reports the bytes that follow them.
        return (if (major == 4) size else size + ID_SIZE).coerceIn(0, buf.size)
    }

    /** Reverses unsynchronisation: every 0xFF 0x00 pair becomes a single 0xFF. */
    private fun removeUnsynchronization(buf: ByteArray): ByteArray {
        val out = ByteArray(buf.size)
        var w = 0
        var i = 0
        while (i < buf.size) {
            out[w++] = buf[i]
            if (buf[i] == FF && i + 1 < buf.size && buf[i + 1] == NUL) i++
            i++
        }
        return out.copyOf(w)
    }

    private fun beUInt32(
        buf: ByteArray,
        pos: Int,
    ): Long =
        ((buf[pos].toLong() and 0xFF) shl 24) or
            ((buf[pos + 1].toLong() and 0xFF) shl 16) or
            ((buf[pos + 2].toLong() and 0xFF) shl 8) or
            (buf[pos + 3].toLong() and 0xFF)

    private fun synchsafe(
        buf: ByteArray,
        pos: Int,
    ): Int =
        ((buf[pos].toInt() and 0x7F) shl 21) or
            ((buf[pos + 1].toInt() and 0x7F) shl 14) or
            ((buf[pos + 2].toInt() and 0x7F) shl 7) or
            (buf[pos + 3].toInt() and 0x7F)

    private fun indexOf(
        buf: ByteArray,
        start: Int,
        end: Int,
        target: Byte,
    ): Int {
        var i = start
        while (i < end) {
            if (buf[i] == target) return i
            i++
        }
        return -1
    }

    private fun skipPast(
        buf: ByteArray,
        start: Int,
        end: Int,
        target: Byte,
    ): Int {
        val at = indexOf(buf, start, end, target)
        return if (at < 0) end else at + 1
    }

    /** Skips a string terminator: one NUL for single-byte encodings, an aligned 0x00 0x00 for UTF-16. */
    private fun skipPastTerminator(
        buf: ByteArray,
        start: Int,
        end: Int,
        encoding: Int,
    ): Int =
        if (encoding == ENCODING_UTF16_BOM || encoding == ENCODING_UTF16_BE) {
            var i = start
            while (i + 1 < end && !(buf[i] == NUL && buf[i + 1] == NUL)) i += 2
            minOf(i + 2, end)
        } else {
            skipPast(buf, start, end, NUL)
        }

    private const val HEADER_SIZE = 10
    private const val FRAME_HEADER_SIZE = 10
    private const val ID_SIZE = 4
    private const val VERSION_OFFSET = 3
    private const val FLAGS_OFFSET = 5
    private const val SIZE_OFFSET = 6
    private const val CHAP_TIMES_SIZE = 16
    private const val UNSYNC_FLAG = 0x80
    private const val EXT_HEADER_FLAG = 0x40
    private const val ENCODING_UTF16_BOM = 1
    private const val ENCODING_UTF16_BE = 2
    private const val ENCODING_UTF8 = 3
    private const val NUL: Byte = 0
    private const val FF: Byte = 0xFF.toByte()
    private const val DEFAULT_MIME = "image/jpeg"
}
