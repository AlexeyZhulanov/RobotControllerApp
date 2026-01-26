package com.example.robotcontrollerapp.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.model.RobotRepository
import com.example.robotcontrollerapp.model.WsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RobotControlViewModel @Inject constructor(
    private val repository: RobotRepository
) : ViewModel() {

    val devices = repository.devices
    val wsState = repository.wsState
    val logs = repository.logs // todo можно использовать потом
    val boardInfo = repository.boardInfo
    val cameraIp = repository.cameraIp
    val isScanning = repository.isScanning

    // --- ЛОГИКА ФИЛЬТРАЦИИ СЕНСОРОВ ---
    private val subscribedSensorNames = devices.map { deviceList ->
        deviceList
            .filter { it.type.trim().equals("sensor", ignoreCase = true) }
            .map { it.name }
            .toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // sensorData будет содержать только те сенсоры, которые есть в subscribedSensorNames
    val sensorData = combine(repository.sensorData, subscribedSensorNames) { allSensors, mySubscriptions ->
        allSensors.filterKeys { it in mySubscriptions }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            while(true) {
                val state = repository.wsState.value
                if(state != WsState.CONNECTED && state != WsState.CONNECTING) {
                    repository.searchAndConnect()
                    refreshDevices()
                }
                delay(5000)
            }
        }
    }


    fun toggleDevice(device: Device, on: Boolean) {
        repository.setDeviceState(device.name, on)
    }

    fun setMotorSpeed(device: Device, speed: Int) {
        repository.setMotorSpeed(device.name, speed)
    }

    fun subscribeSensor(name: String) {
        repository.subscribeSensor(name)
    }

    fun unsubscribeSensor(name: String) {
        repository.unsubscribeSensor(name)
    }

    fun refreshDevices() {
        repository.requestDevices()
    }

    fun refreshBoardStatus() {
        repository.requestBoardStatus()
    }

    fun subscribeAllSensors(devices: List<Device>) {
        devices
            .filter { it.type == "sensor" }
            .forEach { d -> repository.subscribeSensor(d.name) }
    }

    fun unsubscribeAllSensors(devices: List<Device>) {
        devices
            .filter { it.type == "sensor" }
            .forEach { d -> repository.unsubscribeSensor(d.name) }
    }
}
