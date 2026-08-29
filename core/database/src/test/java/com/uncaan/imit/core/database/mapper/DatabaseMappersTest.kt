package com.uncaan.imit.core.database.mapper

import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.database.entity.VideoCacheEntity
import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.model.DownloadStatus
import com.uncaan.imit.core.model.DownloadTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatabaseMappersTest {

    @Test
    fun `toCourseVideo maps VideoCacheEntity correctly`() {
        val entity = VideoCacheEntity(
            identifier = "MIT6_001",
            title = "Structure and Interpretation of Computer Programs",
            description = "Introductory CS lecture",
            creator = "Prof. Harold Abelson",
            year = 2008,
            thumbnailUrl = "https://archive.org/download/MIT6_001/thumb.png",
            downloadsCount = 15000L,
            cachedAt = 1700000000000L
        )

        val model = entity.toCourseVideo()

        assertEquals("MIT6_001", model.identifier)
        assertEquals("Structure and Interpretation of Computer Programs", model.title)
        assertEquals("Introductory CS lecture", model.description)
        assertEquals("Prof. Harold Abelson", model.creator)
        assertEquals(2008, model.year)
        assertEquals("https://archive.org/download/MIT6_001/thumb.png", model.thumbnailUrl)
        assertEquals(15000L, model.downloadsCount)
    }

    @Test
    fun `toEntity maps CourseVideo to VideoCacheEntity with given cachedAt`() {
        val model = CourseVideo(
            identifier = "MIT6_002",
            title = "Circuits and Electronics",
            description = "Introductory EE lecture",
            creator = "Prof. Anant Agarwal",
            year = 2007,
            thumbnailUrl = "https://archive.org/download/MIT6_002/thumb.png",
            downloadsCount = 8000L
        )

        val entity = model.toEntity(cachedAt = 1700000000000L)

        assertEquals("MIT6_002", entity.identifier)
        assertEquals("Circuits and Electronics", entity.title)
        assertEquals("Introductory EE lecture", entity.description)
        assertEquals("Prof. Anant Agarwal", entity.creator)
        assertEquals(2007, entity.year)
        assertEquals("https://archive.org/download/MIT6_002/thumb.png", entity.thumbnailUrl)
        assertEquals(8000L, entity.downloadsCount)
        assertEquals(1700000000000L, entity.cachedAt)
    }

    @Test
    fun `list mappings between VideoCacheEntity and CourseVideo work bidirectionally`() {
        val entities = listOf(
            VideoCacheEntity(
                identifier = "MIT1",
                title = "Title 1",
                description = "Desc 1",
                creator = "Creator 1",
                year = 2020,
                thumbnailUrl = "thumb1",
                downloadsCount = 10L,
                cachedAt = 1000L
            ),
            VideoCacheEntity(
                identifier = "MIT2",
                title = "Title 2",
                description = "Desc 2",
                creator = "Creator 2",
                year = 2021,
                thumbnailUrl = "thumb2",
                downloadsCount = 20L,
                cachedAt = 2000L
            )
        )

        val models = entities.toCourseVideos()
        assertEquals(2, models.size)
        assertEquals("MIT1", models[0].identifier)
        assertEquals("MIT2", models[1].identifier)

        val mappedEntities = models.toEntities(cachedAt = 5000L)
        assertEquals(2, mappedEntities.size)
        assertEquals("MIT1", mappedEntities[0].identifier)
        assertEquals(5000L, mappedEntities[0].cachedAt)
        assertEquals("MIT2", mappedEntities[1].identifier)
        assertEquals(5000L, mappedEntities[1].cachedAt)
    }

    @Test
    fun `toDownloadTask maps DownloadedVideoEntity correctly`() {
        val entity = DownloadedVideoEntity(
            identifier = "MIT6_001_lec01",
            title = "Lecture 01",
            description = "Overview of Lisp",
            fileName = "lec01.mp4",
            downloadUrl = "https://archive.org/download/MIT6_001/lec01.mp4",
            localFilePath = "/storage/emulated/0/Downloads/lec01.mp4",
            fileSizeBytes = 524288000L,
            progress = 100,
            status = DownloadStatus.COMPLETED,
            downloadedAt = 1700000000000L
        )

        val task = entity.toDownloadTask()

        assertEquals("MIT6_001_lec01", task.identifier)
        assertEquals("Lecture 01", task.title)
        assertEquals("Overview of Lisp", task.description)
        assertEquals("lec01.mp4", task.fileName)
        assertEquals("https://archive.org/download/MIT6_001/lec01.mp4", task.downloadUrl)
        assertEquals("/storage/emulated/0/Downloads/lec01.mp4", task.localFilePath)
        assertEquals(524288000L, task.fileSizeBytes)
        assertEquals(100, task.progress)
        assertEquals(DownloadStatus.COMPLETED, task.status)
        assertEquals(1700000000000L, task.downloadedAt)
    }

    @Test
    fun `toEntity maps DownloadTask correctly with null description and localFilePath`() {
        val task = DownloadTask(
            identifier = "MIT6_001_lec02",
            title = "Lecture 02",
            description = null,
            fileName = "lec02.mp4",
            downloadUrl = "https://archive.org/download/MIT6_001/lec02.mp4",
            localFilePath = null,
            fileSizeBytes = 104857600L,
            progress = 45,
            status = DownloadStatus.DOWNLOADING,
            downloadedAt = 0L
        )

        val entity = task.toEntity()

        assertEquals("MIT6_001_lec02", entity.identifier)
        assertEquals("Lecture 02", entity.title)
        assertNull(entity.description)
        assertEquals("lec02.mp4", entity.fileName)
        assertEquals("https://archive.org/download/MIT6_001/lec02.mp4", entity.downloadUrl)
        assertNull(entity.localFilePath)
        assertEquals(104857600L, entity.fileSizeBytes)
        assertEquals(45, entity.progress)
        assertEquals(DownloadStatus.DOWNLOADING, entity.status)
        assertEquals(0L, entity.downloadedAt)
    }

    @Test
    fun `list mappings between DownloadedVideoEntity and DownloadTask work bidirectionally`() {
        val entities = listOf(
            DownloadedVideoEntity(
                identifier = "task1",
                title = "Task 1",
                description = null,
                fileName = "task1.mp4",
                downloadUrl = "url1",
                localFilePath = null,
                fileSizeBytes = 1000L,
                progress = 10,
                status = DownloadStatus.PENDING,
                downloadedAt = 0L
            ),
            DownloadedVideoEntity(
                identifier = "task2",
                title = "Task 2",
                description = "Desc 2",
                fileName = "task2.mp4",
                downloadUrl = "url2",
                localFilePath = "path2",
                fileSizeBytes = 2000L,
                progress = 100,
                status = DownloadStatus.COMPLETED,
                downloadedAt = 5000L
            )
        )

        val tasks = entities.toDownloadTasks()
        assertEquals(2, tasks.size)
        assertEquals("task1", tasks[0].identifier)
        assertEquals(DownloadStatus.PENDING, tasks[0].status)
        assertEquals("task2", tasks[1].identifier)
        assertEquals(DownloadStatus.COMPLETED, tasks[1].status)

        val mappedEntities = tasks.toEntities()
        assertEquals(2, mappedEntities.size)
        assertEquals("task1", mappedEntities[0].identifier)
        assertEquals("task2", mappedEntities[1].identifier)
    }
}
