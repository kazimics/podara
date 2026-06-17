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

class HomeViewModel(
    private val podcastRepository: PodcastRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _podcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val podcasts: StateFlow<List<Podcast>> = _podcasts.asStateFlow()

    init {
        scope.launch {
            podcastRepository.getAll().collect { _podcasts.value = it }
        }
    }
}
