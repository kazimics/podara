package app.podiumpodcasts.podium.ui.route

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.data.repository.EpisodeRepository
import app.podiumpodcasts.podium.data.repository.PodcastRepository
import app.podiumpodcasts.podium.ui.component.EpisodeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailRoute(
    origin: String,
    podcastRepository: PodcastRepository,
    episodeRepository: EpisodeRepository,
    onClickEpisode: (String) -> Unit,
    onBack: () -> Unit
) {
    val podcast by podcastRepository.get(origin).collectAsState(initial = null)
    val episodes by episodeRepository.getAllByOrigin(origin).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(podcast?.fetchTitle() ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        podcast?.let { podcastData ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = podcastData.author,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = podcastData.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                }

                items(episodes) { bundle ->
                    EpisodeListItem(
                        bundle = bundle,
                        onClick = { onClickEpisode(bundle.episode.id) }
                    )
                    HorizontalDivider()
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@kotlinx.serialization.Serializable
data class PodcastDetail(val origin: String)
