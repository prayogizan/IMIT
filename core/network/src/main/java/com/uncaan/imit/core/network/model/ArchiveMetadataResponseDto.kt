package com.uncaan.imit.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveMetadataResponseDto(
    @SerialName("created")
    val created: Long? = null,
    @SerialName("d1")
    val d1: String? = null,
    @SerialName("d2")
    val d2: String? = null,
    @SerialName("dir")
    val dir: String? = null,
    @SerialName("files")
    val files: List<ArchiveFileDto> = emptyList(),
    @SerialName("metadata")
    val metadata: ArchiveItemMetadataDto? = null,
    @SerialName("server")
    val server: String? = null
)

@Serializable
data class ArchiveItemMetadataDto(
    @SerialName("identifier")
    val identifier: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("creator")
    val creator: String? = null,
    @SerialName("date")
    val date: String? = null,
    @SerialName("year")
    val year: String? = null,
    @SerialName("mediatype")
    val mediatype: String? = null
)
