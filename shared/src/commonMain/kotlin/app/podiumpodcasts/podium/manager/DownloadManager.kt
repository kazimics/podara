package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.DownloadTask
import app.podiumpodcasts.podium.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "DownloadManager"

class DownloadManager(
    private val db: AppDatabase,
    private val downloadsDir: File,
    private val speedLimitKbps: Int = 0
) {
    // ── Thread-safe pause/cancel tracking ──
    private val pausedDownloads = ConcurrentHashMap.newKeySet<String>()
    private val cancelledDownloads = ConcurrentHashMap.newKeySet<String>()

    /**
     * Download an episode from [audioUrl] and save to local storage.
     * Supports pause via [isPaused] callback and HTTP Range resume.
     */
    suspend fun downloadEpisode(
        episodeId: String,
        audioUrl: String,
        origin: String,
        episodeTitle: String = episodeId,
        podcastTitle: String = "Unknown",
        isPaused: () -> Boolean = { episodeId in pausedDownloads },
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val podcastDir = File(downloadsDir, sanitizeFileName(podcastTitle))
            podcastDir.mkdirs()
            val ext = audioUrl.substringAfterLast('.', "mp3").substringBefore('?')
            val outputFile = File(podcastDir, "${sanitizeFileName(episodeTitle)}.$ext")

            // Check if we have a paused task to resume from
            val existingTask = db.downloadTasks.getByEpisodeId(episodeId)
            var downloadedBytes = 0L
            var totalBytes = 0L

            if (existingTask != null && existingTask.state == "PAUSED" && existingTask.downloadedBytes > 0) {
                // Resume from partial download
                downloadedBytes = existingTask.downloadedBytes
                totalBytes = existingTask.totalBytes
                db.downloadTasks.updateState(episodeId, "DOWNLOADING")
                Logger.i(TAG, "Resuming download from byte $downloadedBytes: ${outputFile.absolutePath}")
            } else {
                // Fresh download — create task record
                val now = System.currentTimeMillis()
                db.downloadTasks.insert(DownloadTask(
                    episodeId = episodeId, origin = origin, audioUrl = audioUrl,
                    podcastTitle = podcastTitle, episodeTitle = episodeTitle,
                    targetFilePath = outputFile.absolutePath, downloadedBytes = 0, totalBytes = 0,
                    state = "DOWNLOADING", createdAt = now, updatedAt = now
                ))
                // Delete partial file if exists
                if (outputFile.exists()) outputFile.delete()
            }

            Logger.i(TAG, "Downloading to: ${outputFile.absolutePath}")
            onProgress?.invoke(downloadedBytes, totalBytes)

            val url = URL(audioUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000

            // Set Range header for resume
            if (downloadedBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
            }

            val contentLength = connection.contentLength.toLong()
            if (totalBytes == 0L) totalBytes = contentLength
            if (totalBytes == 0L && downloadedBytes > 0) {
                // Server may not return content-length with Range — use original task total
                totalBytes = existingTask?.totalBytes ?: 0L
            }

            Logger.i(TAG, "Content-Length: $contentLength, total: $totalBytes, resume offset: $downloadedBytes")

            // Open input stream (for Range response, server returns 206 — inputStream reads from offset)
            val inputStream = connection.inputStream
            val buffer = ByteArray(8192)

            val fileOutputStream = if (downloadedBytes > 0) {
                java.io.FileOutputStream(outputFile, true) // append mode for resume
            } else {
                outputFile.outputStream() // overwrite for fresh download
            }

            // Per-task rate limiter
            val rateLimiter = if (speedLimitKbps > 0) {
                RateLimiter(speedLimitKbps.toLong() * 1024)
            } else null

            try {
                while (true) {
                    if (cancelledDownloads.remove(episodeId)) {
                        // Cancelled — clean up partial file
                        fileOutputStream.close()
                        inputStream.close()
                        outputFile.delete()
                        db.downloadTasks.delete(episodeId)
                        Logger.i(TAG, "Download cancelled: $episodeId")
                        return@withContext Result.failure(Exception("Download cancelled"))
                    }

                    if (isPaused()) {
                        // Paused — save state and keep partial file
                        fileOutputStream.close()
                        inputStream.close()
                        pausedDownloads.remove(episodeId) // clear pause flag so future pause works correctly
                        db.downloadTasks.updateProgress(episodeId, downloadedBytes, totalBytes)
                        db.downloadTasks.updateState(episodeId, "PAUSED")
                        Logger.i(TAG, "Download paused: $episodeId (${downloadedBytes}/${totalBytes})")
                        return@withContext Result.failure(Exception("Download paused"))
                    }

                    if (!isActive) {
                        // Coroutine cancelled externally
                        fileOutputStream.close()
                        inputStream.close()
                        outputFile.delete()
                        db.downloadTasks.delete(episodeId)
                        Logger.i(TAG, "Download cancelled (coroutine): $episodeId")
                        return@withContext Result.failure(Exception("Download cancelled"))
                    }

                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    fileOutputStream.write(buffer, 0, read)
                    downloadedBytes += read
                    onProgress?.invoke(downloadedBytes, totalBytes)
                    rateLimiter?.throttle(read) { episodeId in pausedDownloads || episodeId in cancelledDownloads }
                }
            } finally {
                fileOutputStream.close()
                inputStream.close()
            }

            Logger.i(TAG, "Download complete: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            db.downloads.insert(
                episodeId = episodeId, origin = origin,
                filePath = outputFile.absolutePath,
                podcastTitle = podcastTitle, episodeTitle = episodeTitle
            )
            db.downloadTasks.delete(episodeId) // clean up task
            cancelledDownloads.remove(episodeId)
            pausedDownloads.remove(episodeId)
            Result.success(outputFile)
        } catch (e: Exception) {
            Logger.e(TAG, "Download failed: $audioUrl", e)
            cancelledDownloads.remove(episodeId)
            pausedDownloads.remove(episodeId)
            // Mark task as FAILED (not cancelled/paused)
            try {
                val task = db.downloadTasks.getByEpisodeId(episodeId)
                if (task != null && task.state == "DOWNLOADING") {
                    db.downloadTasks.updateState(episodeId, "FAILED")
                }
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    /** Pause a running download — keeps partial file on disk. */
    fun pauseDownload(episodeId: String) {
        pausedDownloads.add(episodeId)
    }

    /** Resume a previously paused download. Returns success if download completes. */
    suspend fun resumeDownload(
        episodeId: String,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val task = db.downloadTasks.getByEpisodeId(episodeId)
            ?: return@withContext Result.failure(Exception("No paused task found for $episodeId"))

        val partialFile = File(task.targetFilePath)
        if (!partialFile.exists()) {
            // Partial file was deleted — restart from scratch
            db.downloadTasks.delete(episodeId)
            return@withContext downloadEpisode(
                episodeId = task.episodeId, audioUrl = task.audioUrl,
                origin = task.origin, episodeTitle = task.episodeTitle,
                podcastTitle = task.podcastTitle, onProgress = onProgress
            )
        }

        // Clear the pause flag so downloadEpisode doesn't immediately pause again
        pausedDownloads.remove(episodeId)

        Logger.i(TAG, "Resuming download: $episodeId from byte ${task.downloadedBytes}")

        // Delegate to downloadEpisode which handles resume logic
        downloadEpisode(
            episodeId = task.episodeId, audioUrl = task.audioUrl,
            origin = task.origin, episodeTitle = task.episodeTitle,
            podcastTitle = task.podcastTitle, onProgress = onProgress
        )
    }

    /**
     * Delete a downloaded episode: remove the file, the DB record, and any lingering task.
     * @return true if the record was found and deleted (file may already have been missing).
     */
    suspend fun deleteDownloadedEpisode(episodeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = db.downloads.getByEpisodeId(episodeId)
            if (record != null) {
                val file = File(record.filePath)
                if (file.exists()) {
                    file.delete()
                    Logger.i(TAG, "Deleted file: ${file.absolutePath}")
                }
            }
            db.downloads.delete(episodeId)
            db.downloadTasks.delete(episodeId)
            cancelledDownloads.remove(episodeId)
            pausedDownloads.remove(episodeId)
            Logger.i(TAG, "Deleted download record: $episodeId")
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete download: $episodeId", e)
            false
        }
    }

    /**
     * Delete all downloaded episodes for a podcast origin:
     * removes files, DB records, and any lingering tasks.
     * @return number of records deleted.
     */
    suspend fun deleteDownloadedByOrigin(origin: String): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val records = db.downloads.getAllByOrigin(origin)
            for (record in records) {
                val file = File(record.filePath)
                if (file.exists()) {
                    file.delete()
                }
                db.downloads.delete(record.episodeId)
                db.downloadTasks.delete(record.episodeId)
                cancelledDownloads.remove(record.episodeId)
                pausedDownloads.remove(record.episodeId)
                count++
            }
            Logger.i(TAG, "Deleted $count downloads for origin: $origin")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete downloads for: $origin", e)
        }
        count
    }

    /** Cancel an active download. Cleans up the partial file and DB task immediately. */
    fun cancelDownload(episodeId: String) {
        cancelledDownloads.add(episodeId)
        pausedDownloads.remove(episodeId)
    }

    /**
     * Clean up a paused or failed download task: removes the partial file,
     * the DB task record, and any lingering download record.
     * This is needed when the user cancels a task that is already paused/failed
     * (where the download coroutine has already finished).
     */
    suspend fun cleanupPausedTask(episodeId: String) {
        try {
            val task = db.downloadTasks.getByEpisodeId(episodeId)
            if (task != null) {
                val partialFile = File(task.targetFilePath)
                if (partialFile.exists()) {
                    partialFile.delete()
                    Logger.i(TAG, "Cleaned up partial file: ${partialFile.absolutePath}")
                }
                db.downloadTasks.delete(episodeId)
            }
            db.downloads.delete(episodeId)
            cancelledDownloads.remove(episodeId)
            pausedDownloads.remove(episodeId)
            Logger.i(TAG, "Cleaned up paused task: $episodeId")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to clean up paused task: $episodeId", e)
        }
    }

    fun getDownloadFile(origin: String, audioUrl: String, episodeTitle: String = "", podcastTitle: String = ""): File {
        if (episodeTitle.isNotEmpty() && podcastTitle.isNotEmpty()) {
            val podcastDir = File(downloadsDir, sanitizeFileName(podcastTitle))
            val ext = audioUrl.substringAfterLast('.', "mp3").substringBefore('?')
            return File(podcastDir, "${sanitizeFileName(episodeTitle)}.$ext")
        }
        val episodeDir = File(downloadsDir, origin.sha256())
        val ext = audioUrl.substringAfterLast('.', "mp3").substringBefore('?')
        return File(episodeDir, "${audioUrl.sha256()}.$ext")
    }

    fun sanitizeFileName(name: String): String {
        val illegal = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        var result = name
        for (c in illegal) {
            result = result.replace(c, '_')
        }
        return result.trim()
    }
}

