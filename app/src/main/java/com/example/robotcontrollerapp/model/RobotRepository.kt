package com.example.robotcontrollerapp.model

import android.util.Log
import com.example.robotcontrollerapp.domain.DetectedPin
import com.example.robotcontrollerapp.domain.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RobotRepository @Inject constructor() {

    // Единственный экземпляр клиента на всё приложение
    private val wsClient = RobotWebSocketClient()

    // --- StateFlow (Источники данных для UI) ---

    // Состояние подключения
    private val _wsState = MutableStateFlow(WsState.CLOSED)
    val wsState = _wsState.asStateFlow()

    // Список устройств
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    // Данные сенсоров (Карта: "имя_сенсора" -> значение)
    private val _sensorData = MutableStateFlow<Map<String, Float>>(emptyMap())
    val sensorData = _sensorData.asStateFlow()

    // Логи
    private val _logs = MutableStateFlow<String>("")
    val logs = _logs.asStateFlow()

    // Информация о плате
    private val _boardInfo = MutableStateFlow<Pair<String, String>>("" to "")
    val boardInfo = _boardInfo.asStateFlow()

    private val _boardName = MutableStateFlow<String>("")
    val boardName = _boardName.asStateFlow()

    // Найденные пины (для PinEditor)
    private val _detectedPins = MutableStateFlow<List<DetectedPin>>(emptyList())
    val detectedPins = _detectedPins.asStateFlow()


    init {
        setupClientCallbacks()
    }

    private fun setupClientCallbacks() {
        // Связываем события Клиента с нашими StateFlow
        wsClient.onStateChanged = { state ->
            _wsState.value = state
        }

        wsClient.onDevicesList = { list ->
            Log.d("testRepo", "Devices updated: ${list.size}")
            _devices.value = list
        }

        wsClient.onSensorUpdate = { name, value ->
            // Обновляем карту значений сенсоров
            val currentMap = _sensorData.value.toMutableMap()
            currentMap[name] = value
            _sensorData.value = currentMap
        }

        wsClient.onLog = { msg ->
            // Можно накапливать или просто слать последнее
            _logs.value = msg
        }

        wsClient.onBoardInfo = { board, chip ->
            _boardInfo.value = board to chip
            _boardName.value = board
        }

        wsClient.onDetectedPins = { pins ->
            _detectedPins.value = pins
        }

        wsClient.onDeviceAdded = { name, pin, type ->
            _devices.value = _devices.value + Device(name, pin, type = type)
        }
    }

    // --- ГЛАВНАЯ ЛОГИКА ПОДКЛЮЧЕНИЯ ---

    suspend fun searchAndConnect() {
        if (_wsState.value == WsState.CONNECTED) return

        _logs.value = "Searching for robot via UDP..."
        Log.d("testConnect", "Searching for robot via UDP...")
        val ip = findRobotIpUdp()

        if (ip != null) {
            _logs.value = "Robot found: $ip. Connecting..."
            Log.d("testConnect", "Robot found: $ip. Connecting...")
            val url = "ws://$ip:81"
            wsClient.connect(url)
        } else {
            Log.d("testConnect", "Robot not found via UDP. Check Wi-Fi.")
            _logs.value = "Robot not found via UDP. Check Wi-Fi."
        }
    }

    // --- UDP ПОИСК ---
    private suspend fun findRobotIpUdp(): String? = withContext(Dispatchers.IO) {
        val socket = DatagramSocket(4210)
        socket.soTimeout = 5000

        return@withContext try {
            val buf = ByteArray(256)
            val packet = DatagramPacket(buf, buf.size)

            socket.receive(packet)

            val msg = String(packet.data, 0, packet.length)
            Log.d("UDP", "Received: $msg")

            if (msg.startsWith("I_AM_ROBOT:")) {
                msg.removePrefix("I_AM_ROBOT:")
            } else null
        } catch (e: Exception) {
            Log.e("UDP", "Discovery failed", e)
            null
        } finally {
            socket.close()
        }
    }


    // --- ПРОКСИ-МЕТОДЫ (Пробрасываем команды в клиент) ---
    fun requestDevices() = wsClient.requestDevices()
    fun requestDetectedPins() = wsClient.requestDetectedPins()
    fun setMotorSpeed(name: String, speed: Int) = wsClient.setMotorSpeed(name, speed)
    fun setDeviceState(name: String, state: Boolean) = wsClient.setDeviceState(name, state)
    fun subscribeSensor(name: String) = wsClient.subscribeSensor(name)
    fun unsubscribeSensor(name: String) = wsClient.unsubscribeSensor(name)
    fun requestBoardStatus() = wsClient.requestBoardStatus()
    fun requestSensorsSnapshot() = wsClient.requestSensorsSnapshot()
    fun send(text: String) = wsClient.send(text)
}