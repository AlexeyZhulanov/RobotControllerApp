package com.example.robotcontrollerapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.robotcontrollerapp.R

enum class SlideSide { LeftTop, RightBottom }

@Composable
fun SlideOutControlPanel(
    modifier: Modifier = Modifier,
    panelSize: Dp,
    isVertical: Boolean = false,
    side: SlideSide = SlideSide.LeftTop,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    // сколько панели остается видно (стрелка)
    val handleSize = 32.dp

    val offset by animateDpAsState(
        targetValue = if (expanded) {
            0.dp
        } else {
            when (side) {
                SlideSide.LeftTop -> (panelSize - handleSize)
                SlideSide.RightBottom -> -(panelSize - handleSize)
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

    val parentBoxModifier = if(!isVertical) {
        modifier.width(panelSize).offset(x = offset)
    } else {
        modifier.height(panelSize).offset(y = offset)
    }
    Box(modifier = parentBoxModifier) {
        // Основной контент
        val mainContentModifier = if(!isVertical) {
            Modifier.fillMaxHeight().padding(start = if (side == SlideSide.LeftTop) handleSize else 0.dp,
                end = if (side == SlideSide.RightBottom) handleSize else 0.dp).alpha(alpha)
        } else {
            Modifier.fillMaxWidth().padding(top = if (side == SlideSide.LeftTop) handleSize else 0.dp,
                bottom = if (side == SlideSide.RightBottom) handleSize else 0.dp).alpha(alpha)
        }
        Box(modifier = mainContentModifier) {
            content()
        }

        // HANDLE (стрелка)
        val handleModifier = if(!isVertical) {
            Modifier
                .width(handleSize)
                .fillMaxHeight(0.4f)
                .align(if (side == SlideSide.LeftTop) Alignment.CenterStart else Alignment.CenterEnd)
                .clickable { expanded = !expanded }
        } else {
            Modifier
                .height(handleSize)
                .fillMaxWidth(0.4f)
                .align(if (side == SlideSide.LeftTop) Alignment.TopCenter else Alignment.BottomCenter)
                .clickable { expanded = !expanded }
        }
        Box(modifier = handleModifier, contentAlignment = Alignment.Center) {
            val textModifier = if(!isVertical) {
                Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(vertical = 4.dp)
            } else {
                Modifier.graphicsLayer {
                    rotationZ = 90f
                    transformOrigin = TransformOrigin.Center
                }.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(vertical = 4.dp)
            }
            Icon(
                painter = when {
                    expanded && side == SlideSide.LeftTop -> painterResource(R.drawable.ic_arrow_right)
                    expanded && side == SlideSide.RightBottom -> painterResource(R.drawable.ic_arrow_back)
                    !expanded && side == SlideSide.LeftTop -> painterResource(R.drawable.ic_arrow_back)
                    else -> painterResource(R.drawable.ic_arrow_right)
                },
                tint = Color.White,
                modifier = textModifier,
                contentDescription = "iconArrow"
            )
        }
    }
}
