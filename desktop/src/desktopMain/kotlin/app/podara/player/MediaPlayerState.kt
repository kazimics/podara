package app.podara.player

import app.podara.data.AppDatabase
import app.podara.data.PlayerQueueRow
import app.podara.util.Logger
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

private const val TAG = "MediaPlayerState"

data class QueueItem(
    val url: String,
    val title: String,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    val podcastArtworkUrl: String? = null,
    val episodeId: String? = null,
    val isDownloaded: Boolean = false
)

class MediaPlayerState(
    private val player: AudioPlayerEngine = MpvAudioPlayerEngine()
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var sleepTimerJob: Job? = null

    var isPlaying by mutableStateOf(false)
        private set
    var currentPosition by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(1L)
        private set
    var volume by mutableStateOf(100)
        private set
    private var previousVolumeBeforeMute = 100
    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var isUserPaused = false
    private var lastPlayStartMs = 0L

    var currentUrl by mutableStateOf<String?>(null)
        private set
    var currentTitle by mutableStateOf<String?>(null)
        private set
    var currentSubtitle by mutableStateOf<String?>(null)
        private set
    var currentArtworkUrl by mutableStateOf<String?>(null)
        private set
    var currentEpisodeId by mutableStateOf<String?>(null)
        private set

    var sleepTimerTrigger by mutableStateOf<Long?>(null)
        private set
    var sleepTimerMinutes by mutableStateOf<Int?>(null)
        private set

    val queue = mutableStateListOf<QueueItem>()
    var queueIndex by mutableIntStateOf(-1)
        private set

    init {
        Logger.d(TAG, "MediaPlayerState initialized")
        player.onPlayStateChanged = { playing ->
            Logger.d(TAG, "Play state changed: playing=$playing")
            isPlaying = playing
            isLoading = false
            if (!playing) {
                val elapsed = System.currentTimeMillis() - lastPlayStartMs
                if (elapsed < 3000) {
                    Logger.d(TAG, "Ignoring playState(false) — $elapsed ms since last play(), likely stale")
                } else if (!isUserPaused) {
                    Logger.i(TAG, "Auto-advancing to next track")
                    playNext()
                }
            }
        }
        player.onPositionChanged = { pos, dur ->
            currentPosition = pos
            duration = dur
        }
        player.onError = { msg ->
            Logger.e(TAG, "Playback error: $msg")
            error = msg
            isLoading = false
        }
    }

    fun play(url: String, title: String? = null, subtitle: String? = null, artworkUrl: String? = null, podcastArtworkUrl: String? = null, durationMs: Long = 0L, episodeId: String? = null) {
        Logger.i(TAG, "play() title=$title, url=$url, durationMs=$durationMs")
        currentUrl = url
        currentTitle = title
        currentSubtitle = subtitle
        currentArtworkUrl = artworkUrl ?: podcastArtworkUrl
        currentEpisodeId = episodeId
        isLoading = true
        error = null
        isUserPaused = false

        val existingIndex = queue.indexOfFirst { it.url == url }
        if (existingIndex >= 0) {
            queueIndex = existingIndex
        } else {
            queue.add(QueueItem(url, title ?: "Unknown", subtitle = subtitle, artworkUrl = currentArtworkUrl, podcastArtworkUrl = podcastArtworkUrl))
            queueIndex = queue.size - 1
        }

        player.play(url, durationMs = durationMs)
        // Record when playback started so the onPlayStateChanged(false) handler
        // can distinguish false transitions (loading glitches, stale EOF from a
        // previous file after playNext()) from real EOF. Transitions within 3s
        // are considered stale and ignored.
        lastPlayStartMs = System.currentTimeMillis()
    }

    fun playFromQueue(index: Int) {
        if (index < 0 || index >= queue.size) {
            Logger.w(TAG, "playFromQueue: invalid index=$index, queueSize=${queue.size}")
            return
        }
        queueIndex = index
        val item = queue[index]
        Logger.i(TAG, "playFromQueue: index=$index, title=${item.title}")
        play(item.url, item.title, item.subtitle, item.artworkUrl, item.podcastArtworkUrl, episodeId = item.episodeId)
    }

    fun addToQueue(
        url: String,
        title: String,
        artworkUrl: String? = null,
        podcastArtworkUrl: String? = null,
        episodeId: String? = null,
        isDownloaded: Boolean = false
    ) {
        Logger.d(TAG, "addToQueue: title=$title")
        queue.add(QueueItem(url, title, artworkUrl = artworkUrl, podcastArtworkUrl = podcastArtworkUrl, episodeId = episodeId, isDownloaded = isDownloaded))
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= queue.size) return
        Logger.d(TAG, "removeFromQueue: index=$index")
        val wasPlaying = index == queueIndex
        queue.removeAt(index)
        if (wasPlaying) {
            if (queue.isNotEmpty()) {
                val nextIndex = index.coerceIn(0, queue.size - 1)
                queueIndex = nextIndex
                val item = queue[nextIndex]
                play(item.url, item.title, item.artworkUrl, episodeId = item.episodeId)
            } else {
                queueIndex = -1
                stop()
            }
        } else if (index < queueIndex) {
            queueIndex--
        }
    }

    fun playNext() {
        if (queueIndex + 1 < queue.size) {
            playFromQueue(queueIndex + 1)
        } else {
            Logger.d(TAG, "playNext: no more items in queue")
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in queue.indices || toIndex !in queue.indices) return
        val item = queue.removeAt(fromIndex)
        queue.add(toIndex, item)
        if (queueIndex == fromIndex) {
            queueIndex = toIndex
        } else {
            if (fromIndex < queueIndex && toIndex >= queueIndex) queueIndex--
            else if (fromIndex > queueIndex && toIndex <= queueIndex) queueIndex++
        }
    }

    fun removeSelectedFromQueue(selectedIndices: Set<Int>) {
        val wasPlayingSelected = queueIndex in selectedIndices
        val sorted = selectedIndices.sortedDescending()
        for (index in sorted) {
            if (index in queue.indices) {
                queue.removeAt(index)
            }
        }
        if (wasPlayingSelected) {
            if (queue.isNotEmpty()) {
                queueIndex = queueIndex.coerceIn(0, queue.size - 1)
                val item = queue[queueIndex]
                play(item.url, item.title, item.artworkUrl, episodeId = item.episodeId)
            } else {
                queueIndex = -1
                stop()
            }
        } else {
            queueIndex = if (queue.isEmpty()) -1
            else queueIndex.coerceIn(0, queue.size - 1)
        }
    }

    fun clearQueue() {
        queue.clear()
        queueIndex = -1
        stop()
    }

    fun playPrevious() {
        if (currentPosition > 3000) {
            Logger.d(TAG, "playPrevious: restarting current track (pos=${currentPosition}ms)")
            seek(0)
        } else if (queueIndex > 0) {
            playFromQueue(queueIndex - 1)
        } else {
            Logger.d(TAG, "playPrevious: at beginning of queue")
        }
    }

    fun pause() {
        Logger.d(TAG, "pause()")
        isUserPaused = true
        player.pause()
    }

    fun resume() {
        Logger.d(TAG, "resume()")
        isUserPaused = false
        player.resume()
    }

    fun togglePlayPause() {
        Logger.d(TAG, "togglePlayPause: isPlaying=$isPlaying")
        if (isPlaying) pause() else resume()
    }

    fun stop() {
        Logger.d(TAG, "stop()")
        player.stop()
        currentUrl = null
        currentTitle = null
        currentArtworkUrl = null
        currentEpisodeId = null
    }

    fun seek(positionMs: Long) {
        player.seek(positionMs)
    }

    fun seekBack(incrementMs: Long = 10000L) {
        val newPos = (currentPosition - incrementMs).coerceAtLeast(0L)
        seek(newPos)
    }

    fun seekForward(incrementMs: Long = 10000L) {
        val newPos = (currentPosition + incrementMs).coerceAtMost(duration)
        seek(newPos)
    }

    @Suppress("FunctionName")
    fun changeVolume(vol: Int) {
        player.setVolume(vol)
        volume = vol
    }

    fun toggleMute() {
        if (volume > 0) {
            previousVolumeBeforeMute = volume
            changeVolume(0)
        } else {
            changeVolume(previousVolumeBeforeMute)
        }
    }

    @Suppress("FunctionName")
    fun changePlaybackSpeed(speed: Float) {
        player.setSpeed(speed)
        playbackSpeed = speed
        Logger.d(TAG, "changePlaybackSpeed: speed=$speed")
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            Logger.d(TAG, "setSleepTimer: cancelled")
            sleepTimerTrigger = null
            sleepTimerMinutes = null
            return
        }
        val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        sleepTimerTrigger = triggerTime
        sleepTimerMinutes = minutes
        Logger.i(TAG, "setSleepTimer: ${minutes} minutes")

        sleepTimerJob = scope.launch {
            delay(TimeUnit.MINUTES.toMillis(minutes.toLong()))
            withContext(Dispatchers.Main) {
                Logger.i(TAG, "Sleep timer triggered, pausing playback")
                pause()
                sleepTimerTrigger = null
                sleepTimerMinutes = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerTrigger = null
        sleepTimerMinutes = null
    }

    fun getProgress(): Float {
        return if (duration > 0) {
            currentPosition.toFloat() / duration
        } else 0f
    }

    // ── Session persistence ──

    /**
     * Save current queue and playback state to database.
     */
    suspend fun saveSession(database: AppDatabase) {
        Logger.d(TAG, "saveSession: saving queue (${queue.size} items), queueIndex=$queueIndex, pos=${currentPosition}ms")
        val rows = queue.mapIndexed { index, item ->
            PlayerQueueRow(
                queueOrder = index,
                url = item.url,
                title = item.title,
                subtitle = item.subtitle,
                artworkUrl = item.artworkUrl,
                podcastArtworkUrl = item.podcastArtworkUrl,
                episodeId = item.episodeId,
                isDownloaded = item.isDownloaded
            )
        }
        database.playerQueue.saveQueue(rows)
        database.playerSession.saveSession(
            queueIndex = queueIndex,
            currentPositionMs = currentPosition,
            playbackSpeed = playbackSpeed,
            volume = volume,
            currentEpisodeId = currentEpisodeId
        )
    }

    /**
     * Restore queue and playback state from database.
     * Restores to a paused state at the saved position, ready for the user to tap play.
     */
    suspend fun restoreSession(database: AppDatabase) {
        val session = database.playerSession.loadSession() ?: run {
            Logger.d(TAG, "restoreSession: no saved session found")
            return
        }
        val rows = database.playerQueue.loadQueue()
        Logger.d(TAG, "restoreSession: loaded ${rows.size} queue items, queueIndex=${session.queueIndex}")

        // Restore speed and volume regardless
        playbackSpeed = session.playbackSpeed
        volume = session.volume
        changePlaybackSpeed(session.playbackSpeed)
        changeVolume(session.volume)

        if (rows.isEmpty()) {
            Logger.d(TAG, "restoreSession: empty queue, nothing to restore")
            return
        }

        // Rebuild queue in-memory
        queue.clear()
        rows.forEach { row ->
            queue.add(QueueItem(
                url = row.url,
                title = row.title,
                subtitle = row.subtitle,
                artworkUrl = row.artworkUrl,
                podcastArtworkUrl = row.podcastArtworkUrl,
                episodeId = row.episodeId,
                isDownloaded = row.isDownloaded
            ))
        }

        val idx = session.queueIndex.coerceIn(0, queue.size - 1)
        queueIndex = idx
        val item = queue[idx]

        // Restore current display state
        currentUrl = item.url
        currentTitle = item.title
        currentSubtitle = item.subtitle
        currentArtworkUrl = item.artworkUrl ?: item.podcastArtworkUrl
        currentEpisodeId = item.episodeId

        // Load into player at saved position, then pause.
        // IMPORTANT: set isUserPaused BEFORE pause() so the onPlayStateChanged(false)
        // callback does NOT trigger playNext().
        Logger.i(TAG, "restoreSession: restoring playback at idx=$idx, pos=${session.currentPositionMs}ms")
        isUserPaused = true
        player.play(item.url, startPositionMs = session.currentPositionMs, durationMs = 0L)
        player.pause()
        isPlaying = false
    }

    /**
     * Lightweight position-only save for periodic heartbeats.
     */
    suspend fun savePosition(database: AppDatabase) {
        database.playerSession.updatePosition(currentPositionMs = currentPosition)
    }

    fun release() {
        Logger.d(TAG, "release()")
        sleepTimerJob?.cancel()
        scope.cancel()
        player.release()
    }
}
