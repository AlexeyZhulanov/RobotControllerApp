import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

data class BoardPin(
    val name: String,
    val position: Offset,
    val side: PinSide
)

enum class PinSide { LEFT, RIGHT }

data class BoardStyle(
    val boardColor: Color = Color(0xFF0D47A1),
    val innerBoardColor: Color = Color(0xFF1A237E),
    val pinColor: Color = Color(0xFF9E9E9E),
    val textColor: Color = Color.White,
    val cornerRadius: Float = 40f,
    val pinRadius: Float = 18f
)

@Composable
fun WemosD1MiniBoardV1(
    modifier: Modifier = Modifier,
    boardStyle: BoardStyle = BoardStyle(),
    showLabels: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier.background(Color.Transparent)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Вычисляем масштаб так, чтобы плата занимала всю доступную область
            val scaleX = canvasWidth / 600f  // 600 - исходная ширина SVG
            val scaleY = canvasHeight / 700f // 700 - исходная высота SVG
            val scale = minOf(scaleX, scaleY) // Используем минимальный масштаб для сохранения пропорций

            // Центрируем плату на Canvas
            val offsetX = (canvasWidth - 600f * scale) / 2f
            val offsetY = (canvasHeight - 700f * scale) / 2f

            drawBoard(
                style = boardStyle,
                textMeasurer = textMeasurer,
                showLabels = showLabels,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY
            )
        }
    }
}

@Composable
fun UniversalBoard(
    modifier: Modifier = Modifier,
    boardName: String = "Wemos D1 mini",
    pins: List<BoardPin> = getWemosD1MiniPins(),
    boardStyle: BoardStyle = BoardStyle(),
    showLabels: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(Color.Transparent)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Вычисляем масштаб так, чтобы плата занимала всю доступную область
            val scaleX = canvasWidth / 600f  // 600 - исходная ширина SVG
            val scaleY = canvasHeight / 700f // 700 - исходная высота SVG
            val scale = minOf(scaleX, scaleY)

            // Центрируем плату на Canvas
            val offsetX = (canvasWidth - 600f * scale) / 2f
            val offsetY = (canvasHeight - 700f * scale) / 2f

            drawUniversalBoard(
                boardName = boardName,
                pins = pins,
                style = boardStyle,
                textMeasurer = textMeasurer,
                showLabels = showLabels,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY
            )
        }
    }
}

