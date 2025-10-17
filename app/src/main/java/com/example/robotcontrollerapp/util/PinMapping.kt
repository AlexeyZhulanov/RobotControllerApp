package com.example.robotcontrollerapp.util

fun dToGpio(dNumber: Int): Int {
    return when (dNumber) {
        0 -> 16
        1 -> 5
        2 -> 4
        3 -> 0
        4 -> 2
        5 -> 14
        6 -> 12
        7 -> 13
        8 -> 15
        else -> dNumber // fallback
    }
}

fun gpioToD(gpio: Int): Int {
    return when (gpio) {
        16 -> 0
        5 -> 1
        4 -> 2
        0 -> 3
        2 -> 4
        14 -> 5
        12 -> 6
        13 -> 7
        15 -> 8
        else -> gpio
    }
}