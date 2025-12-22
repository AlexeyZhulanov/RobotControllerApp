package com.example.robotcontrollerapp.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.model.RobotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinEditorViewModel @Inject constructor(
    private val repository: RobotRepository
) : ViewModel() {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    val detectedPins = repository.detectedPins
    val boardName = repository.boardName

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            // Если еще не подключены - ищем, если подключены - просто работаем
            repository.searchAndConnect()
            repository.requestDetectedPins()
            repository.requestDevices()
            repository.devices.collect { repoDevices ->
                _devices.value = repoDevices
            }
        }
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
        repository.send(json)
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _errorFlow.emit(message)
        }
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
