package app.podiumpodcasts.podium.ui.viewmodel

import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.data.repository.EpisodeRepository
import app.podiumpodcasts.podium.data.repository.PodcastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastDetailViewModel(
    private val origin: String,
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _podcast = MutableStateFlow<Podcast?>(null)
    val podcast: StateFlow<Podcast?> = _podcast.asStateFlow()

    private val _episodes = MutableStateFlow<List<PodcastEpisodeBundle>>(emptyList())
    val episodes: StateFlow<List<PodcastEpisodeBundle>> = _episodes.asStateFlow()

    init {
        scope.launch {
            podcastRepository.get(origin).collect { _podcast.value = it }
        }
        scope.launch {
            episodeRepository.getAllByOrigin(origin).collect { _episodes.value = it }
        }
    }
}
