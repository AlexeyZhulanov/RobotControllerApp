package com.example.robotcontrollerapp.ui

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.robotcontrollerapp.R
import com.example.robotcontrollerapp.domain.Device
import com.example.robotcontrollerapp.model.WsState
import com.example.robotcontrollerapp.util.fixedTextStyle
import kotlinx.coroutines.delay
import kotlin.collections.chunked
import kotlin.collections.forEach

sealed interface ControllerScreenLayoutParams {
    val parentBoxModifier: Modifier
    val topBarModifier: Modifier
    val topBarAlignment: Alignment
    val parentColumnModifier: Modifier
    val joystickModifier: Modifier
    val joystickAlignment: Alignment
    val joystickSize: Dp
    val motorControlModifier: Modifier
    val motorControlAlignment: Alignment

    object Portrait : ControllerScreenLayoutParams {
        override val parentBoxModifier: Modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        override val topBarModifier: Modifier = Modifier.padding(top = 8.dp)
        override val topBarAlignment: Alignment = Alignment.TopCenter
        override val parentColumnModifier: Modifier = Modifier.padding(top = 80.dp)
        override val joystickModifier: Modifier = Modifier.padding(bottom = 20.dp)
        override val joystickAlignment: Alignment = Alignment.BottomCenter
        override val joystickSize: Dp = 200.dp
        override val motorControlModifier: Modifier = Modifier
        override val motorControlAlignment: Alignment = Alignment.BottomCenter
    }
    object Landscape : ControllerScreenLayoutParams {
        // todo custom params
        override val parentBoxModifier: Modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        override val topBarModifier: Modifier = Modifier.padding(top = 8.dp)
        override val topBarAlignment: Alignment = Alignment.TopCenter
        override val parentColumnModifier: Modifier = Modifier.padding(top = 80.dp)
        override val joystickModifier: Modifier = Modifier
        override val joystickAlignment: Alignment = Alignment.BottomCenter
        override val joystickSize: Dp = 200.dp
        override val motorControlModifier: Modifier = Modifier
        override val motorControlAlignment: Alignment = Alignment.BottomCenter
    }
}