private fun DrawScope.drawBoard(
    style: BoardStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    showLabels: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {

    fun getScaledOffset(x: Float, y: Float): Offset {
        return Offset(offsetX + x * scale, offsetY + y * scale)
    }

    fun getScaledSize(width: Float, height: Float): Size {
        return Size(width * scale, height * scale)
    }

    // Основной корпус
    drawRoundRect(
        color = style.boardColor,
        topLeft = getScaledOffset(100f, 30f),
        size = getScaledSize(400f, 640f),
        cornerRadius = CornerRadius(style.cornerRadius * scale)
    )

    // Внутренняя плата
    drawRect(
        color = style.innerBoardColor,
        topLeft = getScaledOffset(140f, 100f),
        size = getScaledSize(320f, 500f)
    )

    // Название платы
    if (showLabels) {
        drawText(
            textMeasurer = textMeasurer,
            text = "Wemos D1 mini",
            topLeft = getScaledOffset(170f, 50f),
            style = TextStyle(
                color = style.textColor,
                fontSize = (14 * scale).sp
            )
        )
    }

    // Контакты
    val pins = getWemosD1MiniPins()
    pins.forEach { pin ->
        drawCircle(
            color = style.pinColor,
            center = getScaledOffset(pin.position.x, pin.position.y),
            radius = style.pinRadius * scale
        )

        if (showLabels) {
            val textOffset = when (pin.side) {
                PinSide.LEFT -> getScaledOffset(pin.position.x + 35f, pin.position.y - 16f)
                PinSide.RIGHT -> getScaledOffset(pin.position.x - 75f, pin.position.y - 16f)
            }

            drawText(
                textMeasurer = textMeasurer,
                text = pin.name,
                topLeft = textOffset,
                style = TextStyle(
                    color = style.textColor,
                    fontSize = (10 * scale).sp,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    ),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            )
        }
    }

    // Дополнительные детали - микросхема
    drawRoundRect(
        color = Color(0xFF37474F),
        topLeft = getScaledOffset(250f, 580f),
        size = getScaledSize(100f, 30f),
        cornerRadius = CornerRadius(5f * scale)
    )

    drawRoundRect(
        color = Color(0xFF78909C),
        topLeft = getScaledOffset(270f, 590f),
        size = getScaledSize(60f, 10f),
        cornerRadius = CornerRadius(2f * scale)
    )
}

private fun DrawScope.drawUniversalBoard(
    boardName: String,
    pins: List<BoardPin>,
    style: BoardStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    showLabels: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {

    fun getScaledOffset(x: Float, y: Float): Offset {
        return Offset(offsetX + x * scale, offsetY + y * scale)
    }

    fun getScaledSize(width: Float, height: Float): Size {
        return Size(width * scale, height * scale)
    }

    // Основной корпус
    drawRoundRect(
        color = style.boardColor,
        topLeft = getScaledOffset(100f, 30f),
        size = getScaledSize(400f, 640f),
        cornerRadius = CornerRadius(style.cornerRadius * scale)
    )

    // Внутренняя плата
    drawRect(
        color = style.innerBoardColor,
        topLeft = getScaledOffset(140f, 100f),
        size = getScaledSize(320f, 500f)
    )

    // Название платы
    if (showLabels) {
        // Вычисляем ширину текста для центрирования
        val textLayoutResult = textMeasurer.measure(
            text = boardName,
            style = TextStyle(fontSize = (28 * scale).sp)
        )
        val textWidth = textLayoutResult.size.width
        val centerX = (600f * scale - textWidth) / 2f

        drawText(
            textMeasurer = textMeasurer,
            text = boardName,
            topLeft = Offset(offsetX + centerX, offsetY + 65f * scale),
            style = TextStyle(
                color = style.textColor,
                fontSize = (28 * scale).sp
            )
        )
    }

    // Контакты
    pins.forEach { pin ->
        drawCircle(
            color = style.pinColor,
            center = getScaledOffset(pin.position.x, pin.position.y),
            radius = style.pinRadius * scale
        )

        if (showLabels) {
            val textOffset = when (pin.side) {
                PinSide.LEFT -> getScaledOffset(pin.position.x + 35f, pin.position.y - 8f)
                PinSide.RIGHT -> getScaledOffset(pin.position.x - 75f, pin.position.y - 8f)
            }

            drawText(
                textMeasurer = textMeasurer,
                text = pin.name,
                topLeft = textOffset,
                style = TextStyle(
                    color = style.textColor,
                    fontSize = (20 * scale).sp
                )
            )
        }
    }
}

// Стандартные пины для Wemos D1 Mini
fun getWemosD1MiniPins(): List<BoardPin> {
    return listOf(
        // Левая сторона
        BoardPin("D0", Offset(140f, 150f), PinSide.LEFT),
        BoardPin("D1", Offset(140f, 200f), PinSide.LEFT),
        BoardPin("D2", Offset(140f, 250f), PinSide.LEFT),
        BoardPin("D3", Offset(140f, 300f), PinSide.LEFT),
        BoardPin("D4", Offset(140f, 350f), PinSide.LEFT),
        BoardPin("D5", Offset(140f, 400f), PinSide.LEFT),
        BoardPin("D6", Offset(140f, 450f), PinSide.LEFT),
        BoardPin("D7", Offset(140f, 500f), PinSide.LEFT),
        BoardPin("D8", Offset(140f, 550f), PinSide.LEFT),

        // Правая сторона
        BoardPin("3V3", Offset(460f, 150f), PinSide.RIGHT),
        BoardPin("GND", Offset(460f, 200f), PinSide.RIGHT),
        BoardPin("TX", Offset(460f, 250f), PinSide.RIGHT),
        BoardPin("RX", Offset(460f, 300f), PinSide.RIGHT),
        BoardPin("A0", Offset(460f, 350f), PinSide.RIGHT),
        BoardPin("RST", Offset(460f, 400f), PinSide.RIGHT),
        BoardPin("5V", Offset(460f, 450f), PinSide.RIGHT)
    )
}

// Использование в Compose
@Composable
@Preview
fun BoardExamples() {
    // Простая Wemos D1 Mini - займет весь доступный размер
    WemosD1MiniBoardV1(
        modifier = Modifier.aspectRatio(600f/700f)
    )

//    // Универсальная плата с кастомными настройками
//    UniversalBoard(
//        modifier = Modifier.size(250.dp),
//        boardName = "Custom Board",
//        pins = getWemosD1MiniPins(),
//        boardStyle = BoardStyle(
//            boardColor = Color(0xFF00695C),
//            innerBoardColor = Color(0xFF004D40),
//            pinColor = Color(0xFFBDBDBD)
//        )
//    )
//
//    // На весь экран
//    UniversalBoard(
//        modifier = Modifier.fillMaxSize(),
//        boardName = "Full Screen Board",
//        pins = getWemosD1MiniPins()
//    )
}