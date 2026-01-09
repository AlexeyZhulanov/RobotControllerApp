package com.example.robotcontrollerapp.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

class MjpegInputStream(inputStream: InputStream) : DataInputStream(BufferedInputStream(inputStream, 8192)) {

    fun readMjpegFrame(): Bitmap? {
        var contentLength = -1

        try {
            // 1. Читаем заголовки ПОСТРОЧНО, пока не найдем длину и конец заголовков
            while (true) {
                // Читаем строку (ASCII)
                val line = readLineFromStream() ?: return null // Если конец потока - выходим

                // Ищем Content-Length (регистронезависимо)
                if (line.startsWith("Content-Length", ignoreCase = true)) {
                    val parts = line.split(":")
                    if (parts.size == 2) {
                        contentLength = parts[1].trim().toIntOrNull() ?: -1
                    }
                }

                // Пустая строка означает КОНЕЦ заголовков и НАЧАЛО картинки
                // Но мы выходим из цикла только если уже нашли Content-Length
                if (line.isEmpty() && contentLength > 0) {
                    break
                }
            }

            // 2. Если длину нашли - читаем ровно столько байт
            val imageData = ByteArray(contentLength)
            readFully(imageData) // Гарантированно читает N байт или кидает ошибку

            // 3. Превращаем в картинку
            return BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

        } catch (e: IOException) {
            Log.e("testMjpegStream", "IO Error: ${e.message}")
            return null // Это заставит внешний цикл перезапустить соединение
        } catch (e: Exception) {
            Log.e("testMjpegStream", "Parse Error: ${e.message}")
            return null
        }
    }

    // Вспомогательная функция для чтения одной строки (до \n)
    private fun readLineFromStream(): String? {
        val sb = StringBuilder()
        var c: Int
        while (true) {
            c = read()
            if (c == -1) return null // Конец потока
            if (c == '\n'.code) break // Конец строки
            if (c != '\r'.code) sb.append(c.toChar()) // Собираем строку, игнорируя \r
        }
        return sb.toString()
    }
}
