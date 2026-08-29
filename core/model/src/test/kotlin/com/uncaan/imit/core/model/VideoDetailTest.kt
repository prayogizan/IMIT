package com.uncaan.imit.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoDetailTest {

    private fun createStreamWithHeight(height: Int): PlayableStream {
        return PlayableStream(
            fileName = "video_${height}p.mp4",
            format = "h.264",
            sizeBytes = 1000L,
            durationSeconds = 60.0,
            height = height,
            width = (height * 16) / 9,
            streamUrl = "https://example.com/video_${height}p.mp4"
        )
    }

    @Test
    fun bestStream_and_sdStream_multipleStreams_returnMaxAndMinHeight() {
        val stream360 = createStreamWithHeight(360)
        val stream720 = createStreamWithHeight(720)
        val stream1080 = createStreamWithHeight(1080)

        val detail = VideoDetail(
            identifier = "video-1",
            title = "Test Video",
            description = "Description",
            creator = "Creator",
            streams = listOf(stream720, stream1080, stream360),
            thumbnailUrl = "https://example.com/thumb.jpg"
        )

        assertEquals(stream1080, detail.bestStream)
        assertEquals(stream360, detail.sdStream)
    }

    @Test
    fun bestStream_and_sdStream_emptyStreams_returnNull() {
        val detail = VideoDetail(
            identifier = "video-1",
            title = "Test Video",
            description = "Description",
            creator = "Creator",
            streams = emptyList(),
            thumbnailUrl = "https://example.com/thumb.jpg"
        )

        assertNull(detail.bestStream)
        assertNull(detail.sdStream)
    }

    @Test
    fun videoDetail_serialization_roundTripSuccess() {
        val detail = VideoDetail(
            identifier = "video-1",
            title = "Test Video",
            description = "Description",
            creator = "Creator",
            streams = listOf(createStreamWithHeight(720)),
            thumbnailUrl = "https://example.com/thumb.jpg"
        )

        val json = Json.encodeToString(VideoDetail.serializer(), detail)
        val deserialized = Json.decodeFromString(VideoDetail.serializer(), json)
        assertEquals(detail, deserialized)
    }
}
