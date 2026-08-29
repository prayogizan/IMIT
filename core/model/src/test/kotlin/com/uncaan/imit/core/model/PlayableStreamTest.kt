package com.uncaan.imit.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayableStreamTest {

    private fun createStream(
        height: Int = 720,
        sizeBytes: Long = 100 * 1024 * 1024L,
        durationSeconds: Double = 120.0
    ): PlayableStream {
        return PlayableStream(
            fileName = "lec01.mp4",
            format = "h.264",
            sizeBytes = sizeBytes,
            durationSeconds = durationSeconds,
            height = height,
            width = (height * 16) / 9,
            streamUrl = "https://archive.org/download/lec01.mp4"
        )
    }

    @Test
    fun qualityLabel_variousResolutions_returnsExpectedLabels() {
        assertEquals("HD 1080p", createStream(height = 1080).qualityLabel)
        assertEquals("HD 720p", createStream(height = 720).qualityLabel)
        assertEquals("SD 480p", createStream(height = 480).qualityLabel)
        assertEquals("SD 360p", createStream(height = 360).qualityLabel)
        assertEquals("240p", createStream(height = 240).qualityLabel)
    }

    @Test
    fun formattedSize_megabytesAndGigabytes_returnsFormattedString() {
        // 500 MB
        val size500Mb = 500L * 1024 * 1024
        assertEquals("500.0 MB", createStream(sizeBytes = size500Mb).formattedSize)

        // 1.5 GB
        val size15Gb = (1.5 * 1024 * 1024 * 1024).toLong()
        assertEquals("1.5 GB", createStream(sizeBytes = size15Gb).formattedSize)

        // 1024 MB boundary -> 1.0 GB
        val size1024Mb = 1024L * 1024 * 1024
        assertEquals("1.0 GB", createStream(sizeBytes = size1024Mb).formattedSize)
    }

    @Test
    fun formattedDuration_variousDurations_returnsExpectedFormats() {
        // Less than an hour: 45 seconds -> 0:45
        assertEquals("0:45", createStream(durationSeconds = 45.0).formattedDuration)

        // Less than an hour: 5 minutes 3 seconds -> 5:03
        assertEquals("5:03", createStream(durationSeconds = 303.0).formattedDuration)

        // More than an hour: 1 hour 2 minutes 5 seconds -> 1:02:05
        assertEquals("1:02:05", createStream(durationSeconds = 3725.0).formattedDuration)

        // Multi-hour: 12 hours 0 minutes 0 seconds -> 12:00:00
        assertEquals("12:00:00", createStream(durationSeconds = 43200.0).formattedDuration)
    }

    @Test
    fun playableStream_serialization_roundTripSuccess() {
        val original = createStream()
        val json = Json.encodeToString(PlayableStream.serializer(), original)
        val deserialized = Json.decodeFromString(PlayableStream.serializer(), json)
        assertEquals(original, deserialized)
    }
}
