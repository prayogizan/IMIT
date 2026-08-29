package com.uncaan.imit.core.data.repository

import app.cash.turbine.test
import com.uncaan.imit.core.database.dao.VideoCacheDao
import com.uncaan.imit.core.database.entity.VideoCacheEntity
import com.uncaan.imit.core.network.api.ArchiveApiService
import com.uncaan.imit.core.network.model.ArchiveFileDto
import com.uncaan.imit.core.network.model.ArchiveItemMetadataDto
import com.uncaan.imit.core.network.model.ArchiveMetadataResponseDto
import com.uncaan.imit.core.network.model.ArchiveSearchDocDto
import com.uncaan.imit.core.network.model.ArchiveSearchResponseBodyDto
import com.uncaan.imit.core.network.model.ArchiveSearchResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class VideoRepositoryImplTest {

    private val apiService: ArchiveApiService = mockk()
    private val cacheDao: VideoCacheDao = mockk(relaxed = true)
    private lateinit var repository: VideoRepositoryImpl

    @Before
    fun setUp() {
        repository = VideoRepositoryImpl(apiService, cacheDao)
    }

    @Test
    fun `getMitOcwVideos page 1 evicts old cache, caches new items, and emits success`() = runTest {
        val doc = ArchiveSearchDocDto(
            identifier = "MIT-101",
            title = "Introduction to Computer Science",
            description = "Basic CS",
            creator = "MIT",
            year = 2024,
            downloads = 1000
        )
        val response = ArchiveSearchResponseDto(
            response = ArchiveSearchResponseBodyDto(
                numFound = 1,
                start = 0,
                docs = listOf(doc)
            )
        )

        coEvery { apiService.searchMitOcwCollection(page = 1) } returns response
        coEvery { cacheDao.deleteExpired(any()) } returns 5
        coEvery { cacheDao.insertAll(any()) } returns Unit

        repository.getMitOcwVideos(page = 1).test {
            val item = awaitItem()
            assertTrue(item.isSuccess)
            val videos = item.getOrNull()
            assertEquals(1, videos?.size)
            assertEquals("MIT-101", videos?.first()?.identifier)
            awaitComplete()
        }

        coVerify(exactly = 1) { cacheDao.deleteExpired(any()) }
        coVerify(exactly = 1) { cacheDao.insertAll(any()) }
    }

    @Test
    fun `getMitOcwVideos page greater than 1 does not evict old cache and emits success`() = runTest {
        val doc = ArchiveSearchDocDto(
            identifier = "MIT-102",
            title = "Advanced Algorithms",
            description = "Algo",
            creator = "MIT",
            year = 2024,
            downloads = 500
        )
        val response = ArchiveSearchResponseDto(
            response = ArchiveSearchResponseBodyDto(
                numFound = 1,
                start = 20,
                docs = listOf(doc)
            )
        )

        coEvery { apiService.searchMitOcwCollection(page = 2) } returns response
        coEvery { cacheDao.insertAll(any()) } returns Unit

        repository.getMitOcwVideos(page = 2).test {
            val item = awaitItem()
            assertTrue(item.isSuccess)
            assertEquals(1, item.getOrNull()?.size)
            awaitComplete()
        }

        coVerify(exactly = 0) { cacheDao.deleteExpired(any()) }
        coVerify(exactly = 1) { cacheDao.insertAll(any()) }
    }

    @Test
    fun `getMitOcwVideos on network error emits cached data when available`() = runTest {
        coEvery { apiService.searchMitOcwCollection(page = 1) } throws IOException("No internet connection")
        coEvery { cacheDao.getCachedVideos() } returns listOf(
            VideoCacheEntity(
                identifier = "CACHED-01",
                title = "Cached Lecture",
                description = "Offline description",
                creator = "MIT",
                year = 2023,
                thumbnailUrl = "https://archive.org/download/CACHED-01/CACHED-01.png",
                downloadsCount = 200,
                cachedAt = System.currentTimeMillis()
            )
        )

        repository.getMitOcwVideos(page = 1).test {
            val item = awaitItem()
            assertTrue(item.isSuccess)
            val videos = item.getOrNull()
            assertEquals(1, videos?.size)
            assertEquals("CACHED-01", videos?.first()?.identifier)
            awaitComplete()
        }
    }

    @Test
    fun `getMitOcwVideos on network error emits failure when cache is empty`() = runTest {
        val networkException = IOException("No internet connection")
        coEvery { apiService.searchMitOcwCollection(page = 1) } throws networkException
        coEvery { cacheDao.getCachedVideos() } returns emptyList()

        repository.getMitOcwVideos(page = 1).test {
            val item = awaitItem()
            assertTrue(item.isFailure)
            assertEquals(networkException, item.exceptionOrNull())
            awaitComplete()
        }
    }

    @Test
    fun `getVideoDetail emits success with mapped VideoDetail`() = runTest {
        val metadataResponse = ArchiveMetadataResponseDto(
            metadata = ArchiveItemMetadataDto(
                identifier = "MIT-DETAIL-1",
                title = "Linear Algebra",
                description = "Matrix theory",
                creator = "Gilbert Strang"
            ),
            files = listOf(
                ArchiveFileDto(
                    name = "lecture.mp4",
                    format = "h.264 MPEG4",
                    size = "104857600",
                    length = "3600.0",
                    height = "720",
                    width = "1280"
                )
            )
        )

        coEvery { apiService.getItemMetadata("MIT-DETAIL-1") } returns metadataResponse

        repository.getVideoDetail("MIT-DETAIL-1").test {
            val item = awaitItem()
            assertTrue(item.isSuccess)
            val detail = item.getOrNull()
            assertEquals("MIT-DETAIL-1", detail?.identifier)
            assertEquals("Linear Algebra", detail?.title)
            assertEquals(1, detail?.streams?.size)
            assertEquals("lecture.mp4", detail?.streams?.first()?.fileName)
            awaitComplete()
        }
    }

    @Test
    fun `getVideoDetail on network error emits failure`() = runTest {
        val exception = IOException("Not found")
        coEvery { apiService.getItemMetadata("MIT-INVALID") } throws exception

        repository.getVideoDetail("MIT-INVALID").test {
            val item = awaitItem()
            assertTrue(item.isFailure)
            assertEquals(exception, item.exceptionOrNull())
            awaitComplete()
        }
    }

    @Test
    fun `searchVideos queries API and emits success`() = runTest {
        val doc = ArchiveSearchDocDto(
            identifier = "SEARCH-01",
            title = "Quantum Physics",
            description = "Quantum mechanics",
            creator = "MIT",
            year = 2022,
            downloads = 3000
        )
        val response = ArchiveSearchResponseDto(
            response = ArchiveSearchResponseBodyDto(
                numFound = 1,
                start = 0,
                docs = listOf(doc)
            )
        )

        coEvery {
            apiService.searchMitOcwCollection(
                query = "collection:mit_ocw AND mediatype:movies AND (quantum)",
                page = 1
            )
        } returns response

        repository.searchVideos(query = "quantum", page = 1).test {
            val item = awaitItem()
            assertTrue(item.isSuccess)
            val results = item.getOrNull()
            assertEquals(1, results?.size)
            assertEquals("SEARCH-01", results?.first()?.identifier)
            awaitComplete()
        }
    }

    @Test
    fun `searchVideos on network error emits failure`() = runTest {
        val exception = IOException("Search failed")
        coEvery {
            apiService.searchMitOcwCollection(
                query = "collection:mit_ocw AND mediatype:movies AND (failed_query)",
                page = 1
            )
        } throws exception

        repository.searchVideos(query = "failed_query", page = 1).test {
            val item = awaitItem()
            assertTrue(item.isFailure)
            assertEquals(exception, item.exceptionOrNull())
            awaitComplete()
        }
    }
}
