package com.example.robotcontrollerapp.ui

import android.app.Activity
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.robotcontrollerapp.R
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.model.WsState
import com.example.robotcontrollerapp.util.fixedTextStyle
import kotlin.collections.chunked
import kotlin.collections.forEach
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor


@Composable
fun ControllerScreen(
    viewModel: RobotControlViewModel = hiltViewModel(),
    onOpenPinEditor: () -> Unit
) {
    val wsState by viewModel.wsState.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val boardInfo by viewModel.boardInfo.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val sonarData by viewModel.sonarData.collectAsStateWithLifecycle()
    val cameraIp by viewModel.cameraIp.collectAsStateWithLifecycle()
    var showCamera by remember { mutableStateOf(false) }

    val dev = remember(devices) {
        devices.filter { it.type != "sensor" && it.type != "motor" && it.type != "servo" && it.type != "sonar" }
    }

    val sensorNames = remember(devices) {
        devices.filter { it.type == "sensor" || it.type == "sonar" }.map { it.name }
    }

    val (motors, servos) = remember(devices) {
        devices.filter { it.type == "motor" } to devices.filter { it.type == "servo" }
    }

    val firstServoAngle = remember(servos) {
        servos.firstOrNull()?.name?.substringAfterLast("_")?.toFloatOrNull() ?: 180f
    }
    val secondServoAngle = remember(servos) {
        servos.getOrNull(1)?.name?.substringAfterLast("_")?.toFloatOrNull() ?: 180f
    }
    val firstServoCenterValue = remember(firstServoAngle) {
        val valueRange = 0f..firstServoAngle
        valueRange.start + (valueRange.endInclusive - valueRange.start) / 2f
    }
    val secondServoCenterValue = remember(secondServoAngle) {
        val valueRange = 0f..secondServoAngle
        valueRange.start + (valueRange.endInclusive - valueRange.start) / 2f
    }

    DisposableEffect(sensorNames) {
        viewModel.subscribeSensors(sensorNames)
        onDispose { viewModel.unsubscribeSensors(sensorNames) }
    }

    // Получаем текущую ориентацию экрана (книжная или альбомная)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFC6C5C9))) {
        if(isLandscape) {
            // Скрываем весь худ (открывается по свайпу)
            HideSystemBarsEffect(hidden = true)
            // Альбомная ориентация
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val topHeight = maxHeight * 0.13f
                val bottomHeight = maxHeight * 0.25f
                val bottomWidth = maxWidth * 0.6f
                val maxH = maxHeight
                val maxW = maxWidth

                // Камера под всем UI
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showCamera) {
                        if(cameraIp != null) {
                            MjpegSurface(
                                url = "http://${cameraIp}:81", // Порт 81 как в прошивке
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(painter = painterResource(R.drawable.cam_placeholder),
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop)
                        }
                    }
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        sonarData.forEach { (device, value) ->
                            val v = value.toInt()
                            key(device.name) {
                                val color = remember(v) {
                                    when(v) {
                                        in 0..device.criticalDist -> Color.Red
                                        in device.criticalDist + 1..device.warningDist -> Color.Yellow
                                        else -> Color.White
                                    }
                                }
                                Row {
                                    Icon(painter = painterResource(R.drawable.ic_parking),
                                        contentDescription = null, tint = color)
                                    Text(text = "${device.direction}: $v", color = color)
                                }
                            }
                        }
                    }
                }

                // Верхняя панель
                TopBarLandscape(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    height = topHeight,
                    boardName = if(boardInfo.first.length < 2) "Unknown" else "${boardInfo.first} (${boardInfo.second})",
                    wsState = wsState,
                    isScanning = isScanning,
                    sensorData = sensorData,
                    onSettingsClick = onOpenPinEditor,
                    onCameraClick = { showCamera = !showCamera }
                )

                // Нижняя панель
                BottomPanelLandscape(
                    modifier = Modifier.align(Alignment.BottomStart),
                    height = bottomHeight,
                    width = bottomWidth,
                    devices = dev,
                    servos = servos,
                    firstServoAngle = firstServoAngle,
                    secondServoAngle = secondServoAngle,
                    firstServoCenterValue = firstServoCenterValue,
                    secondServoCenterValue = secondServoCenterValue,
                    onDeviceToggle = { d, on -> viewModel.toggleDevice(d, on) },
                    onSetServoAngle = { servo, angle -> viewModel.setServoAngle(servo, angle) },
                )

                // Джойстик
                MotorsPanelLandscape(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    h = maxH,
                    w = maxW,
                    motors = motors,
                    onSetMotorSpeed = { motor, speed -> viewModel.setMotorSpeed(motor, speed) },
                    onSetMotorSpeedThrottled = { motor, speed -> viewModel.setMotorSpeedThrottled(motor, speed) },
                    onSetTankSpeed = { lm, ls, rm, rs -> viewModel.setTankSpeed(lm, ls, rm, rs) }
                )
            }
        } else {
            // Книжная ориентация
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Column(modifier = Modifier.align(BiasAlignment(0f, 0.3f))) {
                    sonarData.forEach { (device, value) ->
                        val v = value.toInt()
                        key(device.name) {
                            val color = remember(v) {
                                when(v) {
                                    in 0..device.criticalDist -> Color.Red
                                    in device.criticalDist + 1..device.warningDist -> Color.Yellow
                                    else -> Color.White
                                }
                            }
                            Row {
                                Icon(painter = painterResource(R.drawable.ic_parking),
                                    contentDescription = null, tint = color)
                                Text(text = "${device.direction}: $v", color = color)
                            }
                        }
                    }
                }
                TopBar(
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.TopCenter),
                    boardName = if(boardInfo.first.length < 2) "Unknown" else "${boardInfo.first} (${boardInfo.second})",
                    wsState = wsState,
                    isScanning = isScanning,
                    onSettingsClick = onOpenPinEditor,
                    onCameraClick = { showCamera = !showCamera }
                )
                Column(Modifier.padding(top = 80.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    TopPanel(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        sensorData = sensorData,
                        dev = dev,
                        onDeviceToggle = { d, on -> viewModel.toggleDevice(d, on) }
                    )
                    if(showCamera) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                            contentAlignment = Alignment.Center) {
                            if(cameraIp != null) {
                                MjpegSurface(
                                    url = "http://${cameraIp}:81", // Порт 81 как в прошивке
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    val w = remember(showCamera, sensorData.size, dev.size, servos.size, motors.size) {
                        if(showCamera) {
                            when {
                                sensorData.isEmpty() -> 1.5f
                                dev.isEmpty() -> 1.5f
                                sensorData.size <= 3 && dev.size <= 3 -> 1.2f
                                motors.size > 2 && servos.isNotEmpty() -> 1.2f
                                else -> 1f
                            }
                        } else if(motors.size > 2 && servos.isNotEmpty()) 1f else 0.7f
                    }
                    BottomPanel(
                        modifier = Modifier.weight(w).fillMaxWidth(),
                        motors = motors,
                        servos = servos,
                        firstServoAngle = firstServoAngle,
                        secondServoAngle = secondServoAngle,
                        firstServoCenterValue = firstServoCenterValue,
                        secondServoCenterValue = secondServoCenterValue,
                        onSetMotorSpeed = { motor, speed -> viewModel.setMotorSpeed(motor, speed) },
                        onSetMotorSpeedThrottled = { motor, speed -> viewModel.setMotorSpeedThrottled(motor, speed) },
                        onSetServoAngle = { servo, angle -> viewModel.setServoAngle(servo, angle) },
                        onSetTankSpeed = { lm, ls, rm, rs -> viewModel.setTankSpeed(lm, ls, rm, rs) }
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(
    modifier: Modifier,
    boardName: String,
    wsState: WsState,
    isScanning: Boolean,
    onSettingsClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_cam),
            contentDescription = "CameraMode",
            modifier = Modifier
                .size(45.dp)
                .padding(3.dp)
                .clickable(onClick = { onCameraClick() })
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = boardName, color = Color.Black, fontSize = 20.sp)
            if(isScanning && wsState != WsState.CONNECTED) {
                AnimatedConnectingText()
            } else
            when(wsState) {
                WsState.CONNECTED -> Text(text = "Подключено", color = Color(0xFF4CAF50), fontSize = 16.sp)
                WsState.CONNECTING -> AnimatedConnectingText(baseText = "Подключение")
                WsState.CLOSED -> Text(text = "Отключено", color = Color.DarkGray.copy(0.6f), fontSize = 16.sp)
                WsState.ERROR -> Text(text = "Ошибка", color = Color.Red, fontSize = 16.sp)
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_chip),
            contentDescription = "MicroChip",
            modifier = Modifier.size(45.dp).clickable { onSettingsClick() }
        )
    }
}

@Composable
fun TopBarLandscape(
    modifier: Modifier,
    height: Dp,
    boardName: String,
    wsState: WsState,
    isScanning: Boolean,
    sensorData: Map<String, Float>,
    onSettingsClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    val iconsSize = height * 0.9f
    val sensorList = sensorData.toList()
    val firstRow = sensorList.take(6)
    val secondRow = sensorList.drop(6)
    val parentHeight = if(secondRow.isEmpty()) height else height * 2 + 32.dp
    Box(modifier = modifier.height(parentHeight)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(height).background(Color.Black.copy(alpha = 0.3f))) {
                Image(
                    painter = painterResource(R.drawable.ic_cam),
                    contentDescription = "CameraMode",
                    modifier = Modifier
                        .size(iconsSize)
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, top = 3.dp, bottom = 3.dp, end = 3.dp)
                        .clickable(onClick = { onCameraClick() })
                )
                Image(
                    painter = painterResource(R.drawable.ic_chip),
                    contentDescription = "MicroChip",
                    modifier = Modifier
                        .size(iconsSize)
                        .align(Alignment.BottomEnd)
                        .padding(end = 7.dp)
                        .clickable { onSettingsClick() }
                )
                Row(modifier = Modifier.fillMaxWidth().height(height).padding(horizontal = iconsSize + 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val halfSize = (firstRow.size + 1) / 2
                    val firstHalf = firstRow.take(halfSize)
                    val secondHalf = firstRow.drop(halfSize)
                    Row(modifier = Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        firstHalf.forEach { (name, value) ->
                            SensorLandscape(modifier = Modifier, name = name, value = value)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                        val h1 = height * 0.3f
                        val h2 = height * 0.28f
                        Text(text = boardName, color = Color.White, fontSize = h1.value.sp, style = fixedTextStyle)
                        if(isScanning && wsState != WsState.CONNECTED) {
                            AnimatedConnectingText(fontSize = h2.value.sp, textStyle = fixedTextStyle)
                        } else
                            when(wsState) {
                                WsState.CONNECTED -> Text(text = "Подключено", color = Color(0xFF4CAF50), fontSize = h2.value.sp, style = fixedTextStyle)
                                WsState.CONNECTING -> AnimatedConnectingText(fontSize = h2.value.sp, baseText = "Подключение", textStyle = fixedTextStyle)
                                WsState.CLOSED -> Text(text = "Отключено", color = Color.LightGray.copy(0.7f), fontSize = h2.value.sp, style = fixedTextStyle)
                                WsState.ERROR -> Text(text = "Ошибка", color = Color.Red, fontSize = h2.value.sp, style = fixedTextStyle)
                            }
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        secondHalf.forEach { (name, value) ->
                            SensorLandscape(modifier = Modifier, name = name, value = value)
                        }
                    }
                }
            }
            val rowHeight = parentHeight - height
            SlideOutControlPanel(
                modifier = Modifier.fillMaxWidth().height(rowHeight),
                panelSize = rowHeight - 64.dp,
                isVertical = true,
                side = SlideSide.RightBottom
            ) {
                Row(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    secondRow.forEach { (name, value) ->
                        SensorLandscape(modifier = Modifier, name = name, value = value)
                    }
                }
            }
        }
    }
}

