package com.example.robotcontrollerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.model.RobotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
        repository.startAutoConnect()
    }


    fun toggleDevice(device: Device, on: Boolean) {
        repository.setDeviceState(device.name, on)
    }

    fun setMotorSpeed(device: Device, speed: Int) {
        repository.setMotorSpeed(device.name, speed)
    }

    fun setMotorSpeedThrottled(device: Device, speed: Int) {
        repository.setMotorSpeedThrottled(device.name, speed)
    }

    fun setTankSpeed(left: Device, leftSpeed: Int, right: Device, rightSpeed: Int) {
        repository.setTankSpeed(left.name, leftSpeed, right.name, rightSpeed)
    }

    fun subscribeSensor(name: String) {
        repository.subscribeSensor(name)
    }

    fun unsubscribeSensor(name: String) {
        repository.unsubscribeSensor(name)
    }

    fun refreshBoardStatus() {
        repository.requestBoardStatus()
    }

    fun subscribeSensors(names: List<String>) {
        names.forEach { name -> repository.subscribeSensor(name) }
    }

    fun unsubscribeSensors(names: List<String>) {
        names.forEach { name -> repository.unsubscribeSensor(name) }
    }
}
