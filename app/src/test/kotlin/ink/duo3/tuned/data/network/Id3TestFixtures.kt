package ink.duo3.tuned.data.network

import java.io.ByteArrayOutputStream

/**
 * Builds minimal but valid ID3v2.3 / 2.4 tag bytes for parser and repository tests, so the
 * chapter reader is exercised against real frame layouts rather than mocked objects.
 */
internal object Id3TestFixtures {
    const val ENCODING_LATIN1 = 0
    const val ENCODING_UTF16_BOM = 1
    const val ENCODING_UTF8 = 3

    class Image(
        val mimeType: String,
        val data: ByteArray,
    )

    class ChapterSpec(
        val elementId: String,
        val startMs: Long,
        val endMs: Long = startMs + 1_000,
        val title: String?,
        val titleEncoding: Int = ENCODING_UTF8,
        val image: Image? = null,
    )

    /** Assembles a full tag (ID3 header + CHAP frames) for [chapters]. */
    fun tag(
        vararg chapters: ChapterSpec,
        major: Int = 3,
    ): ByteArray {
        val frames = ByteArrayOutputStream()
        chapters.forEach { frames.write(frame("CHAP", chapBody(it, major), major)) }
        val body = frames.toByteArray()
        return ByteArrayOutputStream()
            .apply {
                write("ID3".toByteArray(Charsets.ISO_8859_1))
                write(byteArrayOf(major.toByte(), 0, 0)) // version, revision, flags
                write(synchsafe(body.size))
                write(body)
            }.toByteArray()
    }

    private fun chapBody(
        spec: ChapterSpec,
        major: Int,
    ): ByteArray =
        ByteArrayOutputStream()
            .apply {
                write(spec.elementId.toByteArray(Charsets.ISO_8859_1))
                write(0)
                write(beUInt32(spec.startMs))
                write(beUInt32(spec.endMs))
                write(beUInt32(0xFFFF_FFFFL))
                write(beUInt32(0xFFFF_FFFFL))
                spec.title?.let { write(frame("TIT2", textBody(it, spec.titleEncoding), major)) }
                spec.image?.let { write(frame("APIC", apicBody(it), major)) }
            }.toByteArray()

    private fun textBody(
        text: String,
        encoding: Int,
    ): ByteArray {
        val charset =
            when (encoding) {
                ENCODING_UTF16_BOM -> Charsets.UTF_16
                ENCODING_UTF8 -> Charsets.UTF_8
                else -> Charsets.ISO_8859_1
            }
        return byteArrayOf(encoding.toByte()) + text.toByteArray(charset)
    }

    private fun apicBody(image: Image): ByteArray =
        ByteArrayOutputStream()
            .apply {
                write(ENCODING_LATIN1) // text encoding for the description
                write(image.mimeType.toByteArray(Charsets.ISO_8859_1))
                write(0) // mime terminator
                write(3) // picture type: cover (front)
                write(0) // empty description + terminator
                write(image.data)
            }.toByteArray()

    private fun frame(
        id: String,
        body: ByteArray,
        major: Int,
    ): ByteArray =
        ByteArrayOutputStream()
            .apply {
                write(id.toByteArray(Charsets.ISO_8859_1))
                write(if (major == 4) synchsafe(body.size) else beUInt32(body.size.toLong()))
                write(byteArrayOf(0, 0)) // frame flags
                write(body)
            }.toByteArray()

    private fun beUInt32(value: Long): ByteArray =
        byteArrayOf(
            (value ushr 24 and 0xFF).toByte(),
            (value ushr 16 and 0xFF).toByte(),
            (value ushr 8 and 0xFF).toByte(),
            (value and 0xFF).toByte(),
        )

    private fun synchsafe(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 21 and 0x7F).toByte(),
            (value ushr 14 and 0x7F).toByte(),
            (value ushr 7 and 0x7F).toByte(),
            (value and 0x7F).toByte(),
        )
}
