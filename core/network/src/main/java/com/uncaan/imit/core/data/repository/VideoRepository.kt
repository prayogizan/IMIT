package com.uncaan.imit.core.data.repository

import com.uncaan.imit.core.model.CourseVideo
import com.uncaan.imit.core.model.VideoDetail
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getMitOcwVideos(page: Int): Flow<Result<List<CourseVideo>>>
    fun getVideoDetail(identifier: String): Flow<Result<VideoDetail>>
    fun searchVideos(query: String, page: Int): Flow<Result<List<CourseVideo>>>
}
