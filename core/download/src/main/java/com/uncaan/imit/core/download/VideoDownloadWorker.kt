package com.uncaan.imit.core.download

import android.app.Notification
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
import kotlin.math.abs

/**
 * Background worker responsible for streaming video files from remote URLs to local storage.
 *
 * Managed by WorkManager and injected via Koin. Provides:
 * - Low-importance foreground notification with continuous percentage progress and deep-link navigation.
 * - Storage space guard verifying available disk space (>= 500MB) before downloading.
 * - Real-time progress updates persisted to Room database via [DownloadedVideoDao].
 * - HTTP byte-range resumption (`Range: bytes=<existingSize>-`) using temporary files (`.tmp`).
 * - Clean cooperative cancellation preserving partial `.tmp` files for subsequent resume.
 *
 * @param context Android application context.
 * @param workerParams WorkManager worker parameters.
 * @param okHttpClient HTTP client configured with timeouts and interceptors.
 * @param downloadDao DAO for tracking download progress and states in Room.
 * @param downloadNavigator Navigator creating deep-link PendingIntents for notifications.
 */
class VideoDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadedVideoDao,
    private val downloadNavigator: DownloadNotificationNavigator
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

        /** Default fallback notification ID when identifier is null. */
        const val DEFAULT_NOTIFICATION_ID = 1001

        /**
         * Notification ID constant maintained for backward compatibility.
         * Equivalent to [DEFAULT_NOTIFICATION_ID].
         */
        const val NOTIFICATION_ID = DEFAULT_NOTIFICATION_ID

        /** Minimum required disk space in bytes (500 MB). */
        const val MIN_STORAGE_BYTES = 500L * 1024L * 1024L

        /** Subdirectory name within external files dir for video storage. */
        const val DOWNLOAD_DIR_NAME = "mit_ocw_videos"

        /** Stream read buffer size in bytes (8 KB). */
        private const val BUFFER_SIZE = 8 * 1024

        /**
         * Calculates a deterministic unique positive notification ID from an Archive.org [identifier].
         *
         * Maps the hash code into the range `[1000..100999]` using `abs(identifier.hashCode().toLong()) % 100_000 + 1000`.
         * Returns [DEFAULT_NOTIFICATION_ID] if [identifier] is null.
         *
         * @param identifier Unique Archive.org item identifier.
         * @return Deterministic positive integer notification ID.
         */
        fun getNotificationId(identifier: String?): Int {
            if (identifier == null) return DEFAULT_NOTIFICATION_ID
            return (abs(identifier.hashCode().toLong()) % 100_000L + 1000L).toInt()
        }
    }

    /**
     * Unique positive notification ID derived deterministically from the item [KEY_IDENTIFIER].
     */
    internal val notificationId: Int
        get() = getNotificationId(inputData.getString(KEY_IDENTIFIER))

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = inputData.getString(KEY_TITLE) ?: "Downloading video..."
        return createForegroundInfo(title, 0)
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
            val errorMsg = "Insufficient storage space (<500MB)"
            downloadDao.updateStatus(identifier, DownloadStatus.FAILED)
            showFailureNotification(title, identifier, errorMsg)
            return Result.failure(workDataOf(KEY_ERROR to errorMsg))
        }

        val outputFile = File(targetDir, fileName)
        val partialFile = File(targetDir, "$fileName.tmp")

        var totalBytes = -1L
        var downloadedBytes = 0L
        var currentProgress = 0

        return try {
            val existingBytes = if (partialFile.exists()) partialFile.length() else 0L
            val requestBuilder = Request.Builder().url(downloadUrl)
            if (existingBytes > 0L) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body
            if (!response.isSuccessful) {
                body.close()
                if (response.code == 416 && partialFile.exists()) {
                    partialFile.delete()
                }
                downloadDao.updateStatus(identifier, DownloadStatus.FAILED)
                val errorMsg = "HTTP error: ${response.code}"
                showFailureNotification(title, identifier, errorMsg)
                return Result.failure(workDataOf(KEY_ERROR to errorMsg))
            }

            val isPartial = response.code == 206
            val appendMode: Boolean

            if (isPartial) {
                appendMode = true
                val contentLength = body.contentLength()
                totalBytes = if (contentLength >= 0) existingBytes + contentLength else -1L
                downloadedBytes = existingBytes
            } else {
                appendMode = false
                downloadedBytes = 0L
                totalBytes = body.contentLength()
            }

            currentProgress = if (totalBytes > 0) {
                ((downloadedBytes * 100) / totalBytes).toInt()
            } else {
                0
            }
            var lastReportedProgress = currentProgress

            downloadDao.updateProgress(identifier, currentProgress, DownloadStatus.DOWNLOADING)
            setProgress(workDataOf(KEY_PROGRESS to currentProgress))
            setForeground(createForegroundInfo(title, currentProgress))

            body.byteStream().use { input ->
                FileOutputStream(partialFile, appendMode).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            currentProgress = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt()
                            } else {
                                0
                            }
                            downloadDao.updateProgress(identifier, currentProgress, DownloadStatus.PAUSED)
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
                            currentProgress = progress
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                            downloadDao.updateProgress(identifier, progress, DownloadStatus.DOWNLOADING)
                            setForeground(createForegroundInfo(title, progress))
                        }
                    }
                }
            }

            if (outputFile.exists()) {
                outputFile.delete()
            }
            val renameSuccess = partialFile.renameTo(outputFile)
            val finalPath = if (renameSuccess) outputFile.absolutePath else partialFile.absolutePath

            downloadDao.markCompleted(
                identifier = identifier,
                localFilePath = finalPath,
                status = DownloadStatus.COMPLETED,
                downloadedAt = System.currentTimeMillis()
            )
            showCompletionNotification(title, identifier)
            Result.success(workDataOf(KEY_PROGRESS to 100))
        } catch (e: CancellationException) {
            currentProgress = if (totalBytes > 0) {
                ((downloadedBytes * 100) / totalBytes).toInt()
            } else {
                0
            }
            downloadDao.updateProgress(identifier, currentProgress, DownloadStatus.PAUSED)
            throw e
        } catch (e: Exception) {
            if (partialFile.exists()) {
                partialFile.delete()
            }
            downloadDao.updateStatus(identifier, DownloadStatus.FAILED)
            val errorMsg = e.localizedMessage ?: "Download failed"
            showFailureNotification(title, identifier, errorMsg)
            Result.failure(workDataOf(KEY_ERROR to errorMsg))
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
     * Factory used to construct [Notification] instances from [NotificationCompat.Builder].
     * Can be replaced in unit tests to avoid android.jar mockable stubs throwing in pure JVM tests.
     */
    internal var notificationFactory: (NotificationCompat.Builder) -> Notification = { it.build() }

    /**
     * Builds foreground service notification builder with progress bar, ongoing status, and downloads screen deep link.
     *
     * @param title Title displayed on the notification.
     * @param progress Progress percentage (0..100).
     * @return [NotificationCompat.Builder] configured with ongoing status and navigation pending intent.
     */
    internal fun createForegroundNotificationBuilder(title: String, progress: Int): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("Downloading: $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(downloadNavigator.createDownloadsPendingIntent(context))
    }

    /**
     * Builds foreground service notification with progress bar, ongoing status, and downloads screen deep link.
     *
     * @param title Title displayed on the notification.
     * @param progress Progress percentage (0..100).
     * @return [Notification] configured with ongoing status and navigation pending intent.
     */
    internal fun createForegroundNotification(title: String, progress: Int): Notification {
        return notificationFactory(createForegroundNotificationBuilder(title, progress))
    }

    /**
     * Builds foreground service info with progress notification and dataSync foreground type on Android Q+.
     *
     * @param title Title displayed on the notification.
     * @param progress Progress percentage (0..100).
     * @return [ForegroundInfo] configured with unique notification ID and foreground type.
     */
    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = createForegroundNotification(title, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    /**
     * Builds completion notification builder directing user to video detail screen with auto-cancel enabled.
     *
     * @param title Title displayed on the completion notification.
     * @param identifier Unique Archive.org item identifier for deep-link navigation.
     * @return [NotificationCompat.Builder] configured with autoCancel and detail navigation pending intent.
     */
    internal fun createCompletionNotificationBuilder(title: String, identifier: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(downloadNavigator.createDetailsPendingIntent(context, identifier))
    }

    /**
     * Builds completion notification directing user to video detail screen with auto-cancel enabled.
     *
     * @param title Title displayed on the completion notification.
     * @param identifier Unique Archive.org item identifier for deep-link navigation.
     * @return [Notification] configured with autoCancel and detail navigation pending intent.
     */
    internal fun createCompletionNotification(title: String, identifier: String): Notification {
        return notificationFactory(createCompletionNotificationBuilder(title, identifier))
    }

    /**
     * Displays a completion notification when video download finishes successfully.
     *
     * @param title Title displayed on the completion notification.
     * @param identifier Unique Archive.org item identifier.
     */
    private fun showCompletionNotification(title: String, identifier: String) {
        val notification = createCompletionNotification(title, identifier)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notification)
    }

    /**
     * Builds failure notification builder directing user to video detail screen with auto-cancel enabled.
     *
     * @param title Title displayed on the failure notification.
     * @param identifier Unique Archive.org item identifier for deep-link navigation.
     * @param reason Failure error description.
     * @return [NotificationCompat.Builder] configured with autoCancel and detail navigation pending intent.
     */
    internal fun createFailureNotificationBuilder(
        title: String,
        identifier: String,
        reason: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("$title: $reason")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(downloadNavigator.createDetailsPendingIntent(context, identifier))
    }

    /**
     * Builds failure notification directing user to video detail screen with auto-cancel enabled.
     *
     * @param title Title displayed on the failure notification.
     * @param identifier Unique Archive.org item identifier for deep-link navigation.
     * @param reason Failure error description.
     * @return [Notification] configured with autoCancel and detail navigation pending intent.
     */
    internal fun createFailureNotification(
        title: String,
        identifier: String,
        reason: String
    ): Notification {
        return notificationFactory(createFailureNotificationBuilder(title, identifier, reason))
    }

    /**
     * Displays a failure notification when video download fails.
     *
     * @param title Title displayed on the failure notification.
     * @param identifier Unique Archive.org item identifier.
     * @param reason Failure error description.
     */
    private fun showFailureNotification(title: String, identifier: String, reason: String) {
        val notification = createFailureNotification(title, identifier, reason)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notification)
    }
}
