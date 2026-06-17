package app.podiumpodcasts.podium.ui.route

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.repository.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val enableArtworkColors by settingsRepository.appearance.enableArtworkColors.collectAsState(initial = true)
    val playerPlaybackSpeed by settingsRepository.behavior.playerPlaybackSpeed.collectAsState(initial = 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable artwork colors")
                Switch(
                    checked = enableArtworkColors,
                    onCheckedChange = {
                        kotlinx.coroutines.MainScope().launch {
                            settingsRepository.appearance.setEnableArtworkColors(it)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Playback speed: ${playerPlaybackSpeed}x")

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Podium - Podcast Player",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@kotlinx.serialization.Serializable
object Settings
