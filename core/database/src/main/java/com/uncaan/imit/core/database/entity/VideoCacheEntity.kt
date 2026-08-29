package com.uncaan.imit.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "video_cache")
data class VideoCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "identifier")
    val identifier: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "creator")
    val creator: String,
    @ColumnInfo(name = "year")
    val year: Int,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String,
    @ColumnInfo(name = "downloads_count")
    val downloadsCount: Long = 0L,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
