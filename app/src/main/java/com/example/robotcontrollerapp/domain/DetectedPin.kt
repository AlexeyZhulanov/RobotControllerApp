package com.example.robotcontrollerapp.domain

data class DetectedPin(
    val pin: Int,
    val state: Int,
    val mode: String
)