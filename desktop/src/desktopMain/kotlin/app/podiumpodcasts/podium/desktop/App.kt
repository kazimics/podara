package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val database = remember {
        val userHome = System.getProperty("user.home")
        val dbDir = File(userHome, ".podium")
        dbDir.mkdirs()
        AppDatabase.build(File(dbDir, "podium.db"))
    }

    var podcasts by remember { mutableStateOf(emptyList<Podcast>()) }
    var currentScreen by remember { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        podcasts = database.podcasts.getAllSync()
    }

    PodiumTheme {
        when (currentScreen) {
            "home" -> Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Podium") },
                        actions = {
                            IconButton(onClick = { currentScreen = "settings" }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { padding ->
                if (podcasts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RssFeed, null, Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No podcasts yet", style = MaterialTheme.typography.headlineSmall)
                            Text("Add one to get started!", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                        items(podcasts) { podcast ->
                            ListItem(
                                headlineContent = { Text(podcast.fetchTitle()) },
                                supportingContent = { Text(podcast.author) },
                                leadingContent = { Icon(Icons.Default.Podcasts, null, Modifier.size(48.dp)) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
            "settings" -> SettingsScreen(onBack = { currentScreen = "home" })
        }
    }
}
