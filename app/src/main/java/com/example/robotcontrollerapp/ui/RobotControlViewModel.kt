package com.example.robotcontrollerapp.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.model.RobotWebSocketClient
import com.example.robotcontrollerapp.model.WsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RobotControlViewModel @Inject constructor() : ViewModel() {
    private val wsClient = RobotWebSocketClient("ws://192.168.4.1:81")

    private val _wsState = MutableStateFlow(WsState.CLOSED)
    val wsState = _wsState.asStateFlow()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _boardInfo = MutableStateFlow("Unknown board")
    val boardInfo = _boardInfo.asStateFlow()

    private val _sensorData = MutableStateFlow<Map<String, Float>>(emptyMap())
    val sensorData = _sensorData.asStateFlow()

    private val _logs = MutableSharedFlow<String>(replay = 50)
    val logs = _logs.asSharedFlow()

    init {
        setupClient()
        wsClient.connect()
        refreshDevices()
    }

    private fun setupClient() {
        wsClient.onStateChanged = { _wsState.value = it }
        wsClient.onLog = {
            Log.d("testLog", it)
            _logs.tryEmit(it)
        }

        wsClient.onBoardInfo = { board, chip ->
            _boardInfo.value = "$board ($chip)"
        }

        wsClient.onDevicesList = { list ->
            Log.d("testDevicesList", list.toString())
            _devices.value = list
        }

        wsClient.onSensorUpdate = { name, value ->
            _sensorData.value = _sensorData.value + (name to value)
        }
    }

    fun toggleDevice(device: Device, on: Boolean) {
        wsClient.setDeviceState(device.name, on)
    }

    fun setMotorSpeed(device: Device, speed: Int) {
        wsClient.setMotorSpeed(device.name, speed)
    }

    fun subscribeSensor(name: String) {
        wsClient.subscribeSensor(name)
    }

    fun unsubscribeSensor(name: String) {
        wsClient.unsubscribeSensor(name)
    }

    fun refreshDevices() {
        wsClient.requestDevices()
    }

    fun refreshBoardStatus() {
        wsClient.requestBoardStatus()
    }

    fun subscribeAllSensors(devices: List<Device>) {
        devices
            .filter { it.type == "sensor" }
            .forEach { d -> wsClient.subscribeSensor(d.name) }
    }

    fun unsubscribeAllSensors(devices: List<Device>) {
        devices
            .filter { it.type == "sensor" }
            .forEach { d -> wsClient.unsubscribeSensor(d.name) }
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.close()
    }
}
