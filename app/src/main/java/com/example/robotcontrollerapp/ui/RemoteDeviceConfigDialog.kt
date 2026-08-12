package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.robotcontrollerapp.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDeviceConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, direction: String, critDist: Int, warnDist: Int, safeSpeed: Int) -> Unit
) {
    val deviceTypes = remember {
        listOf(
            DeviceTypeInfo("led", "Светодиод", R.drawable.ic_bulb),
            DeviceTypeInfo("motor", "Мотор", R.drawable.ic_engine),
            DeviceTypeInfo("servo", "Сервопривод", R.drawable.ic_servo),
            DeviceTypeInfo("sensor", "Сенсор", R.drawable.ic_sensor),
            DeviceTypeInfo("sonar", "Сонар", R.drawable.ic_parking)
        )
    }

    // --- Локальное состояние для полей внутри диалога ---
    var selectedType by remember { mutableStateOf(deviceTypes.first()) }
    var name by remember { mutableStateOf("") }
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf("none") }
    var selectedCritDist by remember { mutableIntStateOf(5) }
    var selectedWarnDist by remember { mutableIntStateOf(20) }
    var selectedSafeSpeed by remember { mutableIntStateOf(100) }
    val currentBoard = "uno"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настроить удаленное устройство") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Имя устройства: $name",
                    style = MaterialTheme.typography.labelMedium
                )

                // Селектор типа устройства
                DeviceTypeSelector(
                    types = deviceTypes,
                    selectedType = selectedType,
                    onTypeSelected = {
                        selectedType = it
                        name = ""
                        field1 = ""
                        field2 = ""
                        selectedDirection = "none"
                        selectedCritDist = 5
                        selectedWarnDist = 20
                        selectedSafeSpeed = 100
                    }
                )

                when(selectedType.id) {
                    "led" -> {
                        OutlinedTextField(
                            value = field1,
                            onValueChange = {
                                if((it.matches(Regex("[0-9]+")) || it.isBlank()) && it.length < 5) {
                                    field1 = it
                                    name = "${currentBoard}_led_$it"
                                }
                            },
                            label = { Text("Led pin") },
                            placeholder = { Text("13", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Led pin = куда сейчас воткнут led на Uno плате, например 13",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    "motor" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = field1,
                                onValueChange = {
                                    if((it.matches(Regex("[0-9]+")) || it.isBlank()) && it.length < 5) {
                                        field1 = it
                                        name = "${currentBoard}_motor_${it}_$field2"
                                    }
                                },
                                label = { Text("Speed pin") },
                                placeholder = { Text("3", color = Color.Gray) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = {
                                    if((it.matches(Regex("[0-9]+")) || it.isBlank()) && it.length < 5) {
                                        field2 = it
                                        name = "${currentBoard}_motor_${field1}_$it"
                                    }
                                },
                                label = { Text("Direction pin") },
                                placeholder = { Text("2", color = Color.Gray) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        DirectionSelector(selectedDirection, listOf("left", "none", "right")) { dir ->
                            selectedDirection = dir
                        }
                        Text(
                            "Speed pin = скоростной пин мотора на Uno, например 3\nDirection pin = направляющий пин мотора на Uno, например 2\nЕсли мотора всего два, то нужно указывать их расположение Left/Right",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    "servo" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = field1,
                                onValueChange = {
                                    if((it.matches(Regex("[0-9]+")) || it.isBlank()) && it.length < 5) {
                                        field1 = it
                                        name = "${currentBoard}_servo_${it}_$field2"
                                    }
                                },
                                label = { Text("Servo pin") },
                                placeholder = { Text("6", color = Color.Gray) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = {
                                    if((it.matches(Regex("[0-9]+")) || it.isBlank()) && it.length < 5) {
                                        field2 = it
                                        name = "${currentBoard}_servo_${field1}_$it"
                                    }
                                },
                                label = { Text("Servo angle") },
                                placeholder = { Text("180", color = Color.Gray) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            "Servo pin = куда сейчас воткнут сервопривод на Uno, например 6\nВо втором поле Servo angle необходимо указать градус поворота сервопривода (чаще всего это 180, но бывает и 360)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    "sensor" -> {
                        OutlinedTextField(
                            value = field1,
                            onValueChange = {
                                if((it.matches(Regex("[0-9A]+")) || it.isBlank()) && it.length < 5) {
                                    field1 = it
                                    name = "${currentBoard}_sensor_$it"
                                }
                            },
                            label = { Text("Sensor pin") },
                            placeholder = { Text("A0", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Sensor pin = куда сейчас воткнут сенсор на Uno, например A0",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    "sonar" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = field1,
                                onValueChange = {
                                    if((it.matches(Regex("[0-9A]+")) || it.isBlank()) && it.length < 5) {
                                        field1 = it
                                        name = "${currentBoard}_sonar_${it}_$field2"
                                    }
                                },
                                label = { Text("Trig pin") },
                                placeholder = { Text("9", color = Color.Gray) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = {
                                    if((it.matches(Regex("[0-9A]+")) || it.isBlank()) && it.length < 5) {
                                        field2 = it
                                        name = "${currentBoard}_sonar_${field1}_$it"
                                    }
                                },
                                label = { Text("Echo pin") },
                                placeholder = { Text("10", color = Color.Gray) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        DirectionSelector(selectedDirection, listOf("front", "none", "rear")) { dir ->
                            selectedDirection = dir
                        }
                        SettingSlider(
                            label = "Критическая (Стоп)",
                            value = selectedCritDist,
                            valueRange = 2f..30f,
                            unit = "см",
                            onValueChange = {
                                selectedCritDist = it
                                // Не даем Warning быть меньше Critical
                                if (selectedWarnDist <= it) selectedWarnDist = it + 5
                            }
                        )
                        SettingSlider(
                            label = "Предупреждение (Лимит)",
                            value = selectedWarnDist,
                            valueRange = 5f..100f,
                            unit = "см",
                            onValueChange = {
                                // Не даем Warning опуститься ниже Critical
                                if (it > selectedCritDist) selectedWarnDist = it
                            }
                        )
                        SettingSlider(
                            label = "Безопасная скорость",
                            value = selectedSafeSpeed,
                            valueRange = 0f..255f,
                            onValueChange = { selectedSafeSpeed = it }
                        )
                        Text(
                            "Trig pin = Trig-пин сонара на Uno, например 9\nEcho pin = Echo-пин сонара на Uno, например 10\nНеобходимо указать расположение сонара Front=Спереди, Rear=Сзади",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, selectedType.id, selectedDirection, selectedCritDist, selectedWarnDist, selectedSafeSpeed)
                },
                enabled = when(selectedType.id) {
                    "led" -> field1.isNotBlank()
                    "motor" -> field1.isNotBlank() && field2.isNotBlank() && selectedDirection in listOf("none", "left", "right")
                    "servo" -> field1.isNotBlank() && field2.isNotBlank()
                    "sensor" -> field1.isNotBlank()
                    "sonar" -> field1.isNotBlank() && field2.isNotBlank() && (selectedDirection == "front" || selectedDirection == "rear")
                    else -> false
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun DirectionSelector(dir: String, options: List<String>, onSelectedDirection: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { direction ->
            val isSelected = dir == direction
            Button(
                onClick = { onSelectedDirection(direction) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                // Делаем первую букву заглавной для красоты
                Text(text = direction.replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String = "",
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange
        )
    }
}