package com.uncaan.imit.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveFileDto(
    @SerialName("name")
    val name: String,
    @SerialName("source")
    val source: String? = null,
    @SerialName("format")
    val format: String? = null,
    @SerialName("size")
    val size: String? = null,
    @SerialName("length")
    val length: String? = null,
    @SerialName("height")
    val height: String? = null,
    @SerialName("width")
    val width: String? = null,
    @SerialName("title")
    val title: String? = null
)