@Composable
fun TopPanel(modifier: Modifier, sensorData: Map<String, Float>, dev: List<Device>,
             onDeviceToggle: (Device, Boolean) -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val weight1 = remember(sensorData.size) {
            when {
                sensorData.size in 0..2 -> 1f
                sensorData.size % 3 == 0 -> sensorData.size / 3f
                else -> (sensorData.size / 3 + 1).toFloat()
            }
        }
        val weight2 = remember(dev.size) {
            when {
                dev.size in 0..2 -> 1f
                dev.size % 3 == 0 -> dev.size / 3f
                else -> (dev.size / 3 + 1).toFloat()
            }
        }
        if(sensorData.isNotEmpty()) {
            Sensors(
                modifier = Modifier.weight(weight1),
                rowCount = weight1.toInt(),
                data = sensorData
            )
        }
        if(dev.isNotEmpty()) {
            CustomButtons(
                modifier = Modifier.weight(weight2),
                rowCount = weight2.toInt(),
                devices = dev,
                onDeviceToggle = { d, on -> onDeviceToggle(d, on) }
            )
        }
    }
}

@Composable
fun MotorsPanelLandscape(
    modifier: Modifier,
    h: Dp,
    w: Dp,
    motors: List<Device>,
    onSetMotorSpeed: (Device, Int) -> Unit,
    onSetMotorSpeedThrottled: (Device, Int) -> Unit,
    onSetTankSpeed: (leftMotor: Device, leftSpeed: Int, rightMotor: Device, rightSpeed: Int) -> Unit
) {
    when(motors.size) {
        0 -> {} // нет моторов - даже не показываем панель
        in 1..2 -> {
            SlideOutControlPanel(
                modifier = modifier.height(h * 0.6f),
                panelSize = w * 0.3f
            ) {
                BoxWithConstraints {
                    val size = min(maxWidth, maxHeight) * 0.75f
                    Joystick(
                        modifier = Modifier.fillMaxSize(),
                        size = size,
                        enabled = motors.isNotEmpty(),
                        isTransparent = true,
                        onMotorsChanged = { leftPwmSigned, rightPwmSigned ->
                            when(motors.size) {
                                1 -> {
                                    // один мотор — используем только Y (мы уже получили signed значение в leftPwmSigned)
                                    val motor = motors[0]
                                    // вызываем viewModel, передаём signed speed
                                    onSetMotorSpeedThrottled(motor, leftPwmSigned)
                                }
                                else -> {
                                    // два мотора - используем первые два как лев/право
                                    val leftMotor = motors[0]
                                    val rightMotor = motors[1]
                                    onSetTankSpeed(leftMotor, leftPwmSigned, rightMotor, rightPwmSigned)
                                }
                            }
                        }
                    )
                }
            }
        }
        else -> {
            // три и более мотора
            SlideOutControlPanel(
                modifier = modifier.height(h * 0.65f),
                panelSize = w * 0.4f
            ) {
                MotorControl(
                    modifier = Modifier.fillMaxSize(),
                    motors = motors,
                    isNeedBackground = true,
                    isNeedFixPadding = true,
                    onCommand = { motorName, motorSpeed ->
                        val motor = motors.find { it.name == motorName }
                        if (motor != null) {
                            onSetMotorSpeed(motor, motorSpeed)
                        } else {
                            // Сюда код не должен попасть, но это хорошая проверка
                            Log.e("testMotorControl", "Ошибка: мотор с именем $motorName не найден!")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomPanel(
    modifier: Modifier,
    motors: List<Device>,
    servos: List<Device>,
    firstServoAngle: Float,
    secondServoAngle: Float,
    firstServoCenterValue: Float,
    secondServoCenterValue: Float,
    snapThreshold: Float = 8f,
    onSetMotorSpeed: (Device, Int) -> Unit,
    onSetMotorSpeedThrottled: (Device, Int) -> Unit,
    onSetServoAngle: (Device, Int) -> Unit,
    onSetTankSpeed: (leftMotor: Device, leftSpeed: Int, rightMotor: Device, rightSpeed: Int) -> Unit
) {
    var firstServoValue by remember(firstServoAngle) { mutableFloatStateOf(firstServoAngle / 2f) }
    var secondServoValue by remember(secondServoAngle) { mutableFloatStateOf(secondServoAngle / 2f) }

    val paddingMod = remember(servos.size) {
        if(servos.size == 1) 48.dp else 24.dp
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // todo сюда перенести сонар
        when(motors.size) {
            in 0..2 -> {
                BoxWithConstraints {
                    val size = min(maxWidth, maxHeight) * 0.7f
                    val servoWidth = maxWidth * 0.17f
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        if(servos.isNotEmpty()) {
                            val firstServo = servos.first()
                            CurvedServoSlider(
                                value = firstServoValue,
                                onValueChange = { firstServoValue = it },
                                onValueChangeFinished = { onSetServoAngle(firstServo, firstServoValue.toInt()) },
                                valueRange = 0f..firstServoAngle,
                                isLeftSlider = true,
                                sliderWidth = 16.dp,
                                thumbWidth = 14.dp,
                                modifier = Modifier.width(servoWidth).height(size * 1.2f).offset(y = size * 0.1f)
                            )
                        }
                        Joystick(
                            size = size,
                            enabled = motors.isNotEmpty(),
                            onMotorsChanged = { leftPwmSigned, rightPwmSigned ->
                                when(motors.size) {
                                    0 -> {
                                        // нет моторов — ничего не делаем
                                    }
                                    1 -> {
                                        // один мотор — используем только Y (мы уже получили signed значение в leftPwmSigned)
                                        val motor = motors[0]
                                        // вызываем viewModel, передаём signed speed
                                        onSetMotorSpeedThrottled(motor, leftPwmSigned)
                                    }
                                    else -> {
                                        // два мотора - используем первые два как лев/право
                                        val leftMotor = motors[0]
                                        val rightMotor = motors[1]
                                        onSetTankSpeed(leftMotor, leftPwmSigned, rightMotor, rightPwmSigned)
                                    }
                                }
                            }
                        )
                        if(servos.isNotEmpty()) {
                            val hasSecondServo = servos.size > 1
                            CurvedServoSlider(
                                value = secondServoValue,
                                onValueChange = { secondServoValue = it },
                                onValueChangeFinished = {
                                    if (hasSecondServo) {
                                        val secondServo = servos[1]
                                        onSetServoAngle(secondServo, secondServoValue.toInt())
                                    }
                                },
                                valueRange = 0f..secondServoAngle,
                                isLeftSlider = false,
                                sliderWidth = 16.dp,
                                thumbWidth = 14.dp,
                                enabled = hasSecondServo,
                                modifier = Modifier.width(servoWidth).height(size * 1.2f).offset(y = size * 0.1f)
                            )
                        }
                    }
                }
            }
            else -> {
                // три и более моторов
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if(servos.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = paddingMod)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                                Text(text = firstServoValue.toInt().toString(), color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold, style = fixedTextStyle)
                                val firstServo = servos.first()
                                Slider(
                                    value = firstServoValue,
                                    onValueChange = { newValue ->
                                        firstServoValue = if (abs(newValue - firstServoCenterValue) <= snapThreshold) {
                                            firstServoCenterValue
                                        } else {
                                            newValue
                                        }
                                    },
                                    onValueChangeFinished = { onSetServoAngle(firstServo, firstServoValue.toInt()) },
                                    valueRange = 0f..firstServoAngle,
                                    steps = firstServoAngle.toInt(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Black,
                                        activeTrackColor = Color.DarkGray,
                                        inactiveTrackColor = Color.Gray,
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    )
                                )
                            }
                            if(servos.size > 1) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                                    Text(text = secondServoValue.toInt().toString(), color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold, style = fixedTextStyle)
                                    val secondServo = servos[1]
                                    Slider(
                                        value = secondServoValue,
                                        onValueChange = { newValue ->
                                            secondServoValue = if (abs(newValue - secondServoCenterValue) <= snapThreshold) {
                                                secondServoCenterValue
                                            } else {
                                                newValue
                                            }
                                        },
                                        onValueChangeFinished = { onSetServoAngle(secondServo, secondServoValue.toInt()) },
                                        valueRange = 0f..secondServoAngle,
                                        steps = secondServoAngle.toInt(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.Black,
                                            activeTrackColor = Color.DarkGray,
                                            inactiveTrackColor = Color.Gray,
                                            activeTickColor = Color.Transparent,
                                            inactiveTickColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                    MotorControl(
                        motors = motors,
                        onCommand = { motorName, motorSpeed ->
                            val motor = motors.find { it.name == motorName }
                            if (motor != null) {
                                onSetMotorSpeed(motor, motorSpeed)
                            } else {
                                // Сюда код не должен попасть, но это хорошая проверка
                                Log.e("testMotorControl", "Ошибка: мотор с именем $motorName не найден!")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomPanelLandscape(
    modifier: Modifier,
    height: Dp,
    width: Dp,
    devices: List<Device>,
    servos: List<Device>,
    firstServoAngle: Float,
    secondServoAngle: Float,
    firstServoCenterValue: Float,
    secondServoCenterValue: Float,
    snapThreshold: Float = 8f,
    onDeviceToggle: (Device, Boolean) -> Unit,
    onSetServoAngle: (Device, Int) -> Unit,
) {
    val horizontalSpacing = 10.dp
    val count = devices.size
    val availableWidth = width - horizontalSpacing * (count - 1)
    val emptyCoef = if(devices.isEmpty()) 0 else 1

    val maxCountInRow = remember(width) {
        floor(availableWidth / 45.dp)
    }
    val rowsCount = remember(devices.size, maxCountInRow) {
        if(count % maxCountInRow.toInt() == 0) count / maxCountInRow.toInt()
        else floor(count / maxCountInRow + 1f).toInt()
    }

    var firstServoValue by remember(firstServoAngle) { mutableFloatStateOf(firstServoAngle / 2f) }
    var secondServoValue by remember(secondServoAngle) { mutableFloatStateOf(secondServoAngle / 2f) }

    val verticalSpacing = 5.dp
    val servoHeight = if(servos.isEmpty()) 0.dp else 80.dp
    val servoPadding = if(servos.size == 1) 110.dp else 50.dp
    val h = (height * rowsCount + (verticalSpacing - 32.dp) * (rowsCount - 1)) * emptyCoef + servoHeight
    if(devices.isNotEmpty() || servos.isNotEmpty()) {
        SlideOutControlPanel(
            modifier = modifier.width(width).height(h),
            panelSize = h,
            isVertical = true
        ) {
            val maxCountInRow = ceil(count / rowsCount.toFloat()).toInt()
            val availableHeight = height - 32.dp
            val cellWidth = availableWidth / maxCountInRow
            val cellSize = minOf(cellWidth, availableHeight)
            val deviceRows = if(devices.isEmpty()) listOf() else devices.reversed().chunked(maxCountInRow).reversed()
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(verticalSpacing), horizontalAlignment = Alignment.CenterHorizontally) {
                deviceRows.forEach { deviceRow ->
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        deviceRow.reversed().forEach { device ->
                            key(device.name) {
                                CustomButton(
                                    width = cellSize,
                                    height = cellSize,
                                    isQuad = true,
                                    isLandscape = true,
                                    device = device,
                                    onDeviceToggle = { d, on -> onDeviceToggle(d, on) }
                                )
                            }
                        }
                    }
                }
                if(servos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(start = servoPadding, end = servoPadding, bottom = 5.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                            Text(text = firstServoValue.toInt().toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, style = fixedTextStyle)
                            val firstServo = servos.first()
                            Slider(
                                value = firstServoValue,
                                onValueChange = { newValue ->
                                    firstServoValue = if (abs(newValue - firstServoCenterValue) <= snapThreshold) {
                                        firstServoCenterValue
                                    } else {
                                        newValue
                                    }
                                },
                                onValueChangeFinished = { onSetServoAngle(firstServo, firstServoValue.toInt()) },
                                valueRange = 0f..firstServoAngle,
                                steps = firstServoAngle.toInt(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFF0F0F0),
                                    activeTrackColor = Color(0xFFBFBFBF),
                                    inactiveTrackColor = Color(0xFFF0F0F0),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                )
                            )
                        }
                        if(servos.size > 1) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                                Text(text = secondServoValue.toInt().toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, style = fixedTextStyle)
                                val secondServo = servos[1]
                                Slider(
                                    value = secondServoValue,
                                    onValueChange = { newValue ->
                                        secondServoValue = if (abs(newValue - secondServoCenterValue) <= snapThreshold) {
                                            secondServoCenterValue
                                        } else {
                                            newValue
                                        }
                                    },
                                    onValueChangeFinished = { onSetServoAngle(secondServo, secondServoValue.toInt()) },
                                    valueRange = 0f..secondServoAngle,
                                    steps = secondServoAngle.toInt(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFF0F0F0),
                                        activeTrackColor = Color(0xFFF0F0F0),
                                        inactiveTrackColor = Color(0xFFBFBFBF),
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Sensors(modifier: Modifier, rowCount: Int, data: Map<String, Float>) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        val verticalSpacing = 10.dp
        val horizontalSpacing = 16.dp

        val availableHeight = maxHeight - verticalSpacing * (rowCount - 1)

        var cellHeight = availableHeight / rowCount
        var cellWidth = (maxWidth - horizontalSpacing * 2) / 3

        if(cellHeight > cellWidth) cellHeight = cellWidth
        val isQuad = !isDifferenceMoreThan40Percent(cellWidth.value, cellHeight.value)
        if(isQuad) {
            if(cellWidth > cellHeight) cellWidth = cellHeight
        }
        Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
            val chunkedMaps = data.toList().chunked(3).map { it.toMap() }
            chunkedMaps.forEach { rowMaps ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowMaps.forEach { (key, value) ->
                        key(key) {
                            Sensor(
                                width = cellWidth,
                                height = cellHeight,
                                isQuad = isQuad,
                                name = key,
                                value = value
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomButtons(modifier: Modifier, rowCount: Int, devices: List<Device>, onDeviceToggle: (Device, Boolean) -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        val verticalSpacing = 10.dp
        val horizontalSpacing = 16.dp

        val availableHeight = maxHeight - verticalSpacing * (rowCount - 1)

        var cellHeight = availableHeight / rowCount
        var cellWidth = (maxWidth - horizontalSpacing * 2) / 3

        if(cellHeight > cellWidth) cellHeight = cellWidth
        val isQuad = !isDifferenceMoreThan40Percent(cellWidth.value, cellHeight.value)
        if(isQuad) {
            if(cellWidth > cellHeight) cellWidth = cellHeight
        }
        Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
            val chunked = devices.chunked(3)
            chunked.forEach { rowDevices ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowDevices.forEach { device ->
                        key(device.name) {
                            CustomButton(
                                width = cellWidth,
                                height = cellHeight,
                                isQuad = isQuad,
                                device = device,
                                onDeviceToggle = { d, on -> onDeviceToggle(d, on) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Sensor(width: Dp, height: Dp, isQuad: Boolean, name: String, value: Float) {
    val avgSizeForWidth = (width + height) / 2
    val avgSize = (width + height + height) / 3
    val rcsSize = avgSize / 6
    val startPadding = rcsSize / 2
    val fs1 = (avgSize / 5.7f).value
    val fs2 = (avgSize / 3.5f).value
    val fraction = if(isQuad) 0.8f else 1f
    val finalName = if(name.length > 9) name.replace("_sensor", "") else name
    Box(modifier = Modifier.width(avgSizeForWidth).height(height).background(Color.White, RoundedCornerShape(rcsSize)), contentAlignment = Alignment.CenterStart) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight(fraction = fraction).padding(start = startPadding)) {
            Text(text = finalName, color = Color.Gray, fontSize = fs1.sp, style = fixedTextStyle)
            Text(text = value.toString(), color = Color.Black, fontSize = fs2.sp, style = fixedTextStyle)
        }
    }
}

@Composable
fun SensorLandscape(modifier: Modifier, name: String, value: Float) {
    Box(modifier = modifier.background(color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(4.dp)).padding(vertical = 2.dp, horizontal = 4.dp)) {
        Column {
            Text(
                text = name,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 1,
                style = fixedTextStyle
            )
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = 18.sp,
                maxLines = 1,
                style = fixedTextStyle
            )
        }
    }
}

@Composable
fun CustomButton(width: Dp, height: Dp, isQuad: Boolean, device: Device,
                 isLandscape: Boolean = false, onDeviceToggle: (Device, Boolean) -> Unit) {
    var isOn by remember(device.state) { mutableStateOf(device.state) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val avgSizeForWidth = (width + height) / 2
    val avgSize = (width + height + height) / 3
    val rcsSize = avgSize / 6
    val fraction = if(isQuad) 0.9f else 1f
    val fs = (avgSize / 7f).value
    val imageWeight = if(isQuad) 3f else 2.5f

    val bColor = when {
        isPressed -> if(isLandscape) Color(0xFFB5E0C2).copy(alpha = 0.5f) else Color(0xFFB5E0C2)
        isOn -> if(isLandscape) Color(0xFF81C784).copy(alpha = 0.8f) else Color(0xFF81C784)
        else -> if(isLandscape) Color.Black.copy(alpha = 0.4f) else Color(0xFFF0F0F0)
    }
    val backgroundColor by animateColorAsState(
        targetValue = bColor,
        label = "buttonColor"
    )
    val textColor = if(isLandscape) Color.White else Color.Black
    Box(
        modifier = Modifier
            .width(avgSizeForWidth)
            .height(height)
            .clip(RoundedCornerShape(rcsSize))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = Color.Black.copy(alpha = 0.2f)
                ),
                onClick = {
                    isOn = !isOn
                    onDeviceToggle(device, isOn)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(fraction = fraction).fillMaxWidth()
        ) {
            Icon(painter = painterResource(R.drawable.ic_bulb), contentDescription = "IconButton", modifier = Modifier.weight(imageWeight).fillMaxWidth(), tint = textColor)
            Text(text = device.name, color = textColor, fontSize = fs.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = fixedTextStyle)
        }
    }
}

@Composable
fun AnimatedConnectingText(
    modifier: Modifier = Modifier,
    baseText: String = "Сканирование",
    fontSize: TextUnit = 16.sp,
    textColor: Color = Color.Unspecified,
    textStyle: TextStyle = LocalTextStyle.current
) {
    val transition = rememberInfiniteTransition(label = "dotsTransition")

    // 0..3, меняется каждые 500мс
    val step by transition.animateValue(
        initialValue = 1,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotCount"
    )

    Text(
        text = "$baseText${".".repeat(step)}",
        modifier = modifier,
        fontSize = fontSize,
        color = textColor,
        style = textStyle
    )
}

fun isDifferenceMoreThan40Percent(a: Float, b: Float): Boolean {
    if (a == b) return false
    if (a == 0f || b == 0f) return true

    val max = maxOf(a, b)
    val min = minOf(a, b)

    // Разница в процентах относительно большего числа
    val percentage = ((max - min) / max) * 100

    return percentage > 40
}

@Composable
fun HideSystemBarsEffect(hidden: Boolean) {
    val view = LocalView.current

    DisposableEffect(hidden) {
        val window = (view.context as Activity).window

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, view)

        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose { }
    }
}
