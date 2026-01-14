package com.example.robotcontrollerapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robotcontrollerapp.domain.Device
import kotlin.math.abs

private enum class ControlMode { Individual, Collective }

/**
 * Главный Composable-компонент для управления моторами.
 *
 * @param motors Список настроенных моторов (типа "motor").
 * @param onCommand Лямбда для отправки команды на ESP (имя мотора, скорость от -255 до 255).
 */
@Composable
fun MotorControl(
    modifier: Modifier = Modifier,
    motors: List<Device>, // Передаём отфильтрованный список моторов
    isNeedBackground: Boolean = false,
    onCommand: (name: String, speed: Int) -> Unit
) {
    var controlMode by remember { mutableStateOf(ControlMode.Individual) }

    // Состояние для индивидуальных слайдеров (имя мотора -> значение)
    val sliderValues = remember {
        mutableStateMapOf<String, Float>().apply {
            motors.forEach { put(it.name, 0f) }
        }
    }

    // Состояние для коллективного управления
    var collectiveSpeed by remember { mutableFloatStateOf(0f) }
    var collectiveDirection by remember { mutableIntStateOf(1) } // 1 = FWD, -1 = BWD

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Переключатель режимов
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { controlMode = ControlMode.Individual },
                selected = controlMode == ControlMode.Individual
            ) {
                Text("Раздельно")
            }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { controlMode = ControlMode.Collective },
                selected = controlMode == ControlMode.Collective
            ) {
                Text("Объединенно")
            }
        }

        Spacer(Modifier.height(5.dp))

        // Анимированный контейнер для смены режимов
        AnimatedContent(
            targetState = controlMode,
            label = "ControlModeAnimation",
            transitionSpec = {
                (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false))
            }
        ) { mode ->
            when (mode) {
                ControlMode.Individual -> IndividualMotorControls(
                    motors = motors,
                    sliderValues = sliderValues,
                    isNeedBackground = isNeedBackground,
                    onValueChange = { motorName, newValue ->
                        sliderValues[motorName] = newValue
                    },
                    onValueChangeFinished = { motorName ->
                        val speed = sliderValues[motorName]?.toInt() ?: 0
                        onCommand(motorName, speed)
                    }
                )
                ControlMode.Collective -> CollectiveMotorControls(
                    collectiveSpeed = collectiveSpeed,
                    collectiveDirection = collectiveDirection,
                    isNeedBackground = isNeedBackground,
                    onDirectionChange = { newDirection ->
                        collectiveDirection = newDirection
                        // Немедленно отправляем команду при смене направления
                        motors.forEach {
                            onCommand(it.name, (collectiveSpeed * collectiveDirection).toInt())
                        }
                    },
                    onSpeedChange = { newSpeed ->
                        collectiveSpeed = newSpeed
                    },
                    onSpeedChangeFinished = {
                        // Отправляем команду, когда пользователь отпустил слайдер
                        motors.forEach {
                            onCommand(it.name, (collectiveSpeed * collectiveDirection).toInt())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun IndividualMotorControls(
    motors: List<Device>,
    sliderValues: Map<String, Float>,
    isNeedBackground: Boolean,
    onValueChange: (motorName: String, newValue: Float) -> Unit,
    onValueChangeFinished: (motorName: String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize().drawBehind {
            // Создаем эффект пунктира: 10 пикселей линия, 10 пикселей пробел
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawLine(
                color = Color.Gray, // Цвет линии
                start = Offset(x = 0f, y = size.height / 2.05f),
                end = Offset(x = size.width, y = size.height / 2.05f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect // Применяем эффект пунктира
            )
        }
    ) {

        motors.forEach { motor ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
            ) {
                val sliderValue = sliderValues[motor.name] ?: 0f
                // Вычисляем "силу" (от 0.0 до 1.0)
                val fraction = (abs(sliderValue) / 255f).coerceIn(0f, 1f)
                // Определяем наши цвета
                val dullColor = Color.DarkGray.copy(alpha = if(isNeedBackground) 1f else 0.7f) // Тусклый серый в центре
                val positiveColor = Color.Blue   // Яркий синий (для +)
                val negativeColor = Color.Red    // Яркий красный (для -)
                // Выбираем, в какой цвет красить (в синий или красный)
                val targetColor = if (sliderValue >= 0) positiveColor else negativeColor
                // "Смешиваем" тусклый цвет с нашим целевым цветом
                val dynamicColor = lerp(dullColor, targetColor, fraction)
                // 1. Подпись (извлекаем пины из имени)
                val pins = motor.name.substringAfterLast("motor_").replace("_", "/")
                Text(
                    text = "Мотор (${pins})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontSize = if(motors.size == 3) 14.sp else 12.sp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                // Сам Слайдер
                VerticalSlider(
                    value = sliderValues[motor.name] ?: 0f,
                    onValueChange = { newValue -> onValueChange(motor.name, newValue) },
                    onValueChangeFinished = { onValueChangeFinished(motor.name) },
                    valueRange = -255f..255f,
                    steps = 510,
                    colors = SliderDefaults.colors(
                        thumbColor = dynamicColor, // Бегунок
                        activeTrackColor = dynamicColor, // "Активная" часть дорожки
                        inactiveTrackColor = dynamicColor, // "Неактивная" часть
                        activeTickColor = Color.Transparent, // Скрываем "грязь" от 510 шагов
                        inactiveTickColor = Color.Transparent // Скрываем "грязь" от 510 шагов
                    ),
                    modifier = Modifier.align(Alignment.Center).padding(start = 30.dp, end = 24.dp) // полная инверсия - start = bottom, end = top
                )
                // 4. Текстовое значение
                Text(
                    text = (sliderValues[motor.name] ?: 0f).toInt().toString(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun CollectiveMotorControls(
    collectiveSpeed: Float,
    collectiveDirection: Int,
    isNeedBackground: Boolean,
    onDirectionChange: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeedChangeFinished: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val h = maxHeight
        val topPadding = (h * 0.05f).coerceIn(2.dp, 32.dp)
        val spacerHeight = (h * 0.1f).coerceIn(8.dp, 32.dp)

        Column(
            modifier = Modifier.fillMaxSize().padding(top = topPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Кнопки FWD / BWD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                // Кнопка FWD (вперед)
                Button(
                    onClick = { onDirectionChange(1) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (collectiveDirection == 1) Color(0xFF32A0EB) else Color(0xFFB7B6BA)
                    )
                ) {
                    Text("FWD")
                }
                // Кнопка BWD (назад)
                Button(
                    onClick = { onDirectionChange(-1) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (collectiveDirection == -1) Color(0xFFD86B6B) else Color(0xFFB7B6BA)
                    )
                ) {
                    Text("BWD")
                }
            }

            Spacer(Modifier.height(spacerHeight))

            val fraction = (collectiveSpeed / 255f).coerceIn(0f, 1f)
            val dullColor = Color.DarkGray.copy(alpha = if(isNeedBackground) 1f else 0.7f) // Тусклый серый в начале
            val targetColor = if(collectiveDirection == 1) Color.Blue else Color.Red
            // "Смешиваем" тусклый цвет с нашим целевым цветом
            val dynamicColor = lerp(dullColor, targetColor, fraction)

            // Общий слайдер скорости
            Text(
                text = "Общая скорость: ${collectiveSpeed.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontSize = 16.sp
            )
            Slider(
                value = collectiveSpeed,
                onValueChange = onSpeedChange,
                onValueChangeFinished = onSpeedChangeFinished,
                valueRange = 0f..255f,
                steps = 254,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = dynamicColor, // Бегунок
                    activeTrackColor = dynamicColor, // "Активная" часть дорожки
                    inactiveTrackColor = dynamicColor, // "Неактивная" часть
                    activeTickColor = Color.Transparent, // Скрываем "грязь" от 255 шагов
                    inactiveTickColor = Color.Transparent // Скрываем "грязь" от 255 шагов
                )
            )
        }
    }
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors()
){
    Slider(
        colors = colors,
        interactionSource = interactionSource,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        valueRange = valueRange,
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight,
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .then(modifier)
    )
}