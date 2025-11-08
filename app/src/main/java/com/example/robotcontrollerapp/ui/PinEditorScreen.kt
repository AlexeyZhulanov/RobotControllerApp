package com.example.robotcontrollerapp.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.robotcontrollerapp.R
import com.example.robotcontrollerapp.domain.Device
import kotlinx.coroutines.launch

private enum class ConfigTab { Local, Remote }

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

    var savedAssigned by remember { mutableStateOf<Map<Int, Device>>(emptyMap()) }

    var selectedTab by remember { mutableStateOf(ConfigTab.Local) }
    var selectedPin by remember { mutableStateOf<Int?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showUnbindDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showRemoteDeviceDialog by remember { mutableStateOf(false) }
    var onSaveClicked by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.errorFlow.collect { errorMessage ->
            // Когда из ViewModel прилетает ошибка, мы тут же её показываем
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

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

    // Slave-устройства - те, у которых нет локального пина
    val remoteDevices = remember(devices) {
        val d = devices.toSet().filter { it.pin == -1 }
        Log.d("testRemote", d.toString())
        d
    }

    val fabPosition = remember(remoteDevices.size) {
        if(remoteDevices.size >= 5) FabPosition.Center else FabPosition.End
    }

    val hasUnsavedChanges = remember(assigned, onSaveClicked) {
        if(savedAssigned.isEmpty()) {
            savedAssigned = assigned
            false
        } else if(onSaveClicked) {
            onSaveClicked = false
            savedAssigned = emptyMap()
            false
        } else true
    }

    // Он будет активен, только если есть несохраненные изменения
    BackHandler(enabled = hasUnsavedChanges) {
        // Вместо выхода, показываем диалог
        showUnsavedDialog = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding(),
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text("🔧 $boardName") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if(hasUnsavedChanges) showUnsavedDialog = true else onBack()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Назад"
                            )
                        }
                    },
                    actions = {
                        // 1. Кнопка "Сохранить" в виде иконки
                        IconButton(
                            onClick = {
                                viewModel.saveConfig(devices)
                                onSaveClicked = true
                            },
                            enabled = hasUnsavedChanges
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_done),
                                contentDescription = "Сохранить",
                                tint = if(hasUnsavedChanges) Color.Green else LocalContentColor.current
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

                PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(
                        selected = selectedTab == ConfigTab.Local,
                        onClick = { selectedTab = ConfigTab.Local },
                        text = { Text("Локальные (ESP)") }
                    )
                    Tab(
                        selected = selectedTab == ConfigTab.Remote,
                        onClick = { selectedTab = ConfigTab.Remote },
                        text = { Text("Удалённые (Uno)") }
                    )
                }
            }
        },
        floatingActionButton = {
            // Показываем её только на вкладке Slave-устройств
            if (selectedTab == ConfigTab.Remote) {
                FloatingActionButton(onClick = { showRemoteDeviceDialog = true }) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = "Добавить удалённое устройство")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButtonPosition = fabPosition
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 4.dp)) {
            when (selectedTab) {
                ConfigTab.Local -> {
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
                }
                ConfigTab.Remote -> {
                    RemoteDeviceList(
                        devices = remoteDevices,
                        onDelete = { deviceToDelete ->
                            // Логика удаления из 'assigned'
                            // Нам нужно найти ключ (фиктивный пин), по которому лежит это устройство
                            val device = devices.find { it.name == deviceToDelete.name }
                            if (device != null) {
                                viewModel.onDeviceRemoved(device)
                            }
                        }
                    )
                }
            }
            // Общие диалоги
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
            if (showUnsavedDialog) {
                AlertDialog(
                    onDismissRequest = { showUnsavedDialog = false },
                    title = { Text("Выйти без сохранения?") },
                    text = { Text("У вас есть несохраненные изменения. Вы уверены, что хотите выйти?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showUnsavedDialog = false
                                onBack() // Выполняем реальный выход
                            }
                        ) {
                            Text("Да, выйти")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showUnsavedDialog = false } // Просто закрываем диалог
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }
            if (showRemoteDeviceDialog) {
                RemoteDeviceConfigDialog(
                    onDismiss = { showRemoteDeviceDialog = false },
                    onConfirm = { name, type ->
                        showRemoteDeviceDialog = false

                        // Создаем новое устройство с фиктивным пином
                        val newDevice = Device(name = name, pin = -1, pin2 = null, type = type)

                        viewModel.onDeviceSelected(newDevice)
                    }
                )
            }
        }
    }
}