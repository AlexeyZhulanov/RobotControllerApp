package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Composable
fun NameInputDialog(
    defaultName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onUnbind: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                if (onUnbind != null) {
                    TextButton(onClick = onUnbind) {
                        Text("Отвязать", color = Color.Red)
                    }
                }
                TextButton(onClick = { onConfirm(name) }) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = {
            Text(if (onUnbind != null) "Редактировать или отвязать" else "Имя устройства")
        },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Введите имя") }
            )
        }
    )
}

