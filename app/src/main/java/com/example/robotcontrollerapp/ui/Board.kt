package com.example.robotcontrollerapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.toSize

data class BoardPin(
    val name: String,
    val number: Int? = null
)

data class BoardStyle(
    val boardColor: Color = Color(0xFF0D47A1),
    val innerBoardColor: Color = Color(0xFF1A237E),
    val pinColor: Color = Color(0xFF9E9E9E),
    val textColor: Color = Color.White,
    val cornerRadius: Dp = 40.dp,
    val pinSize: Dp = 36.dp,
    val pinOverlap: Dp = 8.dp // Насколько пины выступают за контур
)

// Альтернативный вариант с абсолютным позиционированием правых пинов
@Composable
fun UniversalBoardFinalV2(
    modifier: Modifier = Modifier,
    boardName: String = "Wemos D1 mini",
    leftPins: List<BoardPin> = emptyList(),
    rightPins: List<BoardPin> = emptyList(),
    boardStyle: BoardStyle = BoardStyle(),
    showLabels: Boolean = true,
    assignedPins: Set<Int> = emptySet(),
    onPinClicked: (BoardPin) -> Unit,
    onPinPositionChanged: (pinNumber: Int, center: Offset) -> Unit = { _, _ -> }
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(boardStyle.cornerRadius))
            .background(boardStyle.boardColor)
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 0.dp)
    ) {
        // Название платы
        if (showLabels) {
            Text(
                text = boardName,
                color = boardStyle.textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
            )
        }

        // Основная плата с rounded corners
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(vertical = 16.dp)
                .background(boardStyle.innerBoardColor, RoundedCornerShape(20.dp))
        )

        // Левые пины
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = -boardStyle.pinOverlap)
        ) {
            leftPins.forEach { pin ->
                LeftPinRowFinal(
                    pin = pin,
                    boardStyle = boardStyle,
                    showLabel = showLabels,
                    isAssigned = assignedPins.contains(pin.number),
                    onPinClicked = { onPinClicked(it) },
                    onPinPositionChanged = onPinPositionChanged
                )
            }
        }

        // Правые пины
        rightPins.forEachIndexed { index, pin ->
            val verticalSpacing = 48.dp
            val startOffset = if (rightPins.size > 1) {
                // Центрируем группу пинов по вертикали
                -((rightPins.size - 1) * verticalSpacing / 2) + (index * verticalSpacing)
            } else {
                0.dp
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = boardStyle.pinOverlap, y = startOffset)
            ) {
                RightPinRowFinal(
                    pin = pin,
                    boardStyle = boardStyle,
                    showLabel = showLabels,
                    isAssigned = assignedPins.contains(pin.number),
                    onPinClicked = { onPinClicked(it) },
                    onPinPositionChanged = onPinPositionChanged
                )
            }
        }
        // Декор
        Box(Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp)
            .width(100.dp)
            .height(30.dp)
            .background(Color(0xFF37474F), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier
                .width(60.dp)
                .height(10.dp)
                .background(Color(0xFF78909C), RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun WemosD1MiniBoard(
    modifier: Modifier = Modifier,
    boardStyle: BoardStyle = BoardStyle(),
    showLabels: Boolean = true,
    assignedPins: Set<Int> = emptySet(),
    onPinClicked: (BoardPin) -> Unit,
    onPinPositionChanged: (pinNumber: Int, center: Offset) -> Unit = { _, _ -> }
) {
    UniversalBoardFinalV2(
        modifier = modifier,
        boardName = "Wemos D1 mini",
        leftPins = getWemosLeftPins(),
        rightPins = getWemosRightPins(),
        boardStyle = boardStyle,
        showLabels = showLabels,
        assignedPins = assignedPins,
        onPinClicked = { onPinClicked(it) },
        onPinPositionChanged = onPinPositionChanged
    )
}

@Composable
fun LeftPinRowFinal(
    pin: BoardPin,
    boardStyle: BoardStyle,
    showLabel: Boolean,
    isAssigned: Boolean,
    onPinClicked: (BoardPin) -> Unit,
    onPinPositionChanged: (pinNumber: Int, center: Offset) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = { onPinClicked(pin) })
            .onGloballyPositioned { coords ->
                pin.number?.let { num ->
                    val center = coords.localToRoot(coords.size.toSize().center)
                    onPinPositionChanged(num, center)
                }
            }
    ) {
        PinCircle(boardStyle = boardStyle, isAssigned = isAssigned)
        if (showLabel) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = pin.name,
                color = boardStyle.textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RightPinRowFinal(
    pin: BoardPin,
    boardStyle: BoardStyle,
    showLabel: Boolean,
    isAssigned: Boolean,
    onPinClicked: (BoardPin) -> Unit,
    onPinPositionChanged: (pinNumber: Int, center: Offset) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = { onPinClicked(pin) })
            .onGloballyPositioned { coords ->
                pin.number?.let { num ->
                    val center = coords.localToRoot(coords.size.toSize().center)
                    onPinPositionChanged(num, center)
                }
            }
    ) {
        if (showLabel) {
            Text(
                text = pin.name,
                color = boardStyle.textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        PinCircle(boardStyle = boardStyle, isAssigned = isAssigned)
    }
}

@Composable
fun PinCircle(
    boardStyle: BoardStyle,
    modifier: Modifier = Modifier,
    isAssigned: Boolean = false
) {
    val color = if (isAssigned) Color(0xFFFFC107) else boardStyle.pinColor
    Box(
        modifier = modifier
            .size(boardStyle.pinSize)
            .clip(CircleShape)
            .background(color)
    )
}

// Пины для Wemos D1 Mini
fun getWemosLeftPins(): List<BoardPin> {
    return listOf(
        BoardPin("D0", 16),  // GPIO16
        BoardPin("D1", 5),   // GPIO5
        BoardPin("D2", 4),   // GPIO4
        BoardPin("D3", 0),   // GPIO0
        BoardPin("D4", 2),   // GPIO2 (встроенный LED)
        BoardPin("D5", 14),  // GPIO14
        BoardPin("D6", 12),  // GPIO12
        BoardPin("D7", 13),  // GPIO13
        BoardPin("D8", 15)   // GPIO15
    )
}

fun getWemosRightPins(): List<BoardPin> {
    return listOf(
        BoardPin("3V3", null), // Питание 3.3В
        BoardPin("GND", null), // Земля
        BoardPin("TX", 1),     // GPIO1 (UART TX)
        BoardPin("RX", 3),     // GPIO3 (UART RX)
        BoardPin("A0", 17),    // Аналоговый вход (особый случай, не GPIO)
        BoardPin("RST", null), // Reset
        BoardPin("5V", null)   // Питание 5В
    )
}

//@Composable
//fun UniversalBoardFinal(
//    modifier: Modifier = Modifier,
//    boardName: String = "Wemos D1 mini",
//    leftPins: List<BoardPin> = emptyList(),
//    rightPins: List<BoardPin> = emptyList(),
//    boardStyle: BoardStyle = BoardStyle(),
//    showLabels: Boolean = true
//) {
//    Box(
//        modifier = modifier
//            .clip(RoundedCornerShape(boardStyle.cornerRadius))
//            .background(boardStyle.boardColor)
//            .padding(24.dp)
//    ) {
//        // Название платы
//        if (showLabels) {
//            Text(
//                text = boardName,
//                color = boardStyle.textColor,
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                textAlign = TextAlign.Center,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.TopCenter)
//            )
//        }
//
//        // Основная плата с rounded corners
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .align(Alignment.Center)
//                .padding(vertical = 32.dp)
//                .background(boardStyle.innerBoardColor, RoundedCornerShape(20.dp))
//        )
//
//        // ЛЕВЫЕ ПИНЫ - рисуются ОТДЕЛЬНО поверх всего
//        Column(
//            verticalArrangement = Arrangement.spacedBy(24.dp),
//            modifier = Modifier
//                .align(Alignment.CenterStart)
//                .offset(x = -boardStyle.pinOverlap)
//        ) {
//            leftPins.forEach { pin ->
//                LeftPinRowFinal(
//                    pin = pin,
//                    boardStyle = boardStyle,
//                    showLabel = showLabels
//                )
//            }
//        }
//
//        // ПРАВЫЕ ПИНЫ - рисуются ОТДЕЛЬНО поверх всего
//        Column(
//            verticalArrangement = Arrangement.spacedBy(24.dp),
//            modifier = Modifier
//                .align(Alignment.CenterEnd)
//                .offset(x = boardStyle.pinOverlap)
//        ) {
//            rightPins.forEach { pin ->
//                RightPinRowFinal(
//                    pin = pin,
//                    boardStyle = boardStyle,
//                    showLabel = showLabels
//                )
//            }
//        }
//    }
//}

//@Composable
//fun UniversalBoard(
//    modifier: Modifier = Modifier,
//    boardName: String = "Wemos D1 mini",
//    leftPins: List<BoardPin> = emptyList(),
//    rightPins: List<BoardPin> = emptyList(),
//    boardStyle: BoardStyle = BoardStyle(),
//    showLabels: Boolean = true
//) {
//    Column(
//        modifier = modifier
//            .clip(RoundedCornerShape(boardStyle.cornerRadius))
//            .background(boardStyle.boardColor)
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        // Название платы
//        if (showLabels) {
//            Text(
//                text = boardName,
//                color = boardStyle.textColor,
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//        }
//
//        // Основная плата с пинами
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(20.dp))
//                .background(boardStyle.innerBoardColor)
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 32.dp),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                // Левая колонка с пинами - выходят за левый край
//                Column(
//                    verticalArrangement = Arrangement.spacedBy(24.dp),
//                    horizontalAlignment = Alignment.Start,
//                    modifier = Modifier.offset(x = -boardStyle.pinOverlap)
//                ) {
//                    leftPins.forEach { pin ->
//                        LeftPinRow(
//                            pin = pin,
//                            boardStyle = boardStyle,
//                            showLabel = showLabels
//                        )
//                    }
//                }
//
//                // Правая колонка с пинами - выходят за правый край
//                Column(
//                    verticalArrangement = Arrangement.spacedBy(24.dp),
//                    horizontalAlignment = Alignment.End,
//                    modifier = Modifier.offset(x = boardStyle.pinOverlap)
//                ) {
//                    rightPins.forEach { pin ->
//                        RightPinRow(
//                            pin = pin,
//                            boardStyle = boardStyle,
//                            showLabel = showLabels
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

