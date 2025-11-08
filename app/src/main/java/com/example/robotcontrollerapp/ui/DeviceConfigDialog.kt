package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.robotcontrollerapp.R
import com.example.robotcontrollerapp.util.pinAliases

data class DeviceTypeInfo(
    val id: String,
    val displayName: String,
    val drawableId: Int
)

@Composable
fun DeviceConfigDialog(
    pin: Int,
    assignedPins: Set<Int>, // Множество уже занятых пинов для фильтрации
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, pin2: Int?) -> Unit
) {
    // --- Данные для селекторов ---
    val deviceTypes = remember {
        listOf(
            DeviceTypeInfo("led", "Светодиод", R.drawable.ic_bulb),
            DeviceTypeInfo("motor", "Мотор", R.drawable.ic_engine),
            DeviceTypeInfo("sensor", "Сенсор", R.drawable.ic_sensor)
        )
    }

    // --- Локальное состояние для полей внутри диалога ---
    var selectedType by remember { mutableStateOf(deviceTypes.first()) }
    var name by remember { mutableStateOf("${selectedType.id}_$pin") }
    var selectedPin2 by remember { mutableStateOf<Int?>(null) }

    // Фильтруем список доступных пинов для второго селектора (направления мотора)
    val availablePinsForPin2 = remember(assignedPins) {
        val allBlockedPins = assignedPins.flatMap { assignedPin ->
            // Для каждого занятого пина берем его самого и его псевдоним (если он есть)
            setOfNotNull(assignedPin, pinAliases[assignedPin])
        }.toSet()
        (0..15).filter { it != pin && it != pinAliases[pin] && it !in allBlockedPins }
    }

    // --- Логика для автоматического обновления ---
    LaunchedEffect(selectedType) {
        // Обновляем имя по умолчанию при смене типа
        name = "${selectedType.id}_$pin"
        // Сбрасываем второй пин, если тип не мотор
        if (selectedType.id != "motor") {
            selectedPin2 = null
        }
    }

    val isConfirmEnabled = remember(selectedType, selectedPin2) {
        // Кнопка "Сохранить" активна, только если для мотора выбран второй пин
        selectedType.id != "motor" || selectedPin2 != null
    }

    // --- Отрисовка самого диалога ---
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(selectedType.drawableId), contentDescription = null, modifier = Modifier.size(32.dp)) },
        title = { Text("Настройка пина D$pin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. Поле для ввода имени
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя устройства") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Селектор типа устройства
                DeviceTypeSelector(
                    types = deviceTypes,
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                // 3. Селектор для второго пина
                if (selectedType.id == "motor") {
                    PinSelector(
                        label = "Пин направления",
                        pins = availablePinsForPin2,
                        selectedPin = selectedPin2,
                        onPinSelected = { selectedPin2 = it }
                    )
                }
            }
        },
        confirmButton = {
            val sp2 = selectedPin2?.let { if(it > 10) pinAliases[selectedPin2] else selectedPin2 }
            Button(
                onClick = { onConfirm(name, selectedType.id, sp2) },
                enabled = isConfirmEnabled
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceTypeSelector(
    types: List<DeviceTypeInfo>,
    selectedType: DeviceTypeInfo,
    onTypeSelected: (DeviceTypeInfo) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Тип устройства") },
            leadingIcon = { Icon(painterResource(selectedType.drawableId), contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    leadingIcon = { Icon(painterResource(type.drawableId), contentDescription = null) },
                    onClick = {
                        onTypeSelected(type)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

// --- Селектор для пинов ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSelector(
    label: String,
    pins: List<Int>,
    selectedPin: Int?,
    onPinSelected: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField(
            value = selectedPin?.let { "D$it" } ?: "Не выбран",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            pins.forEach { pin ->
                DropdownMenuItem(
                    text = { Text("D$pin") },
                    onClick = {
                        onPinSelected(pin)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
@Preview
fun TestConfigDialog() {
    DeviceConfigDialog(2, setOf(1, 3, 5, 6, 7, 8), {}, {_, _, _ -> {}})
}