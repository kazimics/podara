package app.podiumpodcasts.podium.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ListItem(
    val id: Int = 0,
    val listId: Int,
    val contentId: String,
    val isPodcast: Boolean,
    val position: Int
)
