package com.uncaan.imit.core.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.uncaan.imit.core.database.converter.DatabaseConverters
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.dao.VideoCacheDao
import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.database.entity.VideoCacheEntity

@Database(
    entities = [
        VideoCacheEntity::class,
        DownloadedVideoEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ColumnTypeConverters(DatabaseConverters::class)
abstract class MitOcwDatabase : RoomDatabase() {
    abstract fun videoCacheDao(): VideoCacheDao
    abstract fun downloadedVideoDao(): DownloadedVideoDao
}
