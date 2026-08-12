package com.example.robotcontrollerapp.model

import android.util.Log
import com.example.robotcontrollerapp.domain.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class RobotRepository @Inject constructor() {

    // Единственный экземпляр клиента на всё приложение
    private val wsClient = RobotWebSocketClient()

    // --- StateFlow (Источники данных для UI) ---

    // Состояние подключения
    private val _wsState = MutableStateFlow(WsState.CLOSED)
    val wsState = _wsState.asStateFlow()

    // Состояние сканирования
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // IP адрес камеры
    private val _cameraIp = MutableStateFlow<String?>(null)
    val cameraIp = _cameraIp.asStateFlow()

    // Список устройств
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    // Данные сенсоров (Карта: "имя_сенсора" -> значение)
    private val _sensorData = MutableStateFlow<Map<String, Float>>(emptyMap())
    val sensorData = _sensorData.asStateFlow()

    // Логи
    private val _logs = MutableStateFlow("")
    val logs = _logs.asStateFlow()

    // Информация о плате
    private val _boardInfo = MutableStateFlow("" to "")
    val boardInfo = _boardInfo.asStateFlow()

    private val _boardName = MutableStateFlow("")
    val boardName = _boardName.asStateFlow()

    // Базовая защита соединения
    private val connectMutex = Mutex()
    private var scanInProgress = false

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var autoConnectStarted = false

    private var autoConnectJob: Job? = null


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
    }

    // --- ГЛАВНАЯ ЛОГИКА ПОДКЛЮЧЕНИЯ ---
    fun startAutoConnect() {
        if (autoConnectStarted) return
        autoConnectStarted = true

        autoConnectJob = repoScope.launch {
            var prevState: WsState? = null

            while (true) {
                val state = wsState.value

                // 1) Если не подключены — пытаемся найти и подключиться
                if (state != WsState.CONNECTED && state != WsState.CONNECTING) {
                    searchAndConnect()
                }

                // 2) Если ТОЛЬКО ЧТО подключились — один раз запросить устройства
                if (state == WsState.CONNECTED && prevState != WsState.CONNECTED) {
                    requestDevices()
                }

                prevState = state
                delay(5000.milliseconds)
            }
        }
    }

    suspend fun searchAndConnect() {
        connectMutex.withLock {
            // 1) Если уже подключены/подключаемся — просто выходим
            if (_wsState.value == WsState.CONNECTED || _wsState.value == WsState.CONNECTING) {
                Log.d("testConnect", "Already connected/connecting, skip search.")
                return
            }
            // 2) Если уже идёт скан — тоже выходим
            if (scanInProgress) {
                Log.d("testConnect", "Scan already in progress, skip search.")
                return
            }
            scanInProgress = true
            _isScanning.value = true
        }

        try {
            _logs.value = "Scanning network for devices..."
            Log.d("testConnect", "Starting UDP scan...")

            scanNetworkForDevices()

            if (_wsState.value != WsState.CONNECTED && _wsState.value != WsState.CONNECTING) {
                _logs.value = "Robot not found."
            } else {
                _logs.value = "Scan finished."
            }

            if (_cameraIp.value != null) {
                Log.d("testConnect", "Camera is ready at ${_cameraIp.value}")
            }
        } finally {
            connectMutex.withLock {
                scanInProgress = false
                _isScanning.value = false
            }
        }
    }

    // --- UDP ПОИСК ---
    private suspend fun scanNetworkForDevices() = withContext(Dispatchers.IO) {
        val socket = DatagramSocket(4210)
        socket.soTimeout = 2000 // Таймаут ожидания одного пакета (2 сек)

        // Будем слушать эфир 6 секунд суммарно
        val endTime = System.currentTimeMillis() + 6000

        try {
            while (System.currentTimeMillis() < endTime) {
                val buf = ByteArray(256)
                val packet = DatagramPacket(buf, buf.size)

                try {
                    // Блокируется тут, пока не придет пакет или не выйдет таймаут (2 сек)
                    socket.receive(packet)

                    val msg = String(packet.data, 0, packet.length).trim()
                    Log.d("testUDP", "Received: $msg")

                    // 1. Нашли РОБОТА
                    if (msg.startsWith("I_AM_ROBOT:")) {
                        val ip = msg.removePrefix("I_AM_ROBOT:")
                        Log.d("testUDP", "Robot found IP: $ip")

                        // Если еще не подключены - подключаемся
                        if (_wsState.value != WsState.CONNECTED && _wsState.value != WsState.CONNECTING) {
                            val url = "ws://$ip:81"
                            wsClient.connect(url)
                            _logs.value = "Connecting to Robot ($ip)..."
                        }
                    }

                    // 2. Нашли КАМЕРУ
                    if (msg.startsWith("I_AM_CAMERA:")) {
                        val ip = msg.removePrefix("I_AM_CAMERA:")
                        if (_cameraIp.value != ip) {
                            _cameraIp.value = ip
                            Log.d("testUDP", "Camera found IP: $ip")
                            _logs.value = "Camera found ($ip)"
                        }
                    }

                    // Оптимизация: Если нашли обоих, можно выйти из цикла раньше
                    if ((_wsState.value == WsState.CONNECTED || _wsState.value == WsState.CONNECTING) && _cameraIp.value != null) {
                        Log.d("testUDP", "Both devices found. Stopping scan.")
                        break
                    }

                } catch (_: SocketTimeoutException) {
                    // Таймаут приема одного пакета - это нормально, просто крутим цикл дальше
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e("UDP", "Discovery error", e)
            _logs.value = "Discovery error: ${e.message}"
        } finally {
            socket.close()
        }
    }

    // --- ПРОКСИ-МЕТОДЫ (Пробрасываем команды в клиент) ---
    fun requestDevices() = wsClient.requestDevices()
    fun setMotorSpeed(name: String, speed: Int) = wsClient.setMotorSpeed(name, speed)
    fun setMotorSpeedThrottled(name: String, speed: Int) = wsClient.setMotorSpeedThrottled(name, speed)
    fun setServoAngle(name: String, angle: Int) = wsClient.setServoAngle(name, angle)
    fun setTankSpeed(leftName: String, leftSpeed: Int, rightName: String, rightSpeed: Int) =
        wsClient.setTankSpeedsThrottled(leftName, leftSpeed, rightName, rightSpeed)
    fun setDeviceState(name: String, state: Boolean) = wsClient.setDeviceState(name, state)
    fun subscribeSensor(name: String) = wsClient.subscribeSensor(name)
    fun unsubscribeSensor(name: String) = wsClient.unsubscribeSensor(name)
    fun requestBoardStatus() = wsClient.requestBoardStatus()
    fun requestSensorsSnapshot() = wsClient.requestSensorsSnapshot()
    fun send(text: String) = wsClient.send(text)
}