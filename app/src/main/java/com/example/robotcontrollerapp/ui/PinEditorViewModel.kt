package com.example.robotcontrollerapp.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.robotcontrollerapp.domain.DetectedPin
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.model.RobotWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinEditorViewModel @Inject constructor() : ViewModel() {
    private val wsClient = RobotWebSocketClient("ws://192.168.4.1:81")

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _detectedPins = MutableStateFlow<List<DetectedPin>>(emptyList())
    val detectedPins = _detectedPins.asStateFlow()

    private val _boardName = MutableStateFlow("Unknown")
    val boardName = _boardName.asStateFlow()

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    init {
        wsClient.onBoardInfo = { board, _ -> _boardName.value = board }
        wsClient.onDevicesList = { list -> _devices.value = list }
        wsClient.onDetectedPins = { pins -> _detectedPins.value = pins }
        wsClient.onDeviceAdded = { n, p, t -> _devices.value = _devices.value + Device(n, p, type = t) }
        wsClient.connect()
        wsClient.requestDetectedPins()
        wsClient.requestDevices()
    }

    fun onDeviceSelected(device: Device) {
        val names = _devices.value.map { it.name }.toSet()
        if(device.name !in names) {
            _devices.value = _devices.value + device
        } else showError("Ошибка: Такое имя уже занято")
    }

    fun onDeviceRemoved(device: Device) {
        _devices.value = _devices.value - device
    }

    fun saveConfig(devices: List<Device>) {
        Log.d("testSaveCfg", devices.toString())
        val json = buildConfigJson(devices)
        wsClient.send(json)
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _errorFlow.emit(message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.close()
    }

    private fun buildConfigJson(devices: List<Device>): String {
        val sb = StringBuilder()
        sb.append("{\"cmd\":\"set_config\",\"config\":{\"devices\":[")
        devices.forEachIndexed { i, d ->
            sb.append("{\"name\":\"${d.name}\",\"pin\":${d.pin},\"type\":\"${d.type}\"")
            if (d.type == "motor" && d.pin2 != null) {
                sb.append(",\"pin2\":${d.pin2}")
            }
            sb.append("}")
            if (i != devices.lastIndex) sb.append(",")
        }
        sb.append("]}}")
        return sb.toString()
    }
}
