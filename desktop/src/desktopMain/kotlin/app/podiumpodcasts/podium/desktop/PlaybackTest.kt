package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.utils.Logger
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile

private const val TAG = "PlaybackTest"

object PlaybackTest {

    fun run(): Boolean {
        Logger.i(TAG, "=== Starting Playback Test ===")

        // Step 1: Extract native libs
        Logger.i(TAG, "Step 1: Loading native libraries...")
        val nativeDir = File(System.getProperty("user.home"), ".podium/native")
        nativeDir.mkdirs()
        val nativeDlls = listOf("jfxmedia.dll", "gstreamer-lite.dll", "glib-lite.dll", "fxplugins.dll")

        val jarLocations = listOf(
            PlaybackTest::class.java.protectionDomain?.codeSource?.location?.toURI()?.path,
            "libs/javafx-media-21.0.2-win.jar",
            "../libs/javafx-media-21.0.2-win.jar"
        )

        var extracted = false
        for (jarPath in jarLocations) {
            if (jarPath != null && jarPath.endsWith(".jar") && File(jarPath).exists()) {
                Logger.d(TAG, "Found JAR: $jarPath")
                try {
                    JarFile(jarPath).use { jar ->
                        for (dllName in nativeDlls) {
                            val entry = jar.getEntry(dllName) ?: continue
                            val outFile = File(nativeDir, dllName)
                            jar.getInputStream(entry).use { input -> outFile.writeBytes(input.readBytes()) }
                            Logger.d(TAG, "Extracted: $dllName (${outFile.length()} bytes)")
                            extracted = true
                        }
                    }
                    if (extracted) break
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to extract from $jarPath: ${e.message}")
                }
            }
        }

        for (dllName in nativeDlls) {
            val dll = File(nativeDir, dllName)
            if (dll.exists()) {
                try {
                    Runtime.getRuntime().load(dll.absolutePath)
                    Logger.d(TAG, "Loaded: $dllName")
                } catch (e: UnsatisfiedLinkError) {
                    Logger.w(TAG, "Cannot load $dllName: ${e.message}")
                }
            }
        }

        // Step 2: Initialize JavaFX toolkit
        Logger.i(TAG, "Step 2: Initializing JavaFX toolkit...")
        try {
            val initLatch = CountDownLatch(1)
            javafx.application.Platform.startup { initLatch.countDown() }
            initLatch.await(5, TimeUnit.SECONDS)
            Logger.d(TAG, "Toolkit initialized")
        } catch (e: Exception) {
            Logger.w(TAG, "Platform.startup failed (may already be initialized): ${e.message}")
        }

        // Step 3: Test with local file
        val testFile = File("C:\\Users\\Kazimi\\.podium\\downloads\\c465635af671b189b766835d449dfa3b2d461bba29a69b1d059a6697afc8366d\\f214ddcc0743b0288d9c2684da9dc48f9a571f0e4373f96da651a43afd5b4ed4")
        if (!testFile.exists()) {
            Logger.e(TAG, "Test file not found: ${testFile.absolutePath}")
            return false
        }

        Logger.i(TAG, "Step 2: Testing Media creation (${testFile.length()} bytes)")
        val fileUri = testFile.toURI().toString()
        Logger.d(TAG, "File URI: $fileUri")

        try {
            val media = Media(fileUri)
            Logger.i(TAG, "Media object created successfully")

            val latch = CountDownLatch(1)
            var success = false
            var errorMsg = ""

            val mp = MediaPlayer(media)
            mp.setOnReady {
                Logger.i(TAG, "Media READY! Duration=${mp.totalDuration.toMillis()}ms")
                success = true
                latch.countDown()
            }
            mp.setOnError {
                errorMsg = mp.error?.message ?: "Unknown error"
                Logger.e(TAG, "Media ERROR: $errorMsg")
                latch.countDown()
            }

            val completed = latch.await(15, TimeUnit.SECONDS)
            if (!completed) {
                Logger.e(TAG, "Timeout waiting for media (15s)")
            }

            mp.dispose()
            Logger.i(TAG, "=== Test Result: ${if (success) "PASS" else "FAIL ($errorMsg)"} ===")
            return success
        } catch (e: Throwable) {
            Logger.e(TAG, "FAILED: ${e.javaClass.simpleName}: ${e.message}")
            Logger.i(TAG, "=== Test Result: FAIL ===")
            return false
        }
    }
}
