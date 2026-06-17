package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.desktop.player.VlcjMediaPlayer
import kotlin.test.*

class VlcjMediaPlayerTest {

    private lateinit var player: VlcjMediaPlayer

    @BeforeTest
    fun setup() {
        player = VlcjMediaPlayer()
    }

    @AfterTest
    fun teardown() {
        try {
            player.release()
        } catch (_: Exception) {}
    }

    @Test
    fun testInitialState() {
        assertFalse(player.isPlaying)
        assertEquals(0L, player.currentPosition)
        assertEquals(0L, player.duration)
        assertEquals(100, player.volume)
        assertEquals(1.0f, player.playbackSpeed)
    }

    @Test
    fun testSetVolume() {
        player.setVolume(50)
        assertEquals(50, player.volume)

        player.setVolume(0)
        assertEquals(0, player.volume)

        player.setVolume(100)
        assertEquals(100, player.volume)
    }

    @Test
    fun testSetVolumeClampsValues() {
        player.setVolume(-10)
        assertEquals(0, player.volume)

        player.setVolume(150)
        assertEquals(100, player.volume)
    }

    @Test
    fun testSetPlaybackSpeed() {
        player.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, player.playbackSpeed)

        player.setPlaybackSpeed(2.0f)
        assertEquals(2.0f, player.playbackSpeed)
    }

    @Test
    fun testSetPlaybackSpeedClampsValues() {
        player.setPlaybackSpeed(0.1f)
        assertEquals(0.25f, player.playbackSpeed)

        player.setPlaybackSpeed(5.0f)
        assertEquals(4.0f, player.playbackSpeed)
    }

    @Test
    fun testCallbackOnPlayStateChanged() {
        var callbackCalled = false
        var callbackState = false
        player.onPlayStateChanged = { state ->
            callbackCalled = true
            callbackState = state
        }

        // The callback is set but won't be triggered without actual media
        assertNotNull(player.onPlayStateChanged)
    }

    @Test
    fun testCallbackOnPositionChanged() {
        var callbackCalled = false
        player.onPositionChanged = { pos, dur ->
            callbackCalled = true
        }

        assertNotNull(player.onPositionChanged)
    }
}
