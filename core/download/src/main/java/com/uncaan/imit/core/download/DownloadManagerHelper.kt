package com.uncaan.imit.core.download

import android.content.Context
import android.os.StatFs
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

/**
 * Helper class managing video download work requests via [WorkManager].
 *
 * Provides APIs to:
 * - Enqueue background video downloads with network constraints and unique work policy.
 * - Guard against storage exhaustion before scheduling work (< 500MB free space).
 * - Pause and cancel active or scheduled download operations.
 * - Observe reactive download [WorkInfo] state changes.
 *
 * @param context Android application context used for storage resolution and WorkManager.
 * @param workManager Optional WorkManager instance, defaults to [WorkManager.getInstance].
 */
open class DownloadManagerHelper(
    private val context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context)
) {

    companion object {
        /** Global tag applied to all video download work requests. */
        const val TAG_ALL_DOWNLOADS = "tag_video_downloads"

        /** Tag prefix for querying a specific video download. */
        const val TAG_DOWNLOAD_PREFIX = "download_tag_"

        /** Unique work prefix used to manage single downloads per video identifier. */
        private const val UNIQUE_WORK_PREFIX = "download_"

        /**
         * Computes unique work name for a given [identifier].
         */
        fun getUniqueWorkName(identifier: String): String = "$UNIQUE_WORK_PREFIX$identifier"
    }

    /**
     * Enqueues a video download work request.
     *
     * Validates that at least 500MB of storage is available before scheduling.
     * Enqueues as unique work with [ExistingWorkPolicy.KEEP] to prevent duplicate
     * simultaneous downloads for the same lecture.
     *
     * @param identifier Unique Archive.org identifier for the video.
     * @param title Display title shown in notification.
     * @param downloadUrl Direct URL to stream the video.
     * @param fileName Destination file name, defaults to "$identifier.mp4".
     * @return [Result] containing the scheduled [UUID] on success, or an exception on error.
     */
    fun enqueueDownload(
        identifier: String,
        title: String,
        downloadUrl: String,
        fileName: String = "$identifier.mp4"
    ): Result<UUID> {
        if (!hasEnoughStorage()) {
            return Result.failure(IllegalStateException("Insufficient storage space (< 500MB free)"))
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            VideoDownloadWorker.KEY_IDENTIFIER to identifier,
            VideoDownloadWorker.KEY_TITLE to title,
            VideoDownloadWorker.KEY_DOWNLOAD_URL to downloadUrl,
            VideoDownloadWorker.KEY_FILE_NAME to fileName
        )

        val workRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(TAG_ALL_DOWNLOADS)
            .addTag("$TAG_DOWNLOAD_PREFIX$identifier")
            .build()

        workManager.enqueueUniqueWork(
            getUniqueWorkName(identifier),
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        return Result.success(workRequest.id)
    }

    /**
     * Pauses an active download by cancelling its unique WorkManager job.
     *
     * @param identifier Video item identifier to pause.
     */
    fun pauseDownload(identifier: String) {
        workManager.cancelUniqueWork(getUniqueWorkName(identifier))
    }

    /**
     * Cancels an active or queued download and removes any partial or existing file.
     *
     * @param identifier Video item identifier to cancel.
     */
    fun cancelDownload(identifier: String) {
        workManager.cancelUniqueWork(getUniqueWorkName(identifier))
        val targetDir = getDownloadDirectory()
        val targetFile = File(targetDir, "$identifier.mp4")
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    /**
     * Observes reactive [WorkInfo] state for a specific video download.
     *
     * @param identifier Video item identifier to observe.
     * @return [Flow] emitting the latest [WorkInfo] or null if no work exists.
     */
    fun getWorkInfoFlow(identifier: String): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow(getUniqueWorkName(identifier))
            .map { it.firstOrNull() }
    }

    /**
     * Verifies whether the destination download directory has at least 500MB free.
     *
     * @return True if available storage >= 500MB, false otherwise.
     */
    open fun hasEnoughStorage(): Boolean {
        val dir = getDownloadDirectory()
        return try {
            val stat = StatFs(dir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= VideoDownloadWorker.MIN_STORAGE_BYTES
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns the available free bytes on the download storage volume.
     *
     * @return Free space in bytes, or 0 if unable to inspect.
     */
    open fun getAvailableStorageBytes(): Long {
        val dir = getDownloadDirectory()
        return try {
            val stat = StatFs(dir.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Resolves and creates the download storage directory if it does not exist.
     *
     * @return [File] pointing to the video download directory.
     */
    open fun getDownloadDirectory(): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, VideoDownloadWorker.DOWNLOAD_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
