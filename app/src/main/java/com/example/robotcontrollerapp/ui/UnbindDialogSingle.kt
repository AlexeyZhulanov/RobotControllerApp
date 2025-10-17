package com.example.robotcontrollerapp.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.robotcontrollerapp.domain.Device

@Composable
fun UnbindDialogSingle(
    pin: Int,
    device: Device,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Отвязать") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Отмена") }
        },
        title = { Text("Отвязать устройство") },
        text = { Text("Вы уверены, что хотите удалить ${device.name} с D$pin?") }
    )
}
