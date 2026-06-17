package app.podiumpodcasts.podium.desktop.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

data class QueueItem(
    val url: String,
    val title: String,
    val artworkUrl: String? = null
)

class MediaPlayerState {

    private val player = VlcjMediaPlayer()
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
    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var currentUrl by mutableStateOf<String?>(null)
        private set
    var currentTitle by mutableStateOf<String?>(null)
        private set
    var currentArtworkUrl by mutableStateOf<String?>(null)
        private set

    var sleepTimerTrigger by mutableStateOf<Long?>(null)
        private set
    var sleepTimerMinutes by mutableStateOf<Int?>(null)
        private set

    val queue = mutableStateListOf<QueueItem>()
    var queueIndex by mutableIntStateOf(-1)
        private set

    init {
        player.onPlayStateChanged = { playing ->
            isPlaying = playing
            isLoading = false
        }
        player.onPositionChanged = { pos, dur ->
            currentPosition = pos
            duration = dur
        }
        player.onError = { msg ->
            error = msg
            isLoading = false
        }
    }

    fun play(url: String, title: String? = null, artworkUrl: String? = null) {
        currentUrl = url
        currentTitle = title
        currentArtworkUrl = artworkUrl
        isLoading = true
        error = null
        player.play(url)
    }

    fun playFromQueue(index: Int) {
        if (index < 0 || index >= queue.size) return
        queueIndex = index
        val item = queue[index]
        play(item.url, item.title, item.artworkUrl)
    }

    fun addToQueue(url: String, title: String, artworkUrl: String? = null) {
        queue.add(QueueItem(url, title, artworkUrl))
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= queue.size) return
        queue.removeAt(index)
        if (index < queueIndex) {
            queueIndex--
        } else if (index == queueIndex) {
            if (queue.isNotEmpty()) {
                val newIndex = queueIndex.coerceAtMost(queue.size - 1)
                queueIndex = newIndex
                val item = queue[newIndex]
                play(item.url, item.title, item.artworkUrl)
            } else {
                queueIndex = -1
                stop()
            }
        }
    }

    fun playNext() {
        if (queueIndex + 1 < queue.size) {
            playFromQueue(queueIndex + 1)
        }
    }

    fun playPrevious() {
        if (currentPosition > 3000) {
            seek(0)
        } else if (queueIndex > 0) {
            playFromQueue(queueIndex - 1)
        }
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.resume()
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else resume()
    }

    fun stop() {
        player.stop()
        currentUrl = null
        currentTitle = null
        currentArtworkUrl = null
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

    @Suppress("FunctionName")
    fun changePlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        playbackSpeed = speed
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            sleepTimerTrigger = null
            sleepTimerMinutes = null
            return
        }
        val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        sleepTimerTrigger = triggerTime
        sleepTimerMinutes = minutes

        sleepTimerJob = scope.launch {
            delay(TimeUnit.MINUTES.toMillis(minutes.toLong()))
            withContext(Dispatchers.Main) {
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

    fun release() {
        sleepTimerJob?.cancel()
        scope.cancel()
        player.release()
    }
}
