package com.example.robotcontrollerapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SlideSide { Left, Right }

@Composable
fun SlideOutControlPanel(
    modifier: Modifier = Modifier,
    panelWidth: Dp,
    side: SlideSide = SlideSide.Left,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    // сколько панели остается видно (стрелка)
    val handleWidth = 32.dp

    val offsetX by animateDpAsState(
        targetValue = if (expanded) {
            0.dp
        } else {
            when (side) {
                SlideSide.Left -> (panelWidth - handleWidth)
                SlideSide.Right -> -(panelWidth - handleWidth)
            }
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "panelOffset"
    )

    val alpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "panelAlpha"
    )

    Box(
        modifier = modifier
            .width(panelWidth)
            .offset(x = offsetX)
    ) {

        // Основной контент
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = if (side == SlideSide.Left) handleWidth else 0.dp,
                    end = if (side == SlideSide.Right) handleWidth else 0.dp)
                .alpha(alpha)
        ) {
            content()
        }

        // HANDLE (стрелка)
        Box(
            modifier = Modifier
                .width(handleWidth)
                .fillMaxHeight()
                .align(
                    if (side == SlideSide.Left)
                        Alignment.CenterStart
                    else
                        Alignment.CenterEnd
                )
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    expanded && side == SlideSide.Left -> ">"
                    expanded && side == SlideSide.Right -> "<"
                    !expanded && side == SlideSide.Left -> "<"
                    else -> ">"
                },
                fontSize = 32.sp,
                color = Color.White,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(4.dp)
            )
        }
    }
}
