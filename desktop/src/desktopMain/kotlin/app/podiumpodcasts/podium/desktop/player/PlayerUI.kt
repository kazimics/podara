package app.podiumpodcasts.podium.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import app.podiumpodcasts.podium.utils.Strings

@Composable
fun MiniPlayer(
    state: MediaPlayerState,
    onExpand: () -> Unit,
    onShowQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by remember {
        derivedStateOf { state.getProgress() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (state.currentUrl != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onExpand() },
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentTitle ?: Strings["player_no_playback"],
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.currentUrl != null) {
                        Text(
                            text = "${formatTime(state.currentPosition)} / ${formatTime(state.duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { state.seekBack() }) {
                    Icon(Icons.Default.Replay10, contentDescription = Strings["player_seek_back"])
                }

                IconButton(onClick = { state.togglePlayPause() }) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) Strings["player_pause"] else Strings["player_play"]
                    )
                }

                IconButton(onClick = { state.seekForward() }) {
                    Icon(Icons.Default.Forward10, contentDescription = Strings["player_seek_forward"])
                }

                IconButton(onClick = onShowQueue) {
                    Icon(Icons.Default.QueueMusic, contentDescription = Strings["player_queue"])
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayer(
    state: MediaPlayerState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentPosition, state.duration) {
        if (!isDragging && state.duration > 0) {
            sliderPosition = state.currentPosition.toFloat() / state.duration
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = Strings["player_close"])
            }

            Text(
                text = state.currentTitle ?: Strings["player_now_playing"],
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconButton(onClick = { showQueue = true }) {
                Icon(Icons.Default.QueueMusic, contentDescription = Strings["player_queue"])
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                isDragging = true
            },
            onValueChangeFinished = {
                isDragging = false
                val position = (sliderPosition * state.duration).toLong()
                state.seek(position)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(state.currentPosition),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatTime(state.duration),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { state.playPrevious() }) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = Strings["player_previous"],
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { state.seekBack() }) {
                Icon(
                    Icons.Default.Replay10,
                    contentDescription = Strings["player_seek_back"],
                    modifier = Modifier.size(32.dp)
                )
            }

            FilledIconButton(
                onClick = { state.togglePlayPause() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) Strings["player_pause"] else Strings["player_play"],
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { state.seekForward() }) {
                Icon(
                    Icons.Default.Forward10,
                    contentDescription = Strings["player_seek_forward"],
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { state.playNext() }) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = Strings["player_next"],
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpeedSelector(
                currentSpeed = state.playbackSpeed,
                onSpeedSelected = { state.changePlaybackSpeed(it) }
            )

            SleepTimerButton(
                state = state,
                onClick = { showSleepTimer = true }
            )

            VolumeControl(
                currentVolume = state.volume,
                onVolumeChange = { state.changeVolume(it) },
                onToggleMute = { state.toggleMute() }
            )
        }
    }

    if (showQueue) {
        QueueDrawer(
            state = state,
            onDismiss = { showQueue = false }
        )
    }

    if (showSleepTimer) {
        SleepTimerSheet(
            state = state,
            onDismiss = { showSleepTimer = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueDrawer(
    state: MediaPlayerState,
    onDismiss: () -> Unit,
    onDownload: ((QueueItem) -> Unit)? = null,
    onDeleteDownload: ((QueueItem) -> Unit)? = null
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
        )

        Surface(
            modifier = Modifier.fillMaxHeight().width(360.dp).align(Alignment.CenterEnd),
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        if (isSelectionMode) {
                            Text("${selectedIndices.size} selected")
                        } else {
                            Text(Strings.get("player_queue_count", state.queue.size))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = Strings["player_close"])
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedIndices = setOf()
                            } else {
                                isSelectionMode = true
                            }
                        }) {
                            Text(if (isSelectionMode) Strings["dialog_cancel"] else Strings["player_select"])
                        }
                    }
                )

                if (state.queue.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(Strings["player_queue_empty"], color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(state.queue) { index, item ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = item.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (index == state.queueIndex)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingContent = {
                                    if (isSelectionMode) {
                                        Checkbox(
                                            checked = index in selectedIndices,
                                            onCheckedChange = { checked ->
                                                selectedIndices = if (checked) selectedIndices + index
                                                else selectedIndices - index
                                            }
                                        )
                                    } else if (index == state.queueIndex) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                trailingContent = {
                                    if (!isSelectionMode) {
                                        Row {
                                            if (onDownload != null && !item.isDownloaded) {
                                                IconButton(onClick = { onDownload(item) }) {
                                                    Icon(Icons.Default.Download, contentDescription = Strings["episode_download"])
                                                }
                                            }
                                            IconButton(onClick = { state.removeFromQueue(index) }) {
                                                Icon(Icons.Default.Close, contentDescription = Strings["player_remove"])
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.clickable {
                                    if (isSelectionMode) {
                                        selectedIndices = if (index in selectedIndices) selectedIndices - index
                                        else selectedIndices + index
                                    } else {
                                        state.playFromQueue(index)
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }

                if (isSelectionMode && selectedIndices.isNotEmpty()) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (onDownload != null) {
                            TextButton(onClick = {
                                selectedIndices.forEach { idx ->
                                    if (idx in state.queue.indices) onDownload(state.queue[idx])
                                }
                                selectedIndices = setOf()
                                isSelectionMode = false
                            }) {
                                Text(Strings["episode_download"])
                            }
                        }
                        TextButton(onClick = {
                            state.removeSelectedFromQueue(selectedIndices)
                            selectedIndices = setOf()
                            isSelectionMode = false
                        }) {
                            Text(Strings["player_delete"])
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    state: MediaPlayerState,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 10, 15, 20, 30, 45, 60, 90)

    val sleepMinutes = state.sleepTimerMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(Strings["player_sleep_timer"])
                if (sleepMinutes != null) {
                    Text(
                        text = Strings.get("player_sleep_timer_active", sleepMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column {
                if (state.sleepTimerMinutes != null) {
                    TextButton(
                        onClick = {
                            state.cancelSleepTimer()
                            onDismiss()
                        }
                    ) {
                        Text(Strings["player_cancel_timer"])
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                options.forEach { minutes ->
                    TextButton(
                        onClick = {
                            state.setSleepTimer(minutes)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.get("player_minutes", minutes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings["dialog_close"])
            }
        }
    )
}

@Composable
private fun SleepTimerButton(
    state: MediaPlayerState,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Icon(
            Icons.Default.Timer,
            contentDescription = Strings["player_sleep_timer"],
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (state.sleepTimerMinutes != null) "${state.sleepTimerMinutes}m" else Strings["player_timer"]
        )
    }
}

@Composable
private fun SpeedSelector(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("${currentSpeed}x")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = {
                        onSpeedSelected(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun VolumeControl(
    currentVolume: Int,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (currentVolume > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = if (currentVolume > 0) Strings["player_mute"] else Strings["player_unmute"],
            modifier = Modifier.size(20.dp).clickable { onToggleMute() }
        )
        Slider(
            value = currentVolume.toFloat() / 100f,
            onValueChange = { onVolumeChange((it * 100).toInt()) },
            modifier = Modifier.width(100.dp)
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