//@Composable
//fun LeftPinRow(
//    pin: BoardPin,
//    boardStyle: BoardStyle,
//    showLabel: Boolean
//) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.Start
//    ) {
//        // Пин выходит за левый край
//        PinCircle(
//            boardStyle = boardStyle,
//            modifier = Modifier.offset(x = -boardStyle.pinOverlap)
//        )
//
//        // Текст внутри платы
//        if (showLabel) {
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = pin.name,
//                color = boardStyle.textColor,
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Medium
//            )
//        }
//    }
//}
//
//@Composable
//fun RightPinRow(
//    pin: BoardPin,
//    boardStyle: BoardStyle,
//    showLabel: Boolean
//) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.End
//    ) {
//        // Текст внутри платы
//        if (showLabel) {
//            Text(
//                text = pin.name,
//                color = boardStyle.textColor,
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Medium,
//                textAlign = TextAlign.End
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//        }
//
//        // Пин выходит за правый край
//        PinCircle(
//            boardStyle = boardStyle,
//            modifier = Modifier.offset(x = boardStyle.pinOverlap)
//        )
//    }
//}

// Альтернативная версия с абсолютным позиционированием пинов
//@Composable
//fun UniversalBoardWithAbsolutePins(
//    modifier: Modifier = Modifier,
//    boardName: String = "Wemos D1 mini",
//    leftPins: List<BoardPin> = emptyList(),
//    rightPins: List<BoardPin> = emptyList(),
//    boardStyle: BoardStyle = BoardStyle(),
//    showLabels: Boolean = true
//) {
//    Column(
//        modifier = modifier
//            .clip(RoundedCornerShape(boardStyle.cornerRadius))
//            .background(boardStyle.boardColor)
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        // Название платы
//        if (showLabels) {
//            Text(
//                text = boardName,
//                color = boardStyle.textColor,
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//        }
//
//        // Основная плата с абсолютным позиционированием пинов
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(20.dp))
//                .background(boardStyle.innerBoardColor)
//                .height(400.dp) // Фиксированная высота для точного позиционирования
//        ) {
//            // Левые пины
//            leftPins.forEachIndexed { index, pin ->
//                val verticalPosition = calculatePinPosition(index, leftPins.size, 400.dp)
//
//                // Пин (выступает за левый край)
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.CenterStart)
//                        .offset(x = -boardStyle.pinSize / 2)
//                        .offset(y = verticalPosition)
//                        .size(boardStyle.pinSize)
//                        .clip(CircleShape)
//                        .background(boardStyle.pinColor)
//                )
//
//                // Текст пина (внутри платы)
//                if (showLabels) {
//                    Text(
//                        text = pin.name,
//                        color = boardStyle.textColor,
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Medium,
//                        modifier = Modifier
//                            .align(Alignment.CenterStart)
//                            .offset(x = boardStyle.pinSize, y = verticalPosition)
//                    )
//                }
//            }
//
//            // Правые пины
//            rightPins.forEachIndexed { index, pin ->
//                val verticalPosition = calculatePinPosition(index, rightPins.size, 400.dp)
//
//                // Пин (выступает за правый край)
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.CenterEnd)
//                        .offset(x = boardStyle.pinSize / 2)
//                        .offset(y = verticalPosition)
//                        .size(boardStyle.pinSize)
//                        .clip(CircleShape)
//                        .background(boardStyle.pinColor)
//                )
//
//                // Текст пина (внутри платы)
//                if (showLabels) {
//                    Text(
//                        text = pin.name,
//                        color = boardStyle.textColor,
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Medium,
//                        textAlign = TextAlign.End,
//                        modifier = Modifier
//                            .align(Alignment.CenterEnd)
//                            .offset(x = -boardStyle.pinSize, y = verticalPosition)
//                    )
//                }
//            }
//        }
//    }
//}

