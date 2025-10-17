package com.example.robotcontrollerapp.domain

data class Device(
    val name: String,
    val pin: Int,
    val type: String,
    var state: Boolean = false,
    var pwm: Int = 0
)