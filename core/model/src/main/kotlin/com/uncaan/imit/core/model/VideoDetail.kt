package com.uncaan.imit.core.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoDetail(
    val identifier: String,
    val title: String,
    val description: String,
    val creator: String,
    val streams: List<PlayableStream> = emptyList(),
    val thumbnailUrl: String
) {
    val bestStream: PlayableStream?
        get() = streams.maxByOrNull { it.height }

    val sdStream: PlayableStream?
        get() = streams.minByOrNull { it.height }
}
