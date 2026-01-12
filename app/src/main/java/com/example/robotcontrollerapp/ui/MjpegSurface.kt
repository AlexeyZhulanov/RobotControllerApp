package com.example.robotcontrollerapp.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.robotcontrollerapp.model.MjpegInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun MjpegSurface(
    url: String,
    modifier: Modifier = Modifier
) {
    // Создаем Scope, привязанный к жизни этого Composable
    val scope = rememberCoroutineScope()

    // Создаем рендерер один раз. Он будет помнить состояние Surface и текущую задачу
    val renderer = remember { MjpegRenderer(scope) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceView(context).apply {
                // Подключаем наш рендерер к SurfaceView
                holder.addCallback(renderer)
            }
        },
        update = {
            // Мы сообщаем рендереру новый URL, и он сам решит, нужно ли перезапускать поток
            renderer.updateUrl(url)
        }
    )

    // При удалении Composable очищаем ресурсы
    DisposableEffect(Unit) {
        onDispose {
            renderer.close()
        }
    }
}

private class MjpegRenderer(private val scope: CoroutineScope) : SurfaceHolder.Callback {
    private var currentUrl: String = ""
    private var holder: SurfaceHolder? = null
    private var streamJob: Job? = null

    fun updateUrl(newUrl: String) {
        if (currentUrl != newUrl) {
            Log.d("testMjpegSurface", "URL changed: $newUrl")
            currentUrl = newUrl
            restartStream()
        }
    }

    fun close() {
        stopStream()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("testMjpegSurface", "Surface created")
        this.holder = holder
        restartStream()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // Можно обработать изменение размера, если нужно
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("testMjpegSurface", "Surface destroyed")
        this.holder = null
        stopStream()
    }

    private fun restartStream() {
        stopStream() // Сначала убиваем старый
        val h = holder
        if (h != null && currentUrl.isNotEmpty()) {
            streamJob = scope.launch(Dispatchers.IO) {
                streamMjpeg(currentUrl, h)
            }
        }
    }

    private fun stopStream() {
        streamJob?.cancel()
        streamJob = null
    }
}

// Функция потокового чтения и рисования
private suspend fun streamMjpeg(streamUrl: String, holder: SurfaceHolder) {
    var mjpegIn: MjpegInputStream? = null

    // currentCoroutineContext().isActive — это проверка: "нас еще не отменили?"
    while (currentCoroutineContext().isActive) {
        try {
            Log.d("testCamera", "Connecting to stream: $streamUrl")
            val url = URL(streamUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 5000
            conn.connectTimeout = 5000
            conn.doInput = true
            conn.connect()

            mjpegIn = MjpegInputStream(conn.inputStream)

            // Внутренний цикл чтения кадров
            while (currentCoroutineContext().isActive) {
                val bitmap = mjpegIn.readMjpegFrame()
                if (bitmap != null) {
                    // Рисуем
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
                    Log.w("testCamera", "Stream ended (null frame), reconnecting...")
                    break // Выход во внешний цикл для реконнекта
                }
            }
        } catch (e: CancellationException) {
            // Если корутину отменили (скрыли камеру), мы попадаем СЮДА.
            // Важно просто выйти и НЕ пытаться переподключиться.
            Log.d("testCamera", "Stream cancelled (User hidden camera)")
            throw e // Пробрасываем отмену дальше, чтобы это попало в finally и корутина корректно завершилась
        } catch (e: Exception) {
            Log.e("testCamera", "Stream error: ${e.message}. Retrying in 1 sec...")
            try {
                // Если ошибка сети, ждем 1 сек, но проверяем, активны ли мы еще
                if (currentCoroutineContext().isActive) {
                    delay(1000)
                }
            } catch (_: Exception) {}
        } finally {
            try { mjpegIn?.close() } catch (_: Exception) {}
        }
    }
}