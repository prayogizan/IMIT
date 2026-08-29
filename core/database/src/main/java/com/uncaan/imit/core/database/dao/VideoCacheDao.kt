package com.uncaan.imit.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.uncaan.imit.core.database.entity.VideoCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoCacheEntity>)

    @Query("SELECT * FROM video_cache ORDER BY cached_at DESC")
    fun getAllVideos(): Flow<List<VideoCacheEntity>>

    @Query("SELECT * FROM video_cache ORDER BY cached_at DESC")
    suspend fun getCachedVideos(): List<VideoCacheEntity>

    @Query("SELECT * FROM video_cache WHERE identifier = :identifier")
    suspend fun getVideoById(identifier: String): VideoCacheEntity?

    @Query("SELECT * FROM video_cache WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY cached_at DESC")
    fun searchVideos(query: String): Flow<List<VideoCacheEntity>>

    @Query("DELETE FROM video_cache WHERE cached_at < :threshold")
    suspend fun deleteExpired(threshold: Long): Int

    @Query("DELETE FROM video_cache")
    suspend fun clearAll(): Int
}
