package app.podara.data.model

import kotlinx.serialization.Serializable

enum class SyncActionType {
    PLAY,
    SUBSCRIBE,
    UNSUBSCRIBE
}

@Serializable
data class SyncAction(
    val id: String,
    val actionType: String,
    val origin: String,
    val audioUrl: String? = null,
    val position: Int? = null,
    val total: Int? = null,
    val timestamp: Long
)
