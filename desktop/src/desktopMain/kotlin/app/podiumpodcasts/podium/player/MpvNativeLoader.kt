package app.podiumpodcasts.podium.player

import app.podiumpodcasts.podium.util.Logger
import java.io.File

private const val TAG = "MpvNativeLoader"

object MpvNativeLoader {
    private var loaded = false

    fun load(): Boolean {
        if (loaded) return true
        synchronized(this) {
            if (loaded) return true
            try {
                val nativeDir = File(System.getProperty("user.home"), ".podium/native")
                nativeDir.mkdirs()

                val dllName = "mpv-1.dll"
                val outFile = File(nativeDir, dllName)

                if (!outFile.exists()) {
                    // 1. Try filesystem paths (development & legacy)
                    val baseDir = File(System.getProperty("user.dir"))
                    val libDirs = listOf(
                        File(baseDir, "libs"),
                        File(baseDir, "../libs"),
                        File(baseDir, "../../libs"),
                        File(baseDir, "app/libs"),
                        File(baseDir, "app")
                    )

                    var copied = false
                    for (libDir in libDirs) {
                        val srcFile = File(libDir, dllName)
                        if (srcFile.exists()) {
                            srcFile.copyTo(outFile, overwrite = true)
                            Logger.d(TAG, "Copied from filesystem: $dllName (${outFile.length()} bytes)")
                            copied = true
                            break
                        }
                    }

                    // 2. Fallback: extract from classpath (packaged distribution)
                    if (!copied) {
                        val resourcePath = "mpv-1.dll"
                        val stream = this::class.java.classLoader.getResourceAsStream(resourcePath)
                        if (stream != null) {
                            stream.use { input ->
                                outFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Logger.d(TAG, "Extracted from classpath: $dllName (${outFile.length()} bytes)")
                            copied = true
                        }
                    }

                    if (!copied) {
                        Logger.e(TAG, "DLL not found: $dllName. Place it in libs/ directory")
                        return false
                    }
                }

                try {
                    System.load(outFile.absolutePath)
                    Logger.d(TAG, "Loaded: $dllName")
                } catch (e: UnsatisfiedLinkError) {
                    Logger.e(TAG, "Cannot load $dllName: ${e.message}")
                    return false
                }

                loaded = true
                Logger.i(TAG, "mpv native library ready")
                return true
            } catch (e: Throwable) {
                Logger.e(TAG, "Failed to load mpv native library", e)
                return false
            }
        }
    }
}
