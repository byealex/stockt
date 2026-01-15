package com.example.stockt.ui.util
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dashedBorder(
    strokeWidth: Dp,
    color: Color,
    cornerRadius: Dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 6.dp
) = this.then(
    Modifier.drawBehind {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(
                    dashLength.toPx(),
                    gapLength.toPx()
                )
            )
        )

        drawRoundRect(
            color = color,
            style = stroke,
            cornerRadius = CornerRadius(
                cornerRadius.toPx(),
                cornerRadius.toPx()
            )
        )
    }
)
