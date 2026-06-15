package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.robotcontrollerapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDeviceConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String) -> Unit
) {
    val deviceTypes = remember {
        listOf(
            DeviceTypeInfo("led", "Светодиод", R.drawable.ic_bulb),
            DeviceTypeInfo("motor", "Мотор", R.drawable.ic_engine),
            DeviceTypeInfo("servo", "Сервопривод", R.drawable.ic_servo),
            DeviceTypeInfo("sensor", "Сенсор", R.drawable.ic_sensor)
        )
    }

    // --- Локальное состояние для полей внутри диалога ---
    var selectedType by remember { mutableStateOf(deviceTypes.first()) }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настроить удалённое устройство") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя устройства") },
                    placeholder = {
                        val txt = when(selectedType.id) {
                            "led" -> "uno_led13"
                            "motor" -> "uno_motor_9_8"
                            "servo" -> "uno_servo_6_180"
                            "sensor" -> "uno_sensor_A0"
                            else -> "uno_led13"
                        }
                        Text(txt)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Селектор типа устройства
                DeviceTypeSelector(
                    types = deviceTypes,
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                Text(
                    "Имя должно содержать пины, которыми будет управлять Uno.\nПримеры:\nuno_led13\nuno_motor_3_2\nuno_servo_6_180 (градусы)\nuno_sensor_A0",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedType.id) },
                enabled = name.isNotBlank() // Кнопка активна, только если введено имя
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}