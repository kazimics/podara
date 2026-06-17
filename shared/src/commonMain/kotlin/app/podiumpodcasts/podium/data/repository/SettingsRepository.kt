package app.podiumpodcasts.podium.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val settings: Settings) {

    val appearance = Appearance()
    val behavior = Behavior()
    val sync = Sync()
    val privacy = Privacy()
    val debug = Debug()

    inner class Appearance {
        val enableArtworkColors: Flow<Boolean> = getBooleanFlow("appearance_enable_artwork_colors", true)
        suspend fun setEnableArtworkColors(enable: Boolean) = settings.set("appearance_enable_artwork_colors", enable)

        val enableDynamicColors: Flow<Boolean> = getBooleanFlow("appearance_enable_dynamic_colors", true)
        suspend fun setEnableDynamicColors(enable: Boolean) = settings.set("appearance_enable_dynamic_colors", enable)

        val useAlternativeBranding: Flow<Boolean> = getBooleanFlow("appearance_use_alternative_branding", false)
        suspend fun setUseAlternativeBranding(enable: Boolean) = settings.set("appearance_use_alternative_branding", enable)
    }

    inner class Behavior {
        val playerPlaybackSpeed: Flow<Float> = getFloatFlow("behavior_player_playback_speed", 1f)
        suspend fun setPlayerPlaybackSpeed(speed: Float) = settings.set("behavior_player_playback_speed", speed)

        val playerSeekBackIncrement: Flow<Long> = getLongFlow("behavior_player_seek_back_increment", 10000)
        suspend fun setPlayerSeekBackIncrement(increment: Long) = settings.set("behavior_player_seek_back_increment", increment)

        val playerSeekForwardIncrement: Flow<Long> = getLongFlow("behavior_player_seek_forward_increment", 10000)
        suspend fun setPlayerSeekForwardIncrement(increment: Long) = settings.set("behavior_player_seek_forward_increment", increment)

        val updatePodcastsInRoaming: Flow<Boolean> = getBooleanFlow("behavior_update_podcasts_in_roaming", false)
        suspend fun setUpdatePodcastsInRoaming(enable: Boolean) = settings.set("behavior_update_podcasts_in_roaming", enable)

        val updatePodcastsIntervalMinutes: Flow<Int> = getIntFlow("behavior_update_podcasts_interval_minutes", 60)
        suspend fun setUpdatePodcastsIntervalMinutes(minutes: Int) = settings.set("behavior_update_podcasts_interval_minutes", minutes)

        val downloadMetered: Flow<Boolean> = getBooleanFlow("behavior_download_metered", false)
        suspend fun setDownloadMetered(enable: Boolean) = settings.set("behavior_download_metered", enable)

        val downloadInRoaming: Flow<Boolean> = getBooleanFlow("behavior_download_in_roaming", false)
        suspend fun setDownloadInRoaming(enable: Boolean) = settings.set("behavior_download_in_roaming", enable)

        val applySettingsForAutoDownloads: Flow<Boolean> = getBooleanFlow("behavior_auto_downloads_apply_settings", false)
        suspend fun setApplySettingsForAutoDownloads(enable: Boolean) = settings.set("behavior_auto_downloads_apply_settings", enable)

        val deletePlayedDownloads: Flow<Boolean> = getBooleanFlow("behavior_delete_played_downloads", false)
        suspend fun setDeletePlayedDownloads(enabled: Boolean) = settings.set("behavior_delete_played_downloads", enabled)

        val deleteDownloadsAfterSeconds: Flow<Int> = getIntFlow("behavior_delete_downloads_after_seconds", -1)
        suspend fun setDeleteDownloadsAfterSeconds(seconds: Int) = settings.set("behavior_delete_downloads_after_seconds", seconds)

        fun getDiscoverCountryCode(default: String): Flow<String> = getStringFlow("behavior_discover_country_code", default)
        suspend fun setDiscoverCountryCode(cc: String) = settings.set("behavior_discover_country_code", cc)
    }

    inner class Sync {
        val enable: Flow<Boolean> = getBooleanFlow("sync_enable", false)
        suspend fun setEnable(enable: Boolean) = settings.set("sync_enable", enable)

        val type: Flow<String> = getStringFlow("sync_type", "gpodder")
        suspend fun setType(type: String) = settings.set("sync_type", type)

        val baseUrl: Flow<String> = getStringFlow("sync_base_url", "https://gpodder.net")
        suspend fun setBaseUrl(baseUrl: String) = settings.set("sync_base_url", baseUrl)

        val deviceId: Flow<String> = getStringFlow("sync_device_id", "")
        suspend fun setDeviceId(deviceId: String) = settings.set("sync_device_id", deviceId)

        val deviceCaption: Flow<String> = getStringFlow("sync_device_caption", "")
        suspend fun setDeviceCaption(deviceCaption: String) = settings.set("sync_device_caption", deviceCaption)

        val username: Flow<String> = getStringFlow("sync_username", "")
        suspend fun setUsername(username: String) = settings.set("sync_username", username)

        val password: Flow<String> = getStringFlow("sync_password", "")
        suspend fun setPassword(password: String) = settings.set("sync_password", password)

        val auth: Flow<String> = getStringFlow("sync_auth", "")
        suspend fun setAuth(auth: String) = settings.set("sync_auth", auth)

        val timestampSubscriptions: Flow<Long> = getLongFlow("sync_timestamp_subscriptions", 0)
        suspend fun setTimestampSubscriptions(timestamp: Long) = settings.set("sync_timestamp_subscriptions", timestamp)

        val timestampEpisodeActions: Flow<Long> = getLongFlow("sync_timestamp_episode_actions", 0)
        suspend fun setTimestampEpisodeActions(timestamp: Long) = settings.set("sync_timestamp_episode_actions", timestamp)
    }

    inner class Privacy {
        val disableApplePodcastsApi: Flow<Boolean> = getBooleanFlow("privacy_disable_apple_podcasts_api", false)
        suspend fun setDisableApplePodcastsApi(disable: Boolean) = settings.set("privacy_disable_apple_podcasts_api", disable)
    }

    inner class Debug {
        val enableUpdateNotification: Flow<Boolean> = getBooleanFlow("debug_enable_update_notification", false)
        suspend fun setEnableUpdateNotification(enable: Boolean) = settings.set("debug_enable_update_notification", enable)

        val enableNightlyNotification: Flow<Boolean> = getBooleanFlow("debug_enable_nightly_notification", false)
        suspend fun setEnableNightlyNotification(enable: Boolean) = settings.set("debug_enable_nightly_notification", enable)
    }

    private fun getBooleanFlow(key: String, defaultValue: Boolean): Flow<Boolean> {
        val mutableStateFlow = MutableStateFlow(settings.getBoolean(key, defaultValue))
        settings.addBooleanObserver(key) { mutableStateFlow.value = it }
        return mutableStateFlow.asStateFlow()
    }

    private fun getIntFlow(key: String, defaultValue: Int): Flow<Int> {
        val mutableStateFlow = MutableStateFlow(settings.getInt(key, defaultValue))
        settings.addIntObserver(key) { mutableStateFlow.value = it }
        return mutableStateFlow.asStateFlow()
    }

    private fun getLongFlow(key: String, defaultValue: Long): Flow<Long> {
        val mutableStateFlow = MutableStateFlow(settings.getLong(key, defaultValue))
        settings.addLongObserver(key) { mutableStateFlow.value = it }
        return mutableStateFlow.asStateFlow()
    }

    private fun getFloatFlow(key: String, defaultValue: Float): Flow<Float> {
        val mutableStateFlow = MutableStateFlow(settings.getFloat(key, defaultValue))
        settings.addFloatObserver(key) { mutableStateFlow.value = it }
        return mutableStateFlow.asStateFlow()
    }

    private fun getStringFlow(key: String, defaultValue: String): Flow<String> {
        val mutableStateFlow = MutableStateFlow(settings.getString(key, defaultValue))
        settings.addStringObserver(key) { mutableStateFlow.value = it ?: defaultValue }
        return mutableStateFlow.asStateFlow()
    }
}
