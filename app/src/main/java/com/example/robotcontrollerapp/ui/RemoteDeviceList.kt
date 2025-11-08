package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.robotcontrollerapp.R
import com.example.robotcontrollerapp.domain.Device

@Composable
fun RemoteDeviceList(
    devices: List<Device>,
    onDelete: (Device) -> Unit
) {
    if (devices.isEmpty()) {
        Text("Удалённые устройства не настроены. Нажмите '+' для добавления.")
    } else {
        LazyColumn {
            items(devices, key = { it.name }) { device ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(device.name) },
                        leadingContent = {
                            Icon(
                                painter = when (device.type) {
                                    "motor" -> painterResource(R.drawable.ic_engine)
                                    "led" -> painterResource(R.drawable.ic_bulb)
                                    "sensor" -> painterResource(R.drawable.ic_sensor)
                                    else -> painterResource(R.drawable.ic_help)
                                },
                                contentDescription = device.type
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onDelete(device) }) {
                                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Удалить")
                            }
                        }
                    )
                }
            }
        }
    }
}