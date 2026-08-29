package com.uncaan.imit.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveSearchResponseDto(
    @SerialName("response")
    val response: ArchiveSearchResponseBodyDto? = null
)

@Serializable
data class ArchiveSearchResponseBodyDto(
    @SerialName("numFound")
    val numFound: Int = 0,
    @SerialName("start")
    val start: Int = 0,
    @SerialName("docs")
    val docs: List<ArchiveSearchDocDto> = emptyList()
)
