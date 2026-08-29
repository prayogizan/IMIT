package com.uncaan.imit.core.data.repository

import com.uncaan.imit.core.database.dao.VideoCacheDao
import com.uncaan.imit.core.database.mapper.toCourseVideo
import com.uncaan.imit.core.database.mapper.toEntity
import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.model.VideoDetail
import com.uncaan.imit.core.network.api.ArchiveApiService
import com.uncaan.imit.core.network.mapper.toCourseVideo
import com.uncaan.imit.core.network.mapper.toVideoDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VideoRepositoryImpl(
    private val apiService: ArchiveApiService,
    private val cacheDao: VideoCacheDao
) : VideoRepository {

    override fun getMitOcwVideos(page: Int): Flow<Result<List<CourseVideo>>> = flow {
        val result = try {
            val response = apiService.searchMitOcwCollection(page = page)
            val docs = response.response?.docs ?: emptyList()
            val videos = docs.map { it.toCourseVideo() }

            if (page == 1) {
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                try {
                    cacheDao.deleteExpired(sevenDaysAgo)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Non-fatal cache cleanup failure
                }
            }
            try {
                cacheDao.insertAll(videos.map { it.toEntity() })
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Non-fatal caching failure
            }
            Result.success(videos)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = try {
                cacheDao.getCachedVideos()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                emptyList()
            }

            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toCourseVideo() })
            } else {
                Result.failure(e)
            }
        }

        emit(result)
    }

    override fun getVideoDetail(identifier: String): Flow<Result<VideoDetail>> = flow {
        val result = try {
            val response = apiService.getItemMetadata(identifier)
            Result.success(response.toVideoDetail())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(result)
    }

    override fun searchVideos(query: String, page: Int): Flow<Result<List<CourseVideo>>> = flow {
        val result = try {
            val searchQuery = "collection:mit_ocw AND mediatype:movies AND ($query)"
            val response = apiService.searchMitOcwCollection(query = searchQuery, page = page)
            val docs = response.response?.docs ?: emptyList()
            Result.success(docs.map { it.toCourseVideo() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(result)
    }
}
