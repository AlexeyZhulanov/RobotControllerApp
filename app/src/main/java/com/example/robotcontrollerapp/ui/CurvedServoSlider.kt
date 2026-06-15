package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CurvedServoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isLeftSlider: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    snapThreshold: Float = 8f,
    onValueChangeFinished: () -> Unit,
    sliderWidth: Dp = 12.dp,
    thumbWidth: Dp = 12.dp,
    trackColor: Color = Color.Gray.copy(alpha = 0.4f),
    thumbColor: Color = Color.Black,
    activeTrackColor: Color = Color.DarkGray.copy(alpha = 0.7f),
    textColor: Color = Color.Black
) {
    val sweepAngle = 60f

    val actualTrackColor = if (enabled) trackColor else Color.Gray.copy(alpha = 0.2f)
    val actualActiveColor = if (enabled) activeTrackColor else Color.DarkGray.copy(0.3f)
    val actualThumbColor = if (enabled) thumbColor else Color.Gray.copy(alpha = 0.7f)
    val actualTextColor = if (enabled) textColor else Color.Gray

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput // Блокируем любые касания, если false

                    awaitPointerEventScope {
                        while (true) {
                            // Ждем касания (Tap или начало Drag)
                            val down = awaitFirstDown()

                            fun updateValue(y: Float) {
                                val thumbRadiusPx = 12.dp.toPx()
                                val bottomY = size.height - thumbRadiusPx

                                var progress = 1f - (y - thumbRadiusPx) / (bottomY - thumbRadiusPx)
                                progress = progress.coerceIn(0f, 1f)

                                var newValue = valueRange.start + progress * (valueRange.endInclusive - valueRange.start)

                                // Магнитный центр
                                val centerValue = valueRange.start + (valueRange.endInclusive - valueRange.start) / 2f
                                if (abs(newValue - centerValue) <= snapThreshold) {
                                    newValue = centerValue
                                }

                                onValueChange(newValue.coerceIn(valueRange.start, valueRange.endInclusive))
                            }

                            // Моментально реагируем на тап
                            updateValue(down.position.y)
                            down.consume()

                            // Цикл отслеживания движения
                            var isDragging = true
                            while (isDragging) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()

                                if (change != null && change.pressed) {
                                    updateValue(change.position.y)
                                    change.consume()
                                } else {
                                    // Палец отпустили (или тап завершился)
                                    isDragging = false
                                    onValueChangeFinished.invoke()
                                }
                            }
                        }
                    }
                }
        ) {
            val strokeW = sliderWidth.toPx()
            val thumbW = thumbWidth.toPx()

            val usableHeight = size.height - strokeW * 2
            val halfSweepRad = Math.toRadians((sweepAngle / 2f).toDouble()).toFloat()
            val radius = (usableHeight / 2f) / sin(halfSweepRad)

            val centerX = if (isLeftSlider) strokeW + radius else size.width - strokeW - radius
            val centerY = size.height / 2f

            val rectSize = Size(radius * 2, radius * 2)
            val rectOffset = Offset(centerX - radius, centerY - radius)

            val drawStartAngle = if (isLeftSlider) 180f - (sweepAngle / 2f) else -sweepAngle / 2f

            drawArc(
                color = actualTrackColor,
                startAngle = drawStartAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = rectOffset,
                size = rectSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            val topAngle = if (isLeftSlider) 180f + (sweepAngle / 2f) else -sweepAngle / 2f
            val bottomAngle = if (isLeftSlider) 180f - (sweepAngle / 2f) else sweepAngle / 2f

            val currentProgress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val currentAngle = bottomAngle + currentProgress * (topAngle - bottomAngle)

            val activeStartAngle = min(bottomAngle, currentAngle)
            val activeSweepAngle = abs(bottomAngle - currentAngle)

            drawArc(
                color = actualActiveColor,
                startAngle = activeStartAngle,
                sweepAngle = activeSweepAngle,
                useCenter = false,
                topLeft = rectOffset,
                size = rectSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            val currentAngleRad = Math.toRadians(currentAngle.toDouble()).toFloat()
            val thumbX = centerX + radius * cos(currentAngleRad)
            val thumbY = centerY + radius * sin(currentAngleRad)

            drawCircle(
                color = actualThumbColor,
                radius = thumbW,
                center = Offset(thumbX, thumbY)
            )
        }

        Text(
            text = value.toInt().toString(),
            color = actualTextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(
                start = if (isLeftSlider) sliderWidth + 16.dp else 0.dp,
                end = if (isLeftSlider) 0.dp else sliderWidth + 16.dp
            )
        )
    }
}

@Composable
@Preview
fun TestServo() {
    var leftServoValue by remember { mutableFloatStateOf(90f) } // 180° серво (центр 90)
    var rightServoValue by remember { mutableFloatStateOf(180f) } // 360° серво (центр 180)
    Row(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CurvedServoSlider(
            value = leftServoValue,
            onValueChange = { leftServoValue = it },
            onValueChangeFinished = {},
            valueRange = 0f..180f,
            isLeftSlider = true,
            enabled = false,
            modifier = Modifier.width(80.dp).height(250.dp)
        )
        Box(
            modifier = Modifier.size(175.dp).background(Color.DarkGray)
        ) {
            Text("JOYSTICK", modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
        CurvedServoSlider(
            value = rightServoValue,
            onValueChange = { rightServoValue = it },
            onValueChangeFinished = {},
            valueRange = 0f..360f,
            isLeftSlider = false,
            modifier = Modifier.width(80.dp).height(250.dp)
        )
    }
}