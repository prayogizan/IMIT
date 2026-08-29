package com.uncaan.imit.core.data.repository

import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.model.VideoDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeVideoRepository : VideoRepository {

    var shouldReturnError: Boolean = false
    var errorToThrow: Throwable = Exception("Fake repository test error")

    val videos = mutableListOf<CourseVideo>()
    val videoDetails = mutableMapOf<String, VideoDetail>()
    val searchResults = mutableListOf<CourseVideo>()

    override fun getMitOcwVideos(page: Int): Flow<Result<List<CourseVideo>>> = flow {
        if (shouldReturnError) {
            emit(Result.failure(errorToThrow))
        } else {
            emit(Result.success(videos.toList()))
        }
    }

    override fun getVideoDetail(identifier: String): Flow<Result<VideoDetail>> = flow {
        if (shouldReturnError) {
            emit(Result.failure(errorToThrow))
        } else {
            val detail = videoDetails[identifier]
            if (detail != null) {
                emit(Result.success(detail))
            } else {
                emit(Result.failure(NoSuchElementException("No video detail found for $identifier")))
            }
        }
    }

    override fun searchVideos(query: String, page: Int): Flow<Result<List<CourseVideo>>> = flow {
        if (shouldReturnError) {
            emit(Result.failure(errorToThrow))
        } else {
            val results = if (searchResults.isNotEmpty()) {
                searchResults.toList()
            } else {
                videos.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
                }
            }
            emit(Result.success(results))
        }
    }
}
