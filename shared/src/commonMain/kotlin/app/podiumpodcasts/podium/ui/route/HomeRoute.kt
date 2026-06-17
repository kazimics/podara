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
import app.podiumpodcasts.podium.data.repository.PodcastRepository
import app.podiumpodcasts.podium.ui.component.PodcastListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    podcastRepository: PodcastRepository,
    onClickPodcast: (String) -> Unit,
    onClickSettings: () -> Unit
) {
    val podcasts by podcastRepository.getAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podium") },
                actions = {
                    IconButton(onClick = onClickSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (podcasts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No podcasts yet.\nAdd one to get started!",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(podcasts) { podcast ->
                    PodcastListItem(
                        podcast = podcast,
                        onClick = { onClickPodcast(podcast.origin) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@kotlinx.serialization.Serializable
object Home