// Расчет вертикальной позиции пина для равномерного распределения
//private fun calculatePinPosition(index: Int, totalPins: Int, containerHeight: Dp): Dp {
//    val spacing = containerHeight / (totalPins + 1)
//    return spacing * (index + 1) - containerHeight / 2
//}

// Пины для Arduino Uno
fun getArduinoLeftPins(): List<BoardPin> {
    return listOf(
        BoardPin("D0"), BoardPin("D1"), BoardPin("D2"), BoardPin("D3"),
        BoardPin("D4"), BoardPin("D5"), BoardPin("D6"), BoardPin("D7"),
        BoardPin("D8"), BoardPin("D9"), BoardPin("D10"), BoardPin("D11"),
        BoardPin("D12"), BoardPin("D13")
    )
}

fun getArduinoRightPins(): List<BoardPin> {
    return listOf(
        BoardPin("GND"), BoardPin("AREF"), BoardPin("A0"), BoardPin("A1"),
        BoardPin("A2"), BoardPin("A3"), BoardPin("A4"), BoardPin("A5"),
        BoardPin("5V"), BoardPin("3V3"), BoardPin("VIN"), BoardPin("GND"),
        BoardPin("RST")
    )
}

// Примеры использования
@Composable
@Preview
fun BoardExample() {

        // Wemos D1 Mini с пинами по краям
        WemosD1MiniBoard(
            modifier = Modifier
                .height(700.dp),
            boardStyle = BoardStyle(
                pinOverlap = 14.dp
            ),
            onPinClicked = {}
        )

//        // Arduino Uno с большим выступом пинов
//        UniversalBoard(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(600.dp),
//            boardName = "Arduino Uno",
//            leftPins = getArduinoLeftPins(),
//            rightPins = getArduinoRightPins(),
//            boardStyle = BoardStyle(
//                boardColor = Color(0xFF0097A7),
//                innerBoardColor = Color(0xFF006978),
//                pinOverlap = 16.dp
//            )
//        )
//
//        // Версия с абсолютным позиционированием
//        UniversalBoardWithAbsolutePins(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(500.dp),
//            boardName = "ESP32 DevKit",
//            leftPins = getWemosLeftPins(),
//            rightPins = getWemosRightPins(),
//            boardStyle = BoardStyle(
//                boardColor = Color(0xFF7B1FA2),
//                innerBoardColor = Color(0xFF6A1B9A),
//                pinColor = Color(0xFFCE93D8),
//                pinOverlap = 12.dp
//            )
//        )
}