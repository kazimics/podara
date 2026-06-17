package app.podiumpodcasts.podium.ui.viewmodel

import app.podiumpodcasts.podium.data.model.PodcastEpisodeBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaPlayerViewModel {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentEpisode = MutableStateFlow<PodcastEpisodeBundle?>(null)
    val currentEpisode: StateFlow<PodcastEpisodeBundle?> = _currentEpisode.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun play(episode: PodcastEpisodeBundle) {
        _currentEpisode.value = episode
        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun resume() {
        _isPlaying.value = true
    }

    fun seek(position: Long) {
        _currentPosition.value = position
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun updateProgress(position: Long, duration: Long) {
        _currentPosition.value = position
        _duration.value = duration
    }
}
