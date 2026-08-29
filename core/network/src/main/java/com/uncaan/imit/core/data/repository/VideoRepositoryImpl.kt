package com.uncaan.imit.core.data.repository

import com.uncaan.imit.core.database.dao.VideoCacheDao
import com.uncaan.imit.core.database.mapper.toCourseVideo
import com.uncaan.imit.core.database.mapper.toEntity
import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.model.VideoDetail
import com.uncaan.imit.core.network.api.ArchiveApiService
import com.uncaan.imit.core.network.mapper.toCourseVideo
import com.uncaan.imit.core.network.mapper.toVideoDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

class VideoRepositoryImpl(
    private val apiService: ArchiveApiService,
    private val cacheDao: VideoCacheDao
) : VideoRepository {

    override fun getMitOcwVideos(page: Int): Flow<Result<List<CourseVideo>>> = flow {
        try {
            val response = apiService.searchMitOcwCollection(page = page)
            val docs = response.response?.docs ?: emptyList()
            val videos = docs.map { it.toCourseVideo() }

            if (page == 1) {
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                cacheDao.deleteExpired(sevenDaysAgo)
            }
            cacheDao.insertAll(videos.map { it.toEntity() })
            emit(Result.success(videos))
        } catch (e: Exception) {
            emitCachedOrError(e)
        }
    }

    override fun getVideoDetail(identifier: String): Flow<Result<VideoDetail>> = flow {
        try {
            val response = apiService.getItemMetadata(identifier)
            emit(Result.success(response.toVideoDetail()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun searchVideos(query: String, page: Int): Flow<Result<List<CourseVideo>>> = flow {
        try {
            val searchQuery = "collection:mit_ocw AND mediatype:movies AND ($query)"
            val response = apiService.searchMitOcwCollection(query = searchQuery, page = page)
            val docs = response.response?.docs ?: emptyList()
            emit(Result.success(docs.map { it.toCourseVideo() }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private suspend fun FlowCollector<Result<List<CourseVideo>>>.emitCachedOrError(
        originalError: Exception
    ) {
        val cached = cacheDao.getCachedVideos()
        if (cached.isNotEmpty()) {
            emit(Result.success(cached.map { it.toCourseVideo() }))
        } else {
            emit(Result.failure(originalError))
        }
    }
}
