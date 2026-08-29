package com.uncaan.imit.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveSearchDocDto(
    @SerialName("identifier")
    val identifier: String,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("creator")
    val creator: String? = null,
    @SerialName("year")
    val year: Int? = null,
    @SerialName("publicdate")
    val publicdate: String? = null,
    @SerialName("downloads")
    val downloads: Long? = null
)
