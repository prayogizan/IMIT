package com.uncaan.imit.core.database.dao

import app.cash.turbine.test
import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.model.DownloadStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DownloadedVideoDaoTest {

    private lateinit var dao: DownloadedVideoDao

    private fun createEntity(
        id: String = "video-1",
        title: String = "Test Video",
        status: DownloadStatus = DownloadStatus.PENDING,
        downloadedAt: Long = 1000L,
        progress: Int = 0,
        localFilePath: String? = null
    ): DownloadedVideoEntity {
        return DownloadedVideoEntity(
            identifier = id,
            title = title,
            description = "Description for $id",
            fileName = "$id.mp4",
            downloadUrl = "https://archive.org/download/$id/$id.mp4",
            localFilePath = localFilePath,
            fileSizeBytes = 500L * 1024 * 1024,
            progress = progress,
            status = status,
            downloadedAt = downloadedAt
        )
    }

    @Before
    fun setUp() {
        dao = FakeDownloadedVideoDao()
    }

    @Test
    fun `insert stores entity and getDownloadedVideoById returns it`() = runTest {
        val entity = createEntity("video-101", "Linear Algebra")
        dao.insert(entity)

        val retrievedSync = dao.getDownloadedVideoByIdSync("video-101")
        assertNotNull(retrievedSync)
        assertEquals("video-101", retrievedSync?.identifier)
        assertEquals("Linear Algebra", retrievedSync?.title)
        assertEquals(DownloadStatus.PENDING, retrievedSync?.status)

        dao.getDownloadedVideoById("video-101").test {
            val item = awaitItem()
            assertEquals("video-101", item?.identifier)
            assertEquals("Linear Algebra", item?.title)
        }
    }

    @Test
    fun `insertAll stores multiple entities and getAllDownloadedVideos orders by downloadedAt desc`() = runTest {
        val entity1 = createEntity("video-1", "Lecture 1", downloadedAt = 1000L)
        val entity2 = createEntity("video-2", "Lecture 2", downloadedAt = 3000L)
        val entity3 = createEntity("video-3", "Lecture 3", downloadedAt = 2000L)

        dao.insertAll(listOf(entity1, entity2, entity3))

        dao.getAllDownloadedVideos().test {
            val list = awaitItem()
            assertEquals(3, list.size)
            assertEquals("video-2", list[0].identifier)
            assertEquals("video-3", list[1].identifier)
            assertEquals("video-1", list[2].identifier)
        }
    }

    @Test
    fun `getDownloadedVideosByStatus filters correctly by status`() = runTest {
        val pending = createEntity("v-pending", "Pending Vid", status = DownloadStatus.PENDING)
        val downloading = createEntity("v-downloading", "Downloading Vid", status = DownloadStatus.DOWNLOADING)
        val completed = createEntity("v-completed", "Completed Vid", status = DownloadStatus.COMPLETED)

        dao.insertAll(listOf(pending, downloading, completed))

        dao.getDownloadedVideosByStatus(DownloadStatus.DOWNLOADING).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("v-downloading", list[0].identifier)
        }

        dao.getDownloadedVideosByStatus(DownloadStatus.COMPLETED).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("v-completed", list[0].identifier)
        }
    }

    @Test
    fun `updateProgress updates progress and status`() = runTest {
        val entity = createEntity("video-prog", status = DownloadStatus.PENDING, progress = 0)
        dao.insert(entity)

        val updatedRows = dao.updateProgress("video-prog", progress = 45, status = DownloadStatus.DOWNLOADING)
        assertEquals(1, updatedRows)

        val updated = dao.getDownloadedVideoByIdSync("video-prog")
        assertEquals(45, updated?.progress)
        assertEquals(DownloadStatus.DOWNLOADING, updated?.status)
    }

    @Test
    fun `updateStatus updates status only`() = runTest {
        val entity = createEntity("video-stat", status = DownloadStatus.DOWNLOADING, progress = 50)
        dao.insert(entity)

        val updatedRows = dao.updateStatus("video-stat", DownloadStatus.PAUSED)
        assertEquals(1, updatedRows)

        val updated = dao.getDownloadedVideoByIdSync("video-stat")
        assertEquals(DownloadStatus.PAUSED, updated?.status)
        assertEquals(50, updated?.progress)
    }

    @Test
    fun `markCompleted updates localFilePath, timestamp, progress, and status to COMPLETED`() = runTest {
        val entity = createEntity("video-comp", status = DownloadStatus.DOWNLOADING)
        dao.insert(entity)

        val completedAt = 1700000500000L
        val localPath = "/storage/emulated/0/Downloads/imit/video-comp.mp4"
        val updatedRows = dao.markCompleted("video-comp", localPath, DownloadStatus.COMPLETED, completedAt)
        assertEquals(1, updatedRows)

        val updated = dao.getDownloadedVideoByIdSync("video-comp")
        assertEquals(DownloadStatus.COMPLETED, updated?.status)
        assertEquals(100, updated?.progress)
        assertEquals(localPath, updated?.localFilePath)
        assertEquals(completedAt, updated?.downloadedAt)
    }

    @Test
    fun `deleteById removes entity from storage and emits updated list`() = runTest {
        val entity = createEntity("video-del")
        dao.insert(entity)

        dao.getAllDownloadedVideos().test {
            assertEquals(1, awaitItem().size)

            val deletedRows = dao.deleteById("video-del")
            assertEquals(1, deletedRows)

            assertEquals(0, awaitItem().size)
        }

        assertNull(dao.getDownloadedVideoByIdSync("video-del"))
    }

    @Test
    fun `delete entity removes specific record`() = runTest {
        val entity = createEntity("video-del-entity")
        dao.insert(entity)

        val deletedRows = dao.delete(entity)
        assertEquals(1, deletedRows)
        assertNull(dao.getDownloadedVideoByIdSync("video-del-entity"))
    }

    @Test
    fun `clearAll empties all downloaded records`() = runTest {
        val entities = (1..5).map { createEntity("v-$it") }
        dao.insertAll(entities)

        assertEquals(5, dao.clearAll())

        dao.getAllDownloadedVideos().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `getAllDownloads emits same reactive list as getAllDownloadedVideos`() = runTest {
        val entity = createEntity("v-download-alias", "Alias Video")
        dao.insert(entity)

        dao.getAllDownloads().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("v-download-alias", list.first().identifier)
        }
    }

    @Test
    fun `deleteDownload removes record by identifier`() = runTest {
        val entity = createEntity("v-to-delete", "To Delete")
        dao.insert(entity)

        val deletedCount = dao.deleteDownload("v-to-delete")
        assertEquals(1, deletedCount)
        assertNull(dao.getDownloadedVideoByIdSync("v-to-delete"))
    }

    @Test
    fun `getTotalDownloadedSize calculates total bytes of completed videos only`() = runTest {
        val completed1 = createEntity("v-c1", status = DownloadStatus.COMPLETED).copy(fileSizeBytes = 100L)
        val completed2 = createEntity("v-c2", status = DownloadStatus.COMPLETED).copy(fileSizeBytes = 250L)
        val downloading = createEntity("v-d1", status = DownloadStatus.DOWNLOADING).copy(fileSizeBytes = 500L)

        dao.insertAll(listOf(completed1, completed2, downloading))

        val totalSize = dao.getTotalDownloadedSize()
        assertEquals(350L, totalSize)
    }
}
