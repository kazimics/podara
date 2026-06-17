package app.podiumpodcasts.podium.data.model

import kotlinx.serialization.Serializable

enum class SystemLists(val id: Int, val label: String) {
    HEAR_LATER(-1, "Hear Later"),
    FAVORITES(-2, "Favorites")
}

@Serializable
data class ListModel(
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val itemCount: Int = 0,
    val imageUrls: String? = null,
    val createdAt: Long = 0,
    val isSystemList: Boolean = false
) {
    fun systemList(): SystemLists? {
        if (!isSystemList) return null
        return SystemLists.entries.first { it.id == id }
    }

    fun getImageUrls(): List<String> {
        return imageUrls?.split("\n") ?: listOf()
    }
}
