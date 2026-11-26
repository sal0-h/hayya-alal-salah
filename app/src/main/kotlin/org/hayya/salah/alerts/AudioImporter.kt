// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.alerts

import android.content.Context
import android.media.*
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hayya.salah.PrayerRepository
import org.hayya.salah.data.AudioEntity
import java.util.UUID

object AudioImporter {
    suspend fun import(context: Context, uri: Uri, repository: PrayerRepository): AudioEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val directory = context.filesDir.resolve("audio").apply { check(isDirectory || mkdirs()) }
        val file = directory.resolve(id)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                file.outputStream().use { output ->
                    val buffer = ByteArray(16384); var total = 0L
                    while (true) { val count = input.read(buffer); if (count < 0) break; total += count; require(total <= 25L * 1024 * 1024); output.write(buffer, 0, count) }
                    require(total > 0)
                }
            }
            val media = MediaPlayer()
            val duration = try { media.setDataSource(file.absolutePath); media.prepare(); media.duration.toLong().also { require(it in 1..600000) } } finally { media.release() }
            verifyDecodable(file.absolutePath)
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.filterNot(Char::isISOControl)?.take(100) else null
            }?.takeIf { it.isNotBlank() } ?: id
            val entity = AudioEntity(id, name, id, duration, "User-selected local document; not redistributed")
            repository.putAudio(entity)
            entity
        } catch (error: Exception) { file.delete(); throw error }
    }
    /** Decode one actual audio output buffer; a file extension/metadata alone is insufficient. */
    private fun verifyDecodable(path: String) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(path)
            val track = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            requireNotNull(track)
            val format = extractor.getTrackFormat(track)
            extractor.selectTrack(track)
            val decoder = MediaCodec.createDecoderByType(requireNotNull(format.getString(MediaFormat.KEY_MIME)))
            codec = decoder
            decoder.configure(format, null, null, 0); decoder.start()
            var decoded = false
            val info = MediaCodec.BufferInfo()
            repeat(100) {
                if (decoded) return@repeat
                val input = decoder.dequeueInputBuffer(10000)
                if (input >= 0) {
                    val size = extractor.readSampleData(requireNotNull(decoder.getInputBuffer(input)), 0)
                    decoder.queueInputBuffer(input, 0, size.coerceAtLeast(0), extractor.sampleTime.coerceAtLeast(0), if (size < 0) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                    if (size >= 0) extractor.advance()
                }
                val output = decoder.dequeueOutputBuffer(info, 10000)
                if (output >= 0) { decoded = info.size > 0; decoder.releaseOutputBuffer(output, false) }
            }
            require(decoded)
        } finally { codec?.release(); extractor.release() }
    }
}
