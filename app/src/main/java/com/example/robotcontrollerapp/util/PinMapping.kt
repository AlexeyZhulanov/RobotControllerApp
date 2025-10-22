package com.example.robotcontrollerapp.util

fun dToGpio(dNumber: Int): Int {
    return when (dNumber) {
        0 -> 3
        1 -> 1
        2 -> 16
        3, 15 -> 5
        4, 14 -> 4
        5, 13 -> 14
        6, 12 -> 12
        7, 11 -> 13
        8 -> 0
        9 -> 2
        10 -> 15
        else -> dNumber // fallback
    }
}

fun gpioToD(gpio: Int): Int {
    return when (gpio) {
        3 -> 0
        1 -> 1
        16 -> 2
        5 -> 3
        4 -> 4
        14 -> 5
        12 -> 6
        13 -> 7
        0 -> 8
        2 -> 9
        15 -> 10
        else -> gpio
    }
}

// Дублирующиеся пины
val pinAliases = mapOf(
    3 to 15, 15 to 3,
    4 to 14, 14 to 4,
    5 to 13, 13 to 5,
    6 to 12, 12 to 6,
    7 to 11, 11 to 7
)