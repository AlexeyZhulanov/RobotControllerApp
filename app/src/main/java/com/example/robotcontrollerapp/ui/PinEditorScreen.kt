package com.example.robotcontrollerapp.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.robotcontrollerapp.domain.Device
import kotlin.math.hypot

@Composable
fun PinEditorScreen(
    viewModel: PinEditorViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    val boardName by viewModel.boardName.collectAsState()

    var assigned by remember { mutableStateOf(devices.associateBy { it.pin }.toMutableMap()) }

    var selectedPin by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // drag state for the palette item
    var draggingItem by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    var boardSize by remember { mutableStateOf(IntSize.Zero) }

    val pinPositions = remember { mutableStateMapOf<Int, Offset>() }

    Box(Modifier.fillMaxSize().navigationBarsPadding().statusBarsPadding().padding(4.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔧 $boardName", style = MaterialTheme.typography.headlineSmall, fontSize = 22.sp)
                Spacer(Modifier.size(12.dp))
                Button(onClick = { viewModel.saveConfig(assigned.values.toList()) }, modifier = Modifier.weight(1f)) {
                    Text("Сохранить")
                }
            }

            Spacer(Modifier.height(5.dp))
            Text("Назначенные устройства", color = Color.Gray)
            Spacer(Modifier.height(3.dp))
            FlowRow(
                maxLines = 4,
                maxItemsInEachRow = 3,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                assigned.values.forEach { d ->
                    Text("${d.name} → D${d.pin}", color = Color.LightGray)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            Box(
                Modifier
                    .background(Color(0xFF111111), shape = MaterialTheme.shapes.medium)
                    .onSizeChanged { boardSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            dragOffset = change.position
                        }
                    }
            ) {
                WemosD1MiniBoard(
                    modifier = Modifier.fillMaxHeight(fraction = 0.7f),
                    boardStyle = BoardStyle(pinSize = 24.dp, pinOverlap = 10.dp),
                    assignedPins = assigned.keys,
                    onPinClicked = { pin ->
                        Log.d("testPin", "CLICKED: ${pin.number}")
                        selectedPin = pin.number
                        showDialog = true
                    },
                    onPinPositionChanged = { pin, center ->
                        pinPositions[pin] = center
                    }
                )

                // drag preview: if dragging an item, draw it under finger
                draggingItem?.let { item ->
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    (dragOffset.x - 20.dp.toPx()).toInt(), // todo проверить
                                    (dragOffset.y - 20.dp.toPx()).toInt()
                                )
                            }
                            .size(40.dp)
                            .background(Color(0xFFFFC107), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.take(1).uppercase())
                    }
                }

            }
            Spacer(Modifier.height(4.dp))
            // Palette (перетаскиваемые устройства)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                val items = listOf("led", "motor", "sensor")
                items.forEach { t ->
                    Card(modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .weight(1f)
                        .pointerInput(t) { // todo gpt шизил здесь, нужно тщательно проверить
                            detectDragGestures(
                                onDragStart = { offset ->
                                    draggingItem = t
                                    dragOffset = offset
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                },
                                onDragEnd = {
                                    val nearest = findNearestPinFromMap(dragOffset, pinPositions, boardSize)
                                    if (nearest != null) {
                                        assigned = assigned.toMutableMap().apply {
                                            put(
                                                nearest.first,
                                                Device("${t}_${nearest.first}", nearest.first, t)
                                            )
                                        }
                                    }
                                    draggingItem = null
                                },
                                onDragCancel = { draggingItem = null }
                            )
                        }
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
                            Text(t.uppercase())
                        }
                    }
                }
            }
        }
        if (showDialog && selectedPin != null) {
            DeviceTypeDialog(pin = selectedPin!!, onSelect = { type ->
                assigned = assigned.toMutableMap().apply {
                    put(selectedPin!!, Device(name = "${type}_${selectedPin}", pin = selectedPin!!, type = type))
                }
                showDialog = false
            }, onCancel = { showDialog = false })
        }
    }
}

fun findNearestPinFromMap(
    pointerPos: Offset,
    pinPositions: Map<Int, Offset>,
    boardSize: IntSize
): Pair<Int, Float>? {
    var best: Pair<Int, Float>? = null
    for ((pin, pos) in pinPositions) {
        val dist = hypot(pos.x - pointerPos.x, pos.y - pointerPos.y)
        if (best == null || dist < best.second) best = pin to dist
    }
    val threshold = (boardSize.width * 0.15f).coerceAtLeast(60f)
    return if (best != null && best.second < threshold) best else null
}