package app.podiumpodcasts.podium.ui.route

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.repository.EpisodeRepository
import app.podiumpodcasts.podium.ui.component.EpisodeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailRoute(
    episodeId: String,
    episodeRepository: EpisodeRepository,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    val bundle by episodeRepository.getById(episodeId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bundle?.episode?.title ?: "") },
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
        bundle?.let { episodeBundle ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = episodeBundle.episode.title,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = episodeBundle.episode.podcastTitle,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (episodeBundle.episode.duration > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Duration: ${formatDuration(episodeBundle.episode.duration)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onPlay(episodeBundle.episode.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = episodeBundle.episode.description,
                    style = MaterialTheme.typography.bodyMedium
                )
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

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, secs)
        else -> "%d:%02d".format(minutes, secs)
    }
}

@kotlinx.serialization.Serializable
data class EpisodeDetail(val episodeId: String)
