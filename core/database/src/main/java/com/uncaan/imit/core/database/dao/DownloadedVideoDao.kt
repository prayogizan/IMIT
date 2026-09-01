package com.uncaan.imit.core.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.uncaan.imit.core.database.entity.DownloadedVideoEntity
import com.uncaan.imit.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedVideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadedVideo: DownloadedVideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloadedVideos: List<DownloadedVideoEntity>)

    @Query("SELECT * FROM downloaded_videos ORDER BY downloaded_at DESC")
    fun getAllDownloadedVideos(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE identifier = :identifier")
    fun getDownloadedVideoById(identifier: String): Flow<DownloadedVideoEntity?>

    @Query("SELECT * FROM downloaded_videos WHERE identifier = :identifier")
    suspend fun getDownloadedVideoByIdSync(identifier: String): DownloadedVideoEntity?

    @Query("SELECT * FROM downloaded_videos WHERE status = :status ORDER BY downloaded_at DESC")
    fun getDownloadedVideosByStatus(status: DownloadStatus): Flow<List<DownloadedVideoEntity>>

    @Query("UPDATE downloaded_videos SET progress = :progress, status = :status WHERE identifier = :identifier")
    suspend fun updateProgress(identifier: String, progress: Int, status: DownloadStatus): Int

    @Query("UPDATE downloaded_videos SET status = :status WHERE identifier = :identifier")
    suspend fun updateStatus(identifier: String, status: DownloadStatus): Int

    @Query("UPDATE downloaded_videos SET local_file_path = :localFilePath, status = :status, downloaded_at = :downloadedAt WHERE identifier = :identifier")
    suspend fun markCompleted(
        identifier: String,
        localFilePath: String,
        status: DownloadStatus,
        downloadedAt: Long
    ): Int

    @Query("DELETE FROM downloaded_videos WHERE identifier = :identifier")
    suspend fun deleteById(identifier: String): Int

    @Delete
    suspend fun delete(downloadedVideo: DownloadedVideoEntity): Int

    @Query("DELETE FROM downloaded_videos")
    suspend fun clearAll(): Int
}
