package com.uncaan.imit.core.database.converter

import androidx.room3.ColumnTypeConverter
import com.uncaan.imit.core.model.DownloadStatus

class DatabaseConverters {

    @ColumnTypeConverter
    fun fromDownloadStatus(status: DownloadStatus?): String? {
        return status?.name
    }

    @ColumnTypeConverter
    fun toDownloadStatus(value: String?): DownloadStatus? {
        return value?.let { DownloadStatus.fromString(it) }
    }
}
