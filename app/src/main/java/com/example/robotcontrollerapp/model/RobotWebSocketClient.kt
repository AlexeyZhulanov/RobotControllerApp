package com.example.robotcontrollerapp.model

import com.example.robotcontrollerapp.domain.DetectedPin
import com.example.robotcontrollerapp.domain.Device
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.Executors
import kotlin.concurrent.thread

enum class WsState { CONNECTING, CONNECTED, CLOSED, ERROR }

class RobotWebSocketClient(
    private val uriStr: String,
    private val maxBackoffMs: Long = 30000L
) {
    private var client: WebSocketClient? = null
    private val exec = Executors.newSingleThreadExecutor()

    // Callbacks
    var onMessageReceived: ((String) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null
    var onStateChanged: ((WsState) -> Unit)? = null

    // Structured callbacks
    var onSensorUpdate: ((String, Float) -> Unit)? = null
    var onDeviceStateChanged: ((String, Boolean) -> Unit)? = null
    var onBoardInfo: ((String, String) -> Unit)? = null // boardName, chipId
    var onDevicesList: ((List<Device>) -> Unit)? = null
    var onPinChanged: ((Int, Int) -> Unit)? = null
    var onDetectedPins: ((List<DetectedPin>) -> Unit)? = null
    var onStatus: ((Long, Int) -> Unit)? = null
    var onSpeedChanged: ((String, Int) -> Unit)? = null
    var onDeviceAdded: ((String, Int, String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onRawMessage: ((String) -> Unit)? = null


    @Volatile
    private var shouldReconnect = true

    @Volatile
    private var state: WsState = WsState.CLOSED
        set(value) {
            field = value
            onStateChanged?.invoke(value)
        }

    fun connect() {
        shouldReconnect = true
        exec.execute { internalConnectWithBackoff() }
    }

    private fun internalConnectWithBackoff() {
        var attempt = 0
        while (shouldReconnect) {
            try {
                state = WsState.CONNECTING
                log("Connecting to $uriStr ... (attempt ${attempt + 1})")

                client = object : WebSocketClient(URI(uriStr)) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        log("WebSocket opened")
                        state = WsState.CONNECTED
                        attempt = 0
                    }

                    override fun onMessage(message: String?) {
                        if (message == null) return
                        onMessageReceived?.invoke(message)
                        try {
                            handleJsonMessage(message)
                        } catch (e: Exception) {
                            log("Parse error: ${e.message}")
                        }
                    }

                    override fun onClose(code: Int, reason: String?, remote: Boolean) {
                        log("WebSocket closed: $reason")
                        state = WsState.CLOSED
                    }

                    override fun onError(ex: Exception?) {
                        log("WebSocket error: ${ex?.message}")
                        state = WsState.ERROR
                    }
                }

                client?.connectBlocking()
            } catch (e: Exception) {
                log("Connect exception: ${e.message}")
                state = WsState.ERROR
            }

            if (shouldReconnect && state != WsState.CONNECTED) {
                attempt++
                val backoff = calculateBackoff(attempt)
                log("Reconnect in ${backoff}ms")

                try {
                    Thread.sleep(backoff)
                } catch (_: InterruptedException) {
                    break
                }
            } else {
                if (!shouldReconnect) break
                while (shouldReconnect && state == WsState.CONNECTED) {
                    try {
                        Thread.sleep(500)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }
        log("Reconnect loop stopped")
    }

    private fun calculateBackoff(attempt: Int): Long {
        val base = 500L
        var backoff = base * (1L shl (attempt.coerceAtMost(6)))
        if (backoff > maxBackoffMs) backoff = maxBackoffMs
        val jitter = (0..500).random()
        return backoff + jitter
    }

    fun close() {
        shouldReconnect = false
        try {
            client?.close()
        } catch (e: Exception) {
            log("Close error: ${e.message}")
        }
        exec.shutdownNow()
    }

    fun forceReconnect() {
        try { client?.close() } catch (_: Exception) {}
    }

    private fun log(msg: String) = onLog?.invoke(msg)

    // ==========  Command API  ==========

    /** Включить/выключить устройство (например, LED) */
    fun setDeviceState(name: String, state: Boolean) {
        val action = if (state) "on" else "off"
        send(buildJson("action", "device" to name, "action" to action))
    }

    /** Установить скорость мотора (0–255) */
    fun setMotorSpeed(name: String, speed: Int) {
        send(buildJson("action",
            "device" to name,
            "action" to "set_speed",
            "value" to speed.coerceIn(-255, 255)
        ))
    }

    /** Подписаться на обновления конкретного сенсора */
    fun subscribeSensor(name: String) =
        send(buildJson("subscribe", "device" to name))

    /** Отписаться от сенсора */
    fun unsubscribeSensor(name: String) =
        send(buildJson("unsubscribe", "device" to name))

    /** Запросить разовый снимок всех значений */
    fun requestDevices() = send(buildJson("get_devices"))

    /** Запросить общий статус платы (аптайм, количество устройств и т.д.) */
    fun requestBoardStatus() = send(buildJson("get_status"))

    fun requestDetectedPins() = send(buildJson("get_detected_pins"))

    fun requestSensorsSnapshot() = send(buildJson("get_sensors"))

    private fun buildJson(cmd: String, vararg pairs: Pair<String, Any>): String {
        val obj = JSONObject()
        obj.put("cmd", cmd)
        pairs.forEach { (k, v) ->
            when (v) {
                is Number -> obj.put(k, v)
                is Boolean -> obj.put(k, v)
                is String -> obj.put(k, v)
                is JSONObject -> obj.put(k, v)
                is JSONArray -> obj.put(k, v)
                else -> obj.put(k, v.toString())
            }
        }
        return obj.toString()
    }

    fun send(text: String) {
        thread {
            try {
                if (client?.isOpen == true) {
                    client?.send(text)
                } else {
                    log("Send failed: socket not open")
                }
            } catch (e: Exception) {
                log("Send error: ${e.message}")
            }
        }
    }

    // ==========  MESSAGE PARSER  ==========
    private fun handleJsonMessage(msg: String) {
        try {
            val o = JSONObject(msg)
            // board_info
            if (o.optString("cmd") == "board_info") {
                val name = o.optString("board", "unknown")
                val chip = o.optString("chip_id", "")
                onBoardInfo?.invoke(name, chip)
                return
            }

            // devices array (get_devices response OR broadcast)
            if (o.has("devices")) {
                val arr = o.getJSONArray("devices")
                val list = mutableListOf<Device>()
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    val name = d.optString("name")
                    val pin = if (d.has("pin") && !d.isNull("pin")) d.optInt("pin", -1) else -1
                    val type = d.optString("type", "unknown")
                    // state may be boolean or int
                    val stateVal = d.opt("state")
                    val state = when (stateVal) {
                        is Boolean -> stateVal
                        is Number -> stateVal.toInt() != 0
                        else -> false
                    }
                    val pwm = if (d.has("pwmValue")) d.optInt("pwmValue", 0) else 0
                    // your Device class might differ; adapt constructor
                    list.add(Device(name = name, pin = pin, type = type, state = state, pwm = pwm))
                }
                onDevicesList?.invoke(list)
                return
            }

            // sensor_update
            if (o.optString("cmd") == "sensor_update" && o.has("device")) {
                val name = o.getString("device")
                val value = o.optDouble("value", Double.NaN).toFloat()
                if (!value.isNaN()) onSensorUpdate?.invoke(name, value)
                return
            }

            // sensors snapshot (get_sensors response)
            if (o.optString("cmd") == "sensors" && o.has("list")) {
                val arr = o.getJSONArray("list")
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    val name = s.optString("name")
                    val value = s.optDouble("value", Double.NaN).toFloat()
                    if (!value.isNaN()) onSensorUpdate?.invoke(name, value)
                }
                return
            }

            // device_state (single device update broadcast)
            if (o.has("device") && o.has("state") && (o.optString("cmd") == "device_state" || o.has("device") && !o.has("devices"))) {
                val name = o.getString("device")
                val stateVal = o.opt("state")
                val state = when (stateVal) {
                    is Boolean -> stateVal
                    is Number -> stateVal.toInt() != 0
                    else -> false
                }
                onDeviceStateChanged?.invoke(name, state)
                return
            }

            // pin_changed
            if (o.optString("cmd") == "pin_changed") {
                val pin = o.optInt("pin", -1)
                val state = o.optInt("state", -1)
                onPinChanged?.invoke(pin, state)
                return
            }

            // detected_pins
            if (o.optString("cmd") == "detected_pins" && o.has("pins")) {
                val arr = o.getJSONArray("pins")
                val pins = mutableListOf<DetectedPin>()
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    pins.add(DetectedPin(p.optInt("pin"), p.optInt("state"), p.optString("mode")))
                }
                onDetectedPins?.invoke(pins)
                return
            }

            // status
            if (o.optString("cmd") == "status") {
                val uptime = o.optLong("uptime_ms", 0L)
                val count = o.optInt("device_count", 0)
                onStatus?.invoke(uptime, count)
                return
            }

            // speed_changed
            if (o.optString("cmd") == "speed_changed") {
                val name = o.optString("device")
                val v = o.optInt("value", 0)
                onSpeedChanged?.invoke(name, v)
                return
            }

            // device_added
            if (o.optString("cmd") == "device_added") {
                val name = o.optString("name")
                val pin = o.optInt("pin", -1)
                val type = o.optString("type")
                onDeviceAdded?.invoke(name, pin, type)
                return
            }

            // config saved / log / error
            if (o.has("log")) {
                onLog?.invoke(o.optString("log"))
                return
            }
            if (o.has("error")) {
                onError?.invoke(o.optString("error"))
                return
            }

            // fallback
            onRawMessage?.invoke(msg)
        } catch (e: Exception) {
            onError?.invoke("parse_error: ${e.message}")
        }
    }
}