package com.uncaan.imit.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseVideoTest {

    @Test
    fun createCourseVideo_defaultValues_areApplied() {
        val video = CourseVideo(
            identifier = "cs101-lec01",
            title = "Introduction to Computer Science",
            description = "Basic CS principles",
            creator = "Prof. Smith",
            year = 2024,
            thumbnailUrl = "https://example.com/thumb.jpg"
        )

        assertEquals("cs101-lec01", video.identifier)
        assertEquals("Introduction to Computer Science", video.title)
        assertEquals("Basic CS principles", video.description)
        assertEquals("Prof. Smith", video.creator)
        assertEquals(2024, video.year)
        assertEquals("https://example.com/thumb.jpg", video.thumbnailUrl)
        assertEquals(0L, video.downloadsCount)
    }

    @Test
    fun courseVideo_serialization_roundTripSuccess() {
        val original = CourseVideo(
            identifier = "cs101-lec01",
            title = "Introduction to Computer Science",
            description = "Basic CS principles",
            creator = "Prof. Smith",
            year = 2024,
            thumbnailUrl = "https://example.com/thumb.jpg",
            downloadsCount = 1500L
        )

        val json = Json.encodeToString(CourseVideo.serializer(), original)
        val deserialized = Json.decodeFromString(CourseVideo.serializer(), json)

        assertEquals(original, deserialized)
    }
}
