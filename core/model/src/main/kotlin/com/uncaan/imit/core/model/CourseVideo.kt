package com.uncaan.imit.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CourseVideo(
    val identifier: String,
    val title: String,
    val description: String,
    val creator: String,
    val year: Int,
    val thumbnailUrl: String,
    val downloadsCount: Long = 0L
)