/**
 * Per-task rate limiter using cumulative byte tracking.
 * Suspends the caller when actual throughput exceeds [limitBps],
 * checking [shouldStop] periodically so pause/cancel are responsive.
 */
private class RateLimiter(private val limitBps: Long) {
    private var accumulatedBytes = 0L
    private var windowStartNanos = System.nanoTime()

    /**
     * Throttle after reading [bytesRead] bytes. Suspends the current coroutine
     * if the average rate exceeds [limitBps] since construction.
     *
     * @param shouldStop called periodically during long waits — return true
     *   to abort the wait early (e.g. when pause/cancel is requested).
     */
    suspend fun throttle(bytesRead: Int, shouldStop: () -> Boolean = { false }) {
        if (bytesRead <= 0) return
        accumulatedBytes += bytesRead

        val expectedNanos = accumulatedBytes * 1_000_000_000L / limitBps
        val elapsedNanos = System.nanoTime() - windowStartNanos
        var waitNanos = expectedNanos - elapsedNanos

        while (waitNanos > 0) {
            if (shouldStop()) return
            // Cap each delay chunk at 200ms so pause/cancel can be checked promptly
            val chunk = minOf(waitNanos, 200_000_000L)
            delay(chunk / 1_000_000)
            // Recompute remaining wait after the chunk
            val newElapsed = System.nanoTime() - windowStartNanos
            waitNanos = expectedNanos - newElapsed
        }
    }
}

fun String.sha256(): String {
    return java.security.MessageDigest.getInstance("SHA-256").digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
}
