package app.podiumpodcasts.podium.util

import app.podiumpodcasts.podium.player.MediaPlayerState
import java.awt.Color
import java.awt.Font
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JDialog
import javax.swing.JMenuItem
import javax.swing.JSeparator
import javax.swing.SwingUtilities

/**
 * System tray icon with a fully styled context menu.
 *
 * The menu is an undecorated JDialog positioned at the click's screen
 * coordinates.  JDialog is a heavier-weight top-level window than JWindow
 * and respects virtual-screen coordinates on multi-monitor Windows setups
 * more reliably.
 *
 * An empty AWT PopupMenu is set on the TrayIcon so the native peer doesn't
 * swallow right-click events.
 */
class SystemTrayManager(
    private val window: Window,
    private val playerState: MediaPlayerState
) {
    private var trayIcon: TrayIcon? = null
    private var trayAdded = false

    private var menuDialog: JDialog? = null
    private var menuShowHideItem: JMenuItem? = null
    private var menuPlayPauseItem: JMenuItem? = null
    private var globalClickListener: AWTEventListener? = null

    companion object {
        private val MENU_BG      = Color(46, 46, 46)      // #2E2E2E
        private val MENU_FG      = Color(238, 238, 238)   // #EEEEEE
        private val MENU_HOVER   = Color(61, 61, 61)      // #3D3D3D
        private val MENU_BORDER  = Color(85, 85, 85)      // #555555
        private val SEP_BG       = Color(68, 68, 68)      // #444444
    }

    fun setup() {
        if (!SystemTray.isSupported()) {
            Logger.d("SystemTray", "System tray not supported")
            return
        }
        if (trayAdded) return

        val tray = SystemTray.getSystemTray()
        val image = loadTrayIcon()

        trayIcon = TrayIcon(image, "Podium").apply {
            isImageAutoSize = true
            // An empty PopupMenu satisfies the native peer check so
            // right-click events forward to Java, but shows nothing
            // visible (no white block from dummy items).
            popupMenu = PopupMenu()

            addActionListener { showWindow() }
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent)  { maybeShowPopup(e) }
                override fun mouseReleased(e: MouseEvent) { maybeShowPopup(e) }
            })
        }

        try {
            tray.add(trayIcon)
            trayAdded = true
            Logger.d("SystemTray", "Tray icon added")
        } catch (e: Exception) {
            Logger.e("SystemTray", "Failed to add tray icon", e)
        }
    }

    fun remove() {
        hideMenu()
        if (!trayAdded || !SystemTray.isSupported()) return
        try { SystemTray.getSystemTray().remove(trayIcon) } catch (_: Exception) { }
        trayAdded = false
        trayIcon = null
    }

    fun updatePlayPauseLabel(isPlaying: Boolean) {
        menuPlayPauseItem?.text = if (isPlaying) Strings["tray_pause"] else Strings["tray_play"]
    }

    fun updateShowHideLabel(isVisible: Boolean) {
        menuShowHideItem?.text = if (isVisible) Strings["tray_hide"] else Strings["tray_show"]
    }

    // ── Menu ──

    private fun maybeShowPopup(e: MouseEvent) {
        if (e.isPopupTrigger || e.button == MouseEvent.BUTTON3) {
            // getPointerInfo().location queries GetCursorPos directly —
            // it's typically more reliable on multi-monitor than the
            // cached event coordinates.
            val cursorPos = java.awt.MouseInfo.getPointerInfo()?.location
            val x = cursorPos?.x ?: e.xOnScreen
            val y = cursorPos?.y ?: e.yOnScreen
            Logger.d("SystemTray", "showMenu trigger: event=(${e.xOnScreen},${e.yOnScreen})  pointer=($x,$y)")
            showMenu(x, y)
        }
    }

    private fun showMenu(x: Int, y: Int) {
        hideMenu()

        val font = resolveFont()
        val panel = javax.swing.JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = MENU_BG
            border = BorderFactory.createLineBorder(MENU_BORDER)
        }

        menuShowHideItem = mkItem(Strings["tray_hide"], font) { toggleWindowVisibility(); hideMenu() }
        panel.add(menuShowHideItem)
        panel.add(menuSeparator())

        menuPlayPauseItem = mkItem(Strings["tray_play"], font) { playerState.togglePlayPause(); hideMenu() }
        panel.add(menuPlayPauseItem)
        panel.add(mkItem(Strings["tray_previous"], font) { playerState.playPrevious(); hideMenu() })
        panel.add(mkItem(Strings["tray_next"], font) { playerState.playNext(); hideMenu() })
        panel.add(menuSeparator())
        panel.add(mkItem(Strings["tray_quit"], font) { hideMenu(); quitApp() })

        menuDialog = JDialog().apply {
            isUndecorated = true
            isAlwaysOnTop = true
            contentPane = panel
            pack()
            setLocation(x, y)

            println("[SystemTray] showMenu at ($x, $y), size=${size}")

            // Dismiss when the window loses focus (click outside)
            addWindowFocusListener(object : java.awt.event.WindowAdapter() {
                override fun windowLostFocus(e: java.awt.event.WindowEvent) {
                    println("[SystemTray] windowLostFocus: opposite=${e.oppositeWindow}")
                    hideMenu()
                }
            })

            isVisible = true
            toFront()
            requestFocus()
        }

        // Global click listener — backup in case focus events don't fire
        globalClickListener = AWTEventListener { event ->
            if (event is MouseEvent && event.id == MouseEvent.MOUSE_PRESSED) {
                val src = SwingUtilities.getWindowAncestor(event.source as? java.awt.Component)
                println("[SystemTray] AWTEventListener: srcWindow=$src, menuDialog=$menuDialog")
                if (menuDialog != null && src != menuDialog) {
                    hideMenu()
                }
            }
        }
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
            globalClickListener, java.awt.AWTEvent.MOUSE_EVENT_MASK
        )
    }

    private fun hideMenu() {
        if (globalClickListener != null) {
            java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(globalClickListener)
            globalClickListener = null
        }
        menuDialog?.dispose()
        menuDialog = null
    }

    private fun mkItem(text: String, font: Font, onClick: () -> Unit): JMenuItem =
        JMenuItem(text).apply {
            this.font = font
            this.foreground = MENU_FG
            this.background = MENU_BG
            this.isOpaque = true
            addActionListener { onClick() }

            val self = this
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) { self.background = MENU_HOVER }
                override fun mouseExited(e: MouseEvent)  { self.background = MENU_BG }
            })
        }

    private fun menuSeparator(): JSeparator =
        JSeparator().apply {
            foreground = SEP_BG
            background = SEP_BG
        }

    private fun resolveFont(): Font {
        val candidates = listOf("Microsoft YaHei", "Segoe UI", "Dialog")
        for (name in candidates) try { return Font(name, Font.PLAIN, 13) } catch (_: Exception) { }
        return Font("Dialog", Font.PLAIN, 13)
    }

    // ── Window ──

    private fun toggleWindowVisibility() {
        if (window.isShowing) {
            window.isVisible = false
            menuShowHideItem?.text = Strings["tray_show"]
        } else { showWindow() }
    }

    private fun showWindow() {
        window.isVisible = true; window.toFront(); window.requestFocus()
        menuShowHideItem?.text = Strings["tray_hide"]
    }

    private fun quitApp() {
        remove(); playerState.release(); window.dispose(); System.exit(0)
    }

    private fun loadTrayIcon(): java.awt.Image {
        try {
            val bytes = object {}::class.java.classLoader.getResourceAsStream("logo-256.png")!!.readBytes()
            return ImageIO.read(ByteArrayInputStream(bytes))
        } catch (e: Exception) {
            Logger.e("SystemTray", "Failed to load tray icon", e)
            val fb = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val g = fb.createGraphics(); g.color = Color(0xE0B183); g.fillRect(0, 0, 16, 16); g.dispose()
            return fb
        }
    }
}
