package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.robotcontrollerapp.R
import com.example.robotcontrollerapp.domain.Device

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinEditorScreen(
    viewModel: PinEditorViewModel = hiltViewModel(),
    onBack: () -> Unit // todo не используется
) {
    val devices by viewModel.devices.collectAsState()
    val boardName by viewModel.boardName.collectAsState()
    val detectedPins by viewModel.detectedPins.collectAsState()

    var assigned by remember { mutableStateOf(mutableMapOf<Int, Device>()) }

    var selectedPin by remember { mutableStateOf<Int?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showUnbindDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(devices) {
        assigned = buildMap {
            devices.forEach { device ->
                put(device.pin, device)
                // Если есть второй пин (pin2), добавляем и его.
                // Он будет ссылаться на то же самое устройство.
                device.pin2?.let { secondPin ->
                    put(secondPin, device)
                }
            }
        }.toMutableMap()
    }

    val unassignedDetected = detectedPins.filterNot { detected ->
        assigned.keys.contains(detected.pin)
    }.map { it.pin }.toSet()

    Box(Modifier.fillMaxSize().navigationBarsPadding().statusBarsPadding().padding(horizontal = 4.dp)) {
        TopAppBar(
            title = { Text("🔧 $boardName") },
            navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Назад"
                    )
                }
            },
            actions = {
                // 1. Кнопка "Сохранить" в виде иконки
                IconButton(onClick = { viewModel.saveConfig(devices) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_done),
                        contentDescription = "Сохранить"
                    )
                }

                // 2. Меню "три точки" для второстепенных действий
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more),
                            contentDescription = "Дополнительные действия"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Отвязать пин") },
                            onClick = {
                                showMenu = false
                                showUnbindDialog = true
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent // Чтобы не было фона
            )
        )
        Column(Modifier.align(Alignment.BottomCenter)) {
            Box(Modifier.background(Color(0xFF111111), shape = MaterialTheme.shapes.medium)) {
                WemosD1MiniBoard(
                    modifier = Modifier.fillMaxHeight(fraction = 0.8f),
                    boardStyle = BoardStyle(pinSize = 24.dp, pinOverlap = 10.dp),
                    devices = devices,
                    detectedPins = unassignedDetected,
                    onPinClicked = { pin ->
                        selectedPin = pin.number
                        showConfigDialog = true
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                val items = listOf("led", "motor", "sensor")
                items.forEach { t ->
                    Card(modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .weight(1f)
                    ) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(22.dp).background(
                                color = when (t) {
                                    "led" -> Color.Yellow
                                    "motor" -> Color.Green
                                    "sensor" -> Color.Cyan
                                    else -> Color.Gray
                                }, shape = CircleShape
                            ))
                            Spacer(Modifier.width(8.dp))
                            Text(t.uppercase(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        val sPin = selectedPin
        if (showConfigDialog && sPin != null) {
            val alreadyAssigned = assigned.containsKey(sPin)
            if (alreadyAssigned) {
                UnbindDialogSingle(
                    pin = sPin,
                    device = assigned[sPin] ?: Device("", -1, type = ""),
                    onConfirm = {
                        val value = assigned[sPin] // Для того, чтобы сразу два пина у мотора удалить
                        value?.let { viewModel.onDeviceRemoved(it) }
                        showConfigDialog = false
                    },
                    onCancel = { showConfigDialog = false }
                )
            } else {
                DeviceConfigDialog(
                    pin = sPin,
                    assignedPins = assigned.keys,
                    onDismiss = { showConfigDialog = false },
                    onConfirm = { name, type, pin2 ->
                        val newDevice = Device(name = name, pin = sPin, pin2 = pin2, type = type)
                        viewModel.onDeviceSelected(newDevice)
                        showConfigDialog = false
                    }
                )
            }
        }
        if (showUnbindDialog) {
            UnbindDialog(
                devices = devices,
                onUnbind = { device ->
                    viewModel.onDeviceRemoved(device)
                    showUnbindDialog = false
                },
                onDismiss = { showUnbindDialog = false }
            )
        }
    }
}