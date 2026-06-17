package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.model.PodcastEpisodeDownloadState
import app.podiumpodcasts.podium.data.repository.DownloadRepository
import app.podiumpodcasts.podium.utils.sha256
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.readBytes
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadManager(
    private val downloadRepository: DownloadRepository,
    private val downloadsDir: File
) {
    private val client = HttpClient()

    suspend fun downloadEpisode(
        episodeId: String,
        audioUrl: String,
        origin: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                downloadRepository.add(episodeId)
                downloadRepository.updateProgress(
                    episodeId = episodeId,
                    state = PodcastEpisodeDownloadState.DOWNLOADING.value,
                    progress = 0
                )

                val episodeDir = File(downloadsDir, origin.sha256())
                episodeDir.mkdirs()
                val outputFile = File(episodeDir, audioUrl.sha256())

                client.prepareGet(audioUrl).execute { response ->
                    if (!response.status.isSuccess()) {
                        throw Exception("HTTP ${response.status.value}")
                    }

                    val contentLength = response.contentLength() ?: 0L
                    var downloadedBytes = 0L

                    val bytes = response.readBytes()
                    outputFile.writeBytes(bytes)
                    downloadedBytes = bytes.size.toLong()

                    downloadRepository.updateProgress(
                        episodeId = episodeId,
                        state = PodcastEpisodeDownloadState.DOWNLOADING.value,
                        progress = downloadedBytes
                    )
                    onProgress(downloadedBytes, contentLength)
                }

                downloadRepository.updateState(
                    episodeId = episodeId,
                    state = PodcastEpisodeDownloadState.DOWNLOADED.value,
                    filename = outputFile.name,
                    progress = outputFile.length(),
                    size = outputFile.length(),
                    timestamp = kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds()
                )

                Result.success(outputFile)
            } catch (e: Exception) {
                downloadRepository.updateProgress(
                    episodeId = episodeId,
                    state = PodcastEpisodeDownloadState.NOT_DOWNLOADED.value,
                    progress = 0
                )
                Result.failure(e)
            }
        }
    }

    suspend fun deleteEpisodeDownload(
        episodeId: String,
        origin: String,
        audioUrl: String
    ) {
        val episodeDir = File(downloadsDir, origin.sha256())
        val outputFile = File(episodeDir, audioUrl.sha256())
        if (outputFile.exists()) outputFile.delete()

        downloadRepository.delete(episodeId)
    }

    fun getDownloadFile(origin: String, audioUrl: String): File {
        val episodeDir = File(downloadsDir, origin.sha256())
        return File(episodeDir, audioUrl.sha256())
    }

    suspend fun getAllNotDownloaded(): List<String> {
        return downloadRepository.getAllNotDownloaded()
    }
}
