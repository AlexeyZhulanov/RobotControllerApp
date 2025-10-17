package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    onMotorsChanged: (leftSpeed: Int, rightSpeed: Int) -> Unit
) {
    val radius = size / 2
    var handlePosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = handlePosition + dragAmount
                        val distance = newOffset.getDistance()
                        val clampedOffset = if (distance < radius.toPx()) {
                            newOffset
                        } else {
                            val angle = atan2(newOffset.y, newOffset.x)
                            Offset(
                                cos(angle) * radius.toPx(),
                                sin(angle) * radius.toPx()
                            )
                        }
                        handlePosition = clampedOffset

                        // нормализуем координаты (-1..1)
                        val normX = handlePosition.x / radius.toPx()
                        val normY = -handlePosition.y / radius.toPx()

                        // сила (0..1)
                        val strength = handlePosition.getDistance() / radius.toPx()

                        // танковое управление
                        val left = (normY + normX).coerceIn(-1f, 1f)
                        val right = (normY - normX).coerceIn(-1f, 1f)

                        // масштабируем до 0–255
                        val leftPwm = ((left * strength * 255f).toInt()).coerceIn(-255, 255)
                        val rightPwm = ((right * strength * 255f).toInt()).coerceIn(-255, 255)

                        onMotorsChanged(leftPwm, rightPwm)
                    },
                    onDragEnd = {
                        handlePosition = Offset.Zero
                        onMotorsChanged(0, 0)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // внешний круг
        Box(
            modifier = Modifier
                .size(size)
                .background(Color.LightGray, shape = CircleShape)
        )
        // внутренний круг (ручка)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        handlePosition.x.roundToInt(),
                        handlePosition.y.roundToInt()
                    )
                }
                .size(size / 3)
                .background(Color.DarkGray, shape = CircleShape)
        )
    }
}
