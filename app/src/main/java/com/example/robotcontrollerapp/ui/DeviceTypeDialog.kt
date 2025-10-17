package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceTypeDialog(pin: Int, onSelect: (String) -> Unit, onCancel: () -> Unit) {
    val types = listOf("led", "motor", "sensor", "custom")
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = onCancel) { Text("Закрыть") }
        },
        title = { Text("Выберите тип для D$pin") },
        text = {
            Column {
                types.forEach { t ->
                    Button(
                        onClick = { onSelect(t) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(t.uppercase())
                    }
                }
            }
        }
    )
}