package com.example.robotcontrollerapp.domain

data class Device(
    val name: String,
    val pin: Int,
    val pin2: Int? = null, // дополнительный пин (необязательный)
    val type: String,
    var state: Boolean = false,
    var pwm: Int = 0,
    val direction: String = "none", // front, rear для сонара и left, right для моторов
    val criticalDist: Int = 5,
    val warningDist: Int = 20,
    val safeSpeed: Int = 100
)