@Composable
fun ControllerScreen(
    viewModel: RobotControlViewModel = hiltViewModel(),
    onOpenPinEditor: () -> Unit
) { // todo вернуть обратно
    //val wsState by viewModel.wsState.collectAsState()
    //val boardInfo by viewModel.boardInfo.collectAsState()
    //val devices by viewModel.devices.collectAsState()
    //val sensorData by viewModel.sensorData.collectAsState()
    val cameraIp by viewModel.cameraIp.collectAsState()
    var showCamera by remember { mutableStateOf(false) }

    // FAKE DATA (на время тестирования UI) TODO
    val wsState = WsState.CONNECTED
    val boardInfo = "ESP32" to "123123123"
    val devices = listOf(Device("button1", 1, type = "button"),
        Device("button2", 2, type = "button"), Device("button3", 3, type = "button"),
        Device("t1", 5, type = "motor"), Device("t2", 6, type = "motor"),
        Device("t3", 7, type = "motor"))
    val sensorData = mapOf("sensor_A0" to 5.32f, "sensor_A1" to 4.22f)
    // ==================================== TODO
    LaunchedEffect(devices) {
        viewModel.subscribeAllSensors(devices)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.unsubscribeAllSensors(devices)
        }
    }

    // Получаем текущую ориентацию экрана (книжная или альбомная)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutConfig = remember(isLandscape) {
        if(isLandscape) ControllerScreenLayoutParams.Landscape else ControllerScreenLayoutParams.Portrait
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFC6C5C9))) {
        Box(modifier = layoutConfig.parentBoxModifier) {
            TopBar(
                modifier = layoutConfig.topBarModifier.align(layoutConfig.topBarAlignment),
                boardName = if(boardInfo.first.length < 2) "Unknown" else "${boardInfo.first} (${boardInfo.second})",
                wsState = wsState,
                onSettingsClick = onOpenPinEditor,
                onCameraClick = { showCamera = !showCamera }
            )
            // todo под горизонтальную ориентацию нужен вообще другой компонент
            // Для альбомной мы либо делаем "таблицу" и в центральной ячейке камера, либо зададим
            // в процентах ширину и высоту также вычислим
            // Под горизонталку нужен свой компонент сенсор и кнопка (более простые прямоугольные)
            Column(layoutConfig.parentColumnModifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
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
                    val dev = devices.filter { it.type != "sensor" && it.type != "motor" }
                    val weight2 = remember(dev.size) {
                        when {
                            dev.size in 0..2 -> 1f
                            dev.size % 3 == 0 -> dev.size / 3f
                            else -> (dev.size / 3 + 1).toFloat()
                        }
                    }
                    Sensors(
                        modifier = Modifier.weight(weight1),
                        rowCount = weight1.toInt(),
                        data = sensorData
                    )
                    CustomButtons(
                        modifier = Modifier.weight(weight2),
                        rowCount = weight2.toInt(),
                        devices = dev,
                        onDeviceToggle = { d, on -> viewModel.toggleDevice(d, on) }
                    )
                }
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
                val w = remember(showCamera) {
                    if(showCamera) 1f else 0.7f
                }
                Box(modifier = Modifier.weight(w).fillMaxWidth(),
                    contentAlignment = Alignment.Center) {
                    val motors = remember(devices) {
                        devices.filter { it.type == "motor" }
                    }
                    when(motors.size) {
                        in 0..2 -> {
                            // todo можно это попробовать
                            //BoxWithConstraints {
                            //    val size = min(maxWidth, maxHeight) * 0.8f
                            //    Joystick(size = size)
                            //}
                            Joystick(
                                modifier = layoutConfig.joystickModifier.align(layoutConfig.joystickAlignment),
                                size = layoutConfig.joystickSize,
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
                                            viewModel.setMotorSpeed(motor, leftPwmSigned)
                                        }
                                        else -> {
                                            // два мотора - используем первые два как лев/право
                                            val leftMotor = motors[0]
                                            val rightMotor = motors[1]
                                            viewModel.setMotorSpeed(leftMotor, leftPwmSigned)
                                            viewModel.setMotorSpeed(rightMotor, rightPwmSigned)
                                        }
                                    }
                                }
                            )
                        }
                        else -> {
                            // три или четыре мотора
                            MotorControl(
                                modifier = layoutConfig.motorControlModifier.align(layoutConfig.motorControlAlignment),
                                motors = motors,
                                onCommand = { motorName, motorSpeed ->
                                    val motor = motors.find { it.name == motorName }
                                    if (motor != null) {
                                        viewModel.setMotorSpeed(motor, motorSpeed)
                                    } else {
                                        // Сюда код не должен попасть, но это хорошая проверка
                                        Log.e("MotorControl", "Ошибка: мотор с именем $motorName не найден!")
                                    }
                                }
                            )
                        }
                    }
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
            when(wsState) {
                WsState.CONNECTED -> Text(text = "Подключено", color = Color(0xFF4CAF50), fontSize = 16.sp)
                WsState.CONNECTING -> AnimatedConnectingText()
                WsState.CLOSED -> Text(text = "Отключено", color = Color.LightGray, fontSize = 16.sp)
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
                        key(device) {
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
fun CustomButton(width: Dp, height: Dp, isQuad: Boolean, device: Device, onDeviceToggle: (Device, Boolean) -> Unit) {
    var isOn by remember { mutableStateOf(device.state) }
    val avgSizeForWidth = (width + height) / 2
    val avgSize = (width + height + height) / 3
    val rcsSize = avgSize / 6
    val fraction = if(isQuad) 0.9f else 1f
    val fs = (avgSize / 7f).value
    val imageWeight = if(isQuad) 3f else 2.5f
    Box(
        modifier = Modifier
            .width(avgSizeForWidth)
            .height(height)
            .fillMaxSize()
            .background(if (isOn) Color(0xFF81C784) else Color(0xFFF0F0F0),
                RoundedCornerShape(rcsSize))
            .clickable(onClick = {
                isOn = !isOn
                onDeviceToggle(device, isOn)
            }),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(fraction = fraction).fillMaxWidth()
        ) {
            Image(painter = painterResource(R.drawable.ic_bulb), contentDescription = "IconButton", modifier = Modifier.weight(imageWeight).fillMaxWidth())
            Text(text = device.name, color = Color.Black, fontSize = fs.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = fixedTextStyle)
        }
    }
}

@Composable
fun AnimatedConnectingText(
    modifier: Modifier = Modifier,
    baseText: String = "Подключение",
    fontSize: TextUnit = 16.sp
) {
    var dotCount by remember { mutableIntStateOf(1) }

    // Этот эффект будет запускаться один раз и работать, пока компонент на экране
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // Пауза в полсекунды
            dotCount = (dotCount % 3) + 1 // Циклично меняем количество точек: 1 -> 2 -> 3 -> 1
        }
    }

    Text(
        text = "$baseText${".".repeat(dotCount)}",
        modifier = modifier,
        fontSize = fontSize
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
@Preview
fun TestControllerScreen() {
    ControllerScreen(onOpenPinEditor = {})
}