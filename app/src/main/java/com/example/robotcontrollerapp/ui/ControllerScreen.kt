package com.example.robotcontrollerapp.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import kotlinx.coroutines.delay
import kotlin.collections.chunked
import kotlin.collections.forEach

@Composable
fun ControllerScreen(
    viewModel: RobotControlViewModel = hiltViewModel(),
    onOpenPinEditor: () -> Unit
) {
    val wsState by viewModel.wsState.collectAsState()
    val boardInfo by viewModel.boardInfo.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()
    var showLogs by remember { mutableStateOf(false) }
    val logFlow = viewModel.logs
    var logList by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(devices) {
        viewModel.subscribeAllSensors(devices)
    }

    LaunchedEffect(logFlow) {
        logFlow.collect { msg ->
            logList = (logList + msg).takeLast(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.unsubscribeAllSensors(devices)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFC6C5C9))) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val width = maxWidth
            TopBar(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                boardName = boardInfo,
                wsState = wsState,
                onSettingsClick = onOpenPinEditor,
                onLogsClick = { showLogs = true }
            )

            Column(Modifier.padding(top = 80.dp, bottom = 270.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Sensors(
                    modifier = Modifier.weight(1f),
                    width = width,
                    data = sensorData
                )
                CustomButtons(
                    modifier = Modifier.weight(1f),
                    width = width,
                    devices = devices.filter { it.type != "sensor" && it.type != "motor" },
                    onDeviceToggle = { d, on -> viewModel.toggleDevice(d, on) }
                )
            }
            val motors = remember(devices) {
                devices.filter { it.type == "motor" }
            }
            when(motors.size) {
                in 0..2 -> {
                    Joystick(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                        size = 200.dp,
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
                    // три мотора
                    MotorControl(
                        modifier = Modifier.height(270.dp).align(Alignment.BottomCenter),
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
            if (showLogs) {
                LogsDialog(logs = logList, onDismiss = { showLogs = false })
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
    onLogsClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_chat),
            contentDescription = "BackArrow",
            modifier = Modifier
                .size(45.dp)
                .background(Color.LightGray, RoundedCornerShape(8.dp))
                .padding(4.dp)
                .clickable(onClick = { onLogsClick() })
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
fun Sensors(modifier: Modifier, width: Dp, data: Map<String, Float>) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val count = data.size
        val sCount = maxOf(2, count)
        val widthForEach = maxOf(width / (sCount + 1), width / 6)
        val chunkedMaps = data.toList()
            .chunked(2)
            .map { it.toMap() }

        chunkedMaps.forEach { rowMaps ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                rowMaps.forEach { (key, value) ->
                    key(key) {
                        Sensor(
                            size = widthForEach,
                            name = key,
                            value = value
                        )
                    }
                }
            }
        }
    }
//    FlowRow(
//        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
//        horizontalArrangement = Arrangement.SpaceAround,
//        verticalArrangement = Arrangement.spacedBy(10.dp),
//        maxItemsInEachRow = 5
//    ) {
//        val count = data.size
//        val sCount = maxOf(2, count)
//        val widthForEach = maxOf(width / (sCount + 1), width / 6)
//        data.forEach { (key, value) ->
//            key(key) {
//                Sensor(
//                    size = widthForEach,
//                    name = key,
//                    value = value
//                )
//            }
//        }
//    }
}

@Composable
fun CustomButtons(modifier: Modifier, width: Dp, devices: List<Device>, onDeviceToggle: (Device, Boolean) -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        val chunked = devices.chunked(3)
        val count = devices.size
        val sCount = maxOf(2, count)
        val widthForEach = maxOf(width / (sCount + 1), width / 4)

        chunked.forEach { rowDevices ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                rowDevices.forEach { device ->
                    key(device) {
                        CustomButton(
                            size = widthForEach,
                            device = device,
                            onDeviceToggle = { d, on -> onDeviceToggle(d, on) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Sensor(size: Dp, name: String, value: Float) {
    val finalName = if(name.length > 9) name.replace("_sensor", "") else name
    Box(modifier = Modifier.size(size).background(Color.White, RoundedCornerShape(16.dp)), contentAlignment = Alignment.CenterStart) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight(fraction = 0.8f).padding(start = 8.dp)) {
            val fs1 = (size / 5.7f).value
            val fs2 = (size / 3.4f).value
            Text(text = finalName, color = Color.Gray, fontSize = fs1.sp)
            Text(text = value.toString(), color = Color.Black, fontSize = fs2.sp)
        }
    }
}

@Composable
fun CustomButton(size: Dp, device: Device, onDeviceToggle: (Device, Boolean) -> Unit) {
    var isOn by remember { mutableStateOf(device.state) }
    Box(
        modifier = Modifier
            .size(size)
            .fillMaxSize()
            .background(if (isOn) Color(0xFF81C784) else Color(0xFFF0F0F0),
                RoundedCornerShape(16.dp))
            .clickable(onClick = {
                isOn = !isOn
                onDeviceToggle(device, isOn)
            }),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(fraction = 0.9f).fillMaxWidth()
        ) {
            val fs = (size / 7).value
            Image(painter = painterResource(R.drawable.ic_bulb), contentDescription = "IconButton", modifier = Modifier.weight(3f).fillMaxWidth())
            Text(text = device.name, color = Color.Black, fontSize = fs.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
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

@Composable
@Preview
fun TestControllerScreen() {
    ControllerScreen(onOpenPinEditor = {})
}