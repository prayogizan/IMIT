package com.uncaan.imit.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Background worker responsible for streaming video files from remote URLs to local storage.
 *
 * Managed by WorkManager and injected via Koin. Provides:
 * - Low-importance foreground notification with continuous percentage progress.
 * - Storage space guard verifying available disk space (>= 500MB) before downloading.
 * - Real-time progress updates persisted to Room database via [DownloadedVideoDao].
 * - Clean cooperative cancellation and storage cleanup on job interruption.
 *
 * @param context Android application context.
 * @param workerParams WorkManager worker parameters.
 * @param okHttpClient HTTP client configured with timeouts and interceptors.
 * @param downloadDao DAO for tracking download progress and states in Room.
 */
class VideoDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadedVideoDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        /** Input key for the unique Archive.org identifier. */
        const val KEY_IDENTIFIER = "KEY_IDENTIFIER"

        /** Input key for the direct video download URL. */
        const val KEY_DOWNLOAD_URL = "KEY_DOWNLOAD_URL"

        /** Input key for the destination video file name. */
        const val KEY_FILE_NAME = "KEY_FILE_NAME"

        /** Input key for the display title in foreground notification. */
        const val KEY_TITLE = "KEY_TITLE"

        /** Progress output key containing integer percentage (0..100). */
        const val KEY_PROGRESS = "KEY_PROGRESS"

        /** Error output key containing error description string. */
        const val KEY_ERROR = "KEY_ERROR"

        /** Notification channel ID for video downloads. */
        const val CHANNEL_ID = "video_downloads"

        /** Notification ID for foreground download service. */
        const val NOTIFICATION_ID = 1001

        /** Minimum required disk space in bytes (500 MB). */
        const val MIN_STORAGE_BYTES = 500L * 1024L * 1024L

        /** Subdirectory name within external files dir for video storage. */
        const val DOWNLOAD_DIR_NAME = "mit_ocw_videos"

        /** Stream read buffer size in bytes (8 KB). */
        private const val BUFFER_SIZE = 8 * 1024
    }

    override suspend fun doWork(): Result {
        val identifier = inputData.getString(KEY_IDENTIFIER) ?: return Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "$identifier.mp4"
        val title = inputData.getString(KEY_TITLE) ?: "Downloading video..."

        createNotificationChannel()
        setForeground(createForegroundInfo(title, 0))

        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val targetDir = File(baseDir, DOWNLOAD_DIR_NAME)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        if (!hasEnoughStorage(targetDir)) {
            downloadDao.updateStatus(identifier, DownloadStatus.FAILED)
            return Result.failure(workDataOf(KEY_ERROR to "Insufficient storage space (<500MB)"))
        }

        val targetFile = File(targetDir, fileName)

        return try {
            downloadDao.updateProgress(identifier, 0, DownloadStatus.DOWNLOADING)
            val request = Request.Builder().url(downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body
            if (!response.isSuccessful) {
                body.close()
                downloadDao.updateStatus(identifier, DownloadStatus.FAILED)
                return Result.failure()
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            var lastReportedProgress = -1

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            if (targetFile.exists()) {
                                targetFile.delete()
                            }
                            downloadDao.updateStatus(identifier, DownloadStatus.PAUSED)
                            return Result.failure()
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else {
                            0
                        }

                        if (progress != lastReportedProgress) {
                            lastReportedProgress = progress
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                            downloadDao.updateProgress(identifier, progress, DownloadStatus.DOWNLOADING)
                            setForeground(createForegroundInfo(title, progress))
                        }
                    }
                }
            }

            downloadDao.markCompleted(
                identifier = identifier,
                localFilePath = targetFile.absolutePath,
                status = DownloadStatus.COMPLETED,
                downloadedAt = System.currentTimeMillis()
            )
            Result.success(workDataOf(KEY_PROGRESS to 100))
        } catch (e: CancellationException) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            downloadDao.updateStatus(identifier, DownloadStatus.PAUSED)
            throw e
        } catch (e: Exception) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            downloadDao.updateStatus(identifier, DownloadStatus.FAILED)
            Result.retry()
        }
    }

    /**
     * Checks whether the volume hosting [dir] has at least [MIN_STORAGE_BYTES] free.
     */
    private fun hasEnoughStorage(dir: File): Boolean {
        return try {
            val stat = StatFs(dir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= MIN_STORAGE_BYTES
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Creates notification channel for low-importance download progress on Android O+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Video download background progress"
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /**
     * Builds foreground service notification with progress bar and ongoing status.
     *
     * @param title Title displayed on the notification.
     * @param progress Progress percentage (0..100).
     * @return [ForegroundInfo] configured with dataSync foreground type on Android Q+.
     */
    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("Downloading: $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
