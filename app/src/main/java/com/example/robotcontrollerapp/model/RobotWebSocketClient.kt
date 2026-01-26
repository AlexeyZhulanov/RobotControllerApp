package com.example.robotcontrollerapp.model

import android.util.Log
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.util.gpioToD
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.min

enum class WsState { CONNECTING, CONNECTED, CLOSED, ERROR }

class RobotWebSocketClient(
    private val maxBackoffMs: Long = 30000L,
    private val motorSendRateHz: Int = 30,           // 30 пакетов/сек
    private val motorDeadzone: Int = 4,              // дрожание джойстика игнорим +/-4
    private val motorMinDeltaToSend: Int = 4,        // не шлём если изменение меньше 4
    private val heartbeatIntervalMs: Long = 3_000L,  // каждые 3 сек делаем ping
    private val heartbeatTimeoutMs: Long = 7_000L    // если 7 сек нет входящих — реконнект
) {
    @Volatile private var targetUrl: String = ""
    private val clientRef = AtomicReference<WebSocketClient?>(null)

    // только для connect/reconnect логики
    private val connectExec = Executors.newSingleThreadExecutor()
    // только для send (чтобы отправка не блокировалась реконнектом)
    private val sendExec = Executors.newSingleThreadExecutor()

    // Таймер реконнекта
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    @Volatile private var reconnectJob: ScheduledFuture<*>? = null

    // Heartbeat job
    @Volatile private var heartbeatJob: ScheduledFuture<*>? = null
    private val lastRxMillis = AtomicLong(0L)

    // Антиспам для одного мотора
    private data class MotorState(val name: String, val speed: Int)

    private val pendingMotorSingle = AtomicReference<MotorState?>(null)
    private val lastMotorSingleSent = AtomicReference<MotorState?>(null)

    @Volatile private var motorSingleSenderJob: ScheduledFuture<*>? = null

    // Антиспам для двух моторов
    private data class TankState(
        val leftName: String,
        val leftSpeed: Int,
        val rightName: String,
        val rightSpeed: Int
    )

    private val pendingTank = AtomicReference<TankState?>(null)
    private val lastTankSent = AtomicReference<TankState?>(null)

    @Volatile private var motorSenderJob: ScheduledFuture<*>? = null

    // Callbacks
    var onMessageReceived: ((String) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null
    var onStateChanged: ((WsState) -> Unit)? = null

    // Structured callbacks
    var onSensorUpdate: ((String, Float) -> Unit)? = null
    var onDeviceStateChanged: ((String, Boolean) -> Unit)? = null
    var onBoardInfo: ((String, String) -> Unit)? = null // boardName, chipId
    var onDevicesList: ((List<Device>) -> Unit)? = null
    var onStatus: ((Long, Int) -> Unit)? = null
    var onSpeedChanged: ((String, Int) -> Unit)? = null
    var onDeviceAdded: ((String, Int, String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onRawMessage: ((String) -> Unit)? = null


    @Volatile private var shouldReconnect = true

    @Volatile
    private var state: WsState = WsState.CLOSED
        set(value) {
            field = value
            onStateChanged?.invoke(value)
        }

    // Счётчик попыток реконнекта
    @Volatile private var attempt = 0

    @Synchronized
    fun connect(url: String) {
        if (state == WsState.CONNECTED || state == WsState.CONNECTING) {
            log("connect() ignored: already $state")
            return
        }
        targetUrl = url
        shouldReconnect = true
        attempt = 0

        connectExec.execute {
            cancelReconnectJob()
            doConnect()
        }
    }

    private fun doConnect() {
        if (!shouldReconnect) return

        state = WsState.CONNECTING
        log("Connecting to $targetUrl ... (attempt ${attempt + 1})")

        val wsClient = object : WebSocketClient(URI(targetUrl)) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                log("WebSocket opened")
                state = WsState.CONNECTED
                attempt = 0

                lastRxMillis.set(System.currentTimeMillis())

                startHeartbeat()
                startMotorSender()
                startSingleMotorSender()
            }

            override fun onMessage(message: String?) {
                if (message == null) return

                lastRxMillis.set(System.currentTimeMillis())

                onMessageReceived?.invoke(message)
                try {
                    handleJsonMessage(message)
                } catch (e: Exception) {
                    log("Parse error: ${e.message}")
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                log("WebSocket closed: code=$code reason=$reason remote=$remote")
                state = WsState.CLOSED

                stopHeartbeat()
                stopMotorSender()
                stopSingleMotorSender()
                scheduleReconnectIfNeeded()
            }

            override fun onError(ex: Exception?) {
                log("WebSocket error: ${ex?.message}")
                state = WsState.ERROR

                stopHeartbeat()
                stopMotorSender()
                stopSingleMotorSender()
                scheduleReconnectIfNeeded()
            }
        }

        clientRef.set(wsClient)

        try {
            wsClient.connect()
        } catch (e: Exception) {
            log("Connect exception: ${e.message}")
            state = WsState.ERROR
            scheduleReconnectIfNeeded()
        }
    }

    private fun scheduleReconnectIfNeeded() {
        if (!shouldReconnect) return
        if (state == WsState.CONNECTED || state == WsState.CONNECTING) return

        attempt++
        val backoff = calculateBackoff(attempt)
        log("Reconnect scheduled in ${backoff}ms")

        cancelReconnectJob()
        reconnectJob = scheduler.schedule({
            connectExec.execute {
                // на момент таймера могло уже подключиться — ещё раз проверим
                if (!shouldReconnect) return@execute
                if (state == WsState.CONNECTED || state == WsState.CONNECTING) return@execute
                doConnect()
            }
        }, backoff, TimeUnit.MILLISECONDS)
    }

    private fun cancelReconnectJob() {
        reconnectJob?.cancel(true)
        reconnectJob = null
    }

    private fun calculateBackoff(attempt: Int): Long {
        val base = 500L
        val exp = (1L shl min(attempt, 6))
        val backoff = min(base * exp, maxBackoffMs)
        val jitter = (0..500).random()
        return backoff + jitter
    }

    fun close() {
        shouldReconnect = false
        cancelReconnectJob()

        stopHeartbeat()
        stopMotorSender()

        state = WsState.CLOSED
        try {
            clientRef.getAndSet(null)?.close()
        } catch (e: Exception) {
            log("Close error: ${e.message}")
        }
    }

    private fun forceReconnectFromHeartbeat() {
        if (!shouldReconnect) return

        // важно: меняем состояние сразу, чтобы UI и логика перестали считать соединение живым
        state = WsState.ERROR

        try { clientRef.get()?.close() } catch (_: Exception) {}

        // Если onClose/onError не придёт — всё равно планируем реконнект сами
        scheduleReconnectIfNeeded()
    }

    private fun log(msg: String) {
        Log.d("testWSClient", msg)
        onLog?.invoke(msg)
    }

    // ================= Heartbeat =================
    private fun startHeartbeat() {
        stopHeartbeat()
        if (heartbeatIntervalMs <= 0) return

        heartbeatJob = scheduler.scheduleWithFixedDelay({
            if (state != WsState.CONNECTED) return@scheduleWithFixedDelay

            val now = System.currentTimeMillis()
            val silent = now - lastRxMillis.get()

            if (silent > heartbeatTimeoutMs) {
                log("Heartbeat timeout: no RX for ${silent}ms -> forceReconnect()")
                forceReconnectFromHeartbeat()
                return@scheduleWithFixedDelay
            }

            send(buildJson("get_status"))
        }, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS)

        log("Heartbeat started: interval=$heartbeatIntervalMs timeout=$heartbeatTimeoutMs")
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel(true)
        heartbeatJob = null
    }

    // ================= Motor spam control =================
    private fun startMotorSender() {
        stopMotorSender()
        if (motorSendRateHz <= 0) return

        val delayMs = (1000L / motorSendRateHz).coerceAtLeast(10L)

        motorSenderJob = scheduler.scheduleWithFixedDelay({
            if (state != WsState.CONNECTED) return@scheduleWithFixedDelay

            val m = pendingTank.get() ?: return@scheduleWithFixedDelay
            val last = lastTankSent.get()

            fun deltaOk(a: Int, b: Int): Boolean = abs(a - b) >= motorMinDeltaToSend

            // Если обе скорости практически не изменились — не шлём
            if (last != null &&
                last.leftName == m.leftName &&
                last.rightName == m.rightName &&
                !deltaOk(m.leftSpeed, last.leftSpeed) &&
                !deltaOk(m.rightSpeed, last.rightSpeed)
            ) {
                return@scheduleWithFixedDelay
            }

            // Отправляем ЛЕВЫЙ
            send(buildJson(
                "action",
                "device" to m.leftName,
                "action" to "set_speed",
                "value" to m.leftSpeed
            ))

            // Отправляем ПРАВЫЙ
            send(buildJson(
                "action",
                "device" to m.rightName,
                "action" to "set_speed",
                "value" to m.rightSpeed
            ))

            lastTankSent.set(m)

        }, 0L, delayMs, TimeUnit.MILLISECONDS)

        log("Tank motor sender started: rate=${motorSendRateHz}Hz delay=${delayMs}ms")
    }

    private fun stopMotorSender() {
        motorSenderJob?.cancel(true)
        motorSenderJob = null
        pendingTank.set(null)
        lastTankSent.set(null)
    }

    fun setTankSpeedsThrottled(
        leftName: String,
        leftSpeed: Int,
        rightName: String,
        rightSpeed: Int
    ) {
        fun filterSpeed(s: Int): Int {
            val clamped = s.coerceIn(-255, 255)
            return if (abs(clamped) <= motorDeadzone) 0 else clamped
        }

        val ls = filterSpeed(leftSpeed)
        val rs = filterSpeed(rightSpeed)

        pendingTank.set(
            TankState(
                leftName = leftName,
                leftSpeed = ls,
                rightName = rightName,
                rightSpeed = rs
            )
        )
    }

    fun setMotorSpeedThrottled(name: String, speed: Int) {
        val s = speed.coerceIn(-255, 255)
        val filtered = if (abs(s) <= motorDeadzone) 0 else s
        pendingMotorSingle.set(MotorState(name, filtered))
    }

    private fun startSingleMotorSender() {
        stopSingleMotorSender()
        if (motorSendRateHz <= 0) return

        val delayMs = (1000L / motorSendRateHz).coerceAtLeast(10L)

        motorSingleSenderJob = scheduler.scheduleWithFixedDelay({
            if (state != WsState.CONNECTED) return@scheduleWithFixedDelay

            val m = pendingMotorSingle.get() ?: return@scheduleWithFixedDelay
            val last = lastMotorSingleSent.get()

            if (last != null && last.name == m.name) {
                val delta = abs(m.speed - last.speed)
                if (delta < motorMinDeltaToSend) return@scheduleWithFixedDelay
            }

            send(buildJson(
                "action",
                "device" to m.name,
                "action" to "set_speed",
                "value" to m.speed
            ))

            lastMotorSingleSent.set(m)

        }, 0L, delayMs, TimeUnit.MILLISECONDS)

        log("Single motor sender started: rate=${motorSendRateHz}Hz delay=${delayMs}ms")
    }

    private fun stopSingleMotorSender() {
        motorSingleSenderJob?.cancel(true)
        motorSingleSenderJob = null
        pendingMotorSingle.set(null)
        lastMotorSingleSent.set(null)
    }

    // ================= Generic send =================
    fun send(text: String) {
        Log.d("testSend", text)
        sendExec.execute {
            try {
                val c = clientRef.get()
                if (c?.isOpen == true) {
                    c.send(text)
                } else {
                    log("Send failed: socket not open (state=$state)")
                }
            } catch (e: Exception) {
                log("Send error: ${e.message}")
            }
        }
    }

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
                    list.add(Device(name = name, pin = gpioToD(pin), type = type, state = state, pwm = pwm))
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

            // uno_sensors (пакетное обновление с Arduino Uno)
            if (o.optString("cmd") == "uno_sensors" && o.has("data")) {
                val dataString = o.optString("data") // "512,1023,44,..."
                if (dataString.isNotEmpty()) {
                    val sensorValues = dataString.split(",")
                    // Проходим по каждому значению в строке
                    sensorValues.forEachIndexed { index, valueString ->
                        // Генерируем имя, соответствующее пину (A0, A1, ...)
                        val sensorName = "uno_sensor_A${index}"
                        val value = valueString.toFloatOrNull()

                        // Отправляем обновление, как будто это обычный 'sensor_update'
                        if (value != null) {
                            onSensorUpdate?.invoke(sensorName, value)
                        }
                    }
                }
                return
            }

            // device_state (single device update broadcast)
            if (o.optString("cmd") == "device_state" && o.has("state")) {
                val name = o.optString("device", o.optString("name"))
                val stateVal = o.opt("state")
                val state = when (stateVal) {
                    is Boolean -> stateVal
                    is Number -> stateVal.toInt() != 0
                    else -> false
                }
                if (name.isNotEmpty()) onDeviceStateChanged?.invoke(name, state)
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