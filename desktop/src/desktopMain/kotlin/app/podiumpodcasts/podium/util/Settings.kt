package app.podiumpodcasts.podium.util

import java.io.File
import java.util.Properties

object Settings {
    private val settingsFile = File(System.getProperty("user.home"), ".podium/settings.properties")
    private val props = Properties()

    init {
        try {
            if (settingsFile.exists()) {
                settingsFile.inputStream().use { props.load(it) }
            }
        } catch (e: Exception) {
            Logger.e("Settings", "Failed to load settings", e)
        }
    }

    fun getDownloadSpeedLimitKbps(): Int {
        val raw = props.getProperty("download_speed_limit_kbps", "0")
        val value = raw.toIntOrNull() ?: return 0
        return maxOf(value, 0)
    }

    fun setDownloadSpeedLimitKbps(limitKbps: Int) {
        props.setProperty("download_speed_limit_kbps", limitKbps.toString())
        save()
    }

    fun getDownloadPath(): String {
        return props.getProperty("download_path", defaultDownloadPath())
    }

    fun setDownloadPath(path: String) {
        props.setProperty("download_path", path)
        save()
    }

    fun resetDownloadPath() {
        props.remove("download_path")
        save()
    }

    fun getLanguage(): String {
        return props.getProperty("language", "en")
    }

    fun setLanguage(language: String) {
        props.setProperty("language", language)
        save()
    }

    private fun save() {
        try {
            settingsFile.parentFile?.mkdirs()
            settingsFile.outputStream().use { props.store(it, "Podium Settings") }
        } catch (e: Exception) {
            Logger.e("Settings", "Failed to save settings", e)
        }
    }

    private fun defaultDownloadPath(): String {
        return File(System.getProperty("user.home"), ".podium/downloads").absolutePath
    }
}
