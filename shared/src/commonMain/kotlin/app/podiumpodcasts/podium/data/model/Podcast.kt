package app.podiumpodcasts.podium.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Podcast(
    val origin: String,
    val link: String,
    val title: String,
    val description: String,
    val author: String,
    val imageUrl: String,
    val imageSeedColor: Int = 0,
    val languageCode: String,
    val fileSize: Long = 0,
    val overrideTitle: String = "",
    val skipBeginning: Int = 0,
    val skipEnding: Int = 0
) {
    fun fetchTitle(): String {
        return overrideTitle.ifBlank { title }
    }
}
