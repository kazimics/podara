package app.podara

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.podara.util.Strings
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.decodeToImageBitmap

private val appIcon = lazy {
    val bytes = object {}::class.java.classLoader.getResourceAsStream("logo-256.png")!!.readBytes()
    BitmapPainter(bytes.decodeToImageBitmap())
}

fun main() {
    val logFile = File(System.getProperty("user.home"), ".podara/crash.log")

    try {
        application {
            val windowState = rememberWindowState(
                size = DpSize(1200.dp, 800.dp),
                position = WindowPosition(Alignment.Center)
            )

            Window(
                onCloseRequest = ::exitApplication,
                state = windowState,
                title = Strings["app_name"],
                undecorated = true,
                transparent = false,
                icon = appIcon.value
            ) {
                App(windowState, window)
            }
        }
    } catch (e: Exception) {
        logFile.parentFile?.mkdirs()
        logFile.appendText(
            "[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}] FATAL: ${e.message}\n${e.stackTraceToString()}\n\n"
        )
        throw e
    }
}
