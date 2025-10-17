package com.example.robotcontrollerapp.ui

import androidx.lifecycle.ViewModel
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.model.RobotWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PinEditorViewModel @Inject constructor() : ViewModel() {
    private val wsClient = RobotWebSocketClient("ws://192.168.4.1:81")

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _boardName = MutableStateFlow("Unknown")
    val boardName = _boardName.asStateFlow()

    init {
        wsClient.onBoardInfo = { board, _ -> _boardName.value = board }
        wsClient.onDevicesList = { list -> _devices.value = list }
        wsClient.connect()
    }

    fun saveConfig(devices: List<Device>) {
        val json = buildConfigJson(devices)
        wsClient.send(json)
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.close()
    }

    private fun buildConfigJson(devices: List<Device>): String {
        val sb = StringBuilder()
        sb.append("{\"cmd\":\"set_config\",\"config\":{\"devices\":[")
        devices.forEachIndexed { i, d ->
            sb.append("{\"name\":\"${d.name}\",\"pin\":${d.pin},\"type\":\"${d.type}\"}")
            if (i != devices.lastIndex) sb.append(",")
        }
        sb.append("]}}")
        return sb.toString()
    }
}
