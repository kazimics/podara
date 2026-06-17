package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.model.SyncAction
import app.podiumpodcasts.podium.data.repository.SyncRepository
import app.podiumpodcasts.podium.data.repository.SubscriptionRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DeviceUpdate(
    val podcast: String,
    val episode: String,
    val action: String,
    val timestamp: Long,
    val position: Int? = null,
    val total: Int? = null
)

@Serializable
data class DeviceUpdateResponse(
    val update: List<DeviceUpdate> = emptyList()
)

sealed class SyncResult {
    data class Success(val actionCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
    data object NoActions : SyncResult()
}

sealed class PullResult {
    data class Success(val updates: DeviceUpdateResponse) : PullResult()
    data class Error(val message: String) : PullResult()
}

class SyncManager(
    private val syncRepository: SyncRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val client: HttpClient = HttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sync(
        baseUrl: String,
        username: String,
        password: String,
        deviceId: String
    ): SyncResult {
        return try {
            val actions = syncRepository.getAll()
            if (actions.isEmpty()) {
                return SyncResult.NoActions
            }

            val deviceUpdates = actions.map { action ->
                DeviceUpdate(
                    podcast = action.origin,
                    episode = action.audioUrl ?: "",
                    action = action.actionType.lowercase(),
                    timestamp = action.timestamp / 1000,
                    position = action.position,
                    total = action.total
                )
            }

            val response = client.post("$baseUrl/api/2/subscriptions/$username/$deviceId.json") {
                basicAuth(username, password)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(DeviceUpdate.serializer().descriptor.toString(), deviceUpdates))
            }

            if (response.status == HttpStatusCode.OK) {
                actions.forEach { syncRepository.delete(it.id) }
                SyncResult.Success(actions.size)
            } else {
                SyncResult.Error("HTTP ${response.status.value}: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun pullUpdates(
        baseUrl: String,
        username: String,
        password: String,
        deviceId: String,
        timestamp: Long
    ): PullResult {
        return try {
            val response = client.get("$baseUrl/api/2/subscriptions/$username/$deviceId.json?since=$timestamp") {
                basicAuth(username, password)
            }

            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val updates = json.decodeFromString<DeviceUpdateResponse>(body)
                PullResult.Success(updates)
            } else {
                PullResult.Error("HTTP ${response.status.value}: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            PullResult.Error(e.message ?: "Unknown error")
        }
    }
}
