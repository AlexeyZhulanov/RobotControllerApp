package com.example.robotcontrollerapp.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.robotcontrollerapp.model.MjpegInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun MjpegSurface(
    url: String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        isRunning = true
                        scope.launch(Dispatchers.IO) {
                            streamMjpeg(url, holder) { isRunning }
                        }
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        isRunning = false
                    }
                })
            }
        },
        update = {
            // Если URL меняется, можно добавить логику перезапуска, TODO
            // но для простоты пока оставим как есть.
        }
    )
}

// Функция потокового чтения и рисования
private suspend fun streamMjpeg(streamUrl: String, holder: SurfaceHolder, isRunning: () -> Boolean) {
    var mjpegIn: MjpegInputStream? = null

    // Внешний цикл: отвечает за переподключение при обрыве
    while (isRunning()) {
        try {
            Log.d("testCamera", "Connecting to stream: $streamUrl")
            val url = URL(streamUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 5000
            conn.connectTimeout = 5000
            conn.doInput = true
            conn.connect()

            mjpegIn = MjpegInputStream(conn.inputStream)

            // Внутренний цикл: чтение кадров пока есть соединение
            while (isRunning()) {
                val bitmap = mjpegIn.readMjpegFrame()
                if (bitmap != null) {
                    val canvas = holder.lockCanvas()
                    if (canvas != null) {
                        try {
                            val destRect = Rect(0, 0, canvas.width, canvas.height)
                            canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
                            canvas.drawBitmap(bitmap, null, destRect, null)
                        } finally {
                            holder.unlockCanvasAndPost(canvas)
                        }
                    }
                    bitmap.recycle()
                } else {
                    // Поток вернул null (соединение закрыто сервером)
                    Log.w("testCamera", "Stream ended, reconnecting...")
                    break // Выходим из внутреннего цикла, внешний цикл запустит подключение заново
                }
            }
        } catch (e: Exception) {
            Log.e("testCamera", "Stream error: ${e.message}. Retrying in 1 sec...")
            // Ошибка сети. Ждем немного перед повторной попыткой, чтобы не спамить
            try {
                withContext(Dispatchers.IO) { Thread.sleep(1000) }
            } catch (_: Exception) {}
        } finally {
            try { mjpegIn?.close() } catch (_: Exception) {}
        }
    }
}