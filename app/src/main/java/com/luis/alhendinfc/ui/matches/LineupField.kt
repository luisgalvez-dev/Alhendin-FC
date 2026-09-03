package com.luis.alhendinfc.ui.matches

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Formation
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.assignPlayersToFormation

@Composable
fun LineupField(
    formation: Formation,
    titulares: List<Player>,
    modifier: Modifier = Modifier
) {
    val assigned = assignPlayersToFormation(titulares, formation)
    val fieldGreen = Color(0xFF2D7A2D)
    val lineWhite = Color.White

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(10.dp))
    ) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.005f)
        val pad = w * 0.025f

        drawRect(color = fieldGreen, size = size)

        drawRect(
            color = lineWhite,
            topLeft = Offset(pad, pad),
            size = Size(w - pad * 2, h - pad * 2),
            style = stroke
        )

        drawLine(
            color = lineWhite,
            start = Offset(w * 0.5f, pad),
            end = Offset(w * 0.5f, h - pad),
            strokeWidth = w * 0.004f
        )
        drawCircle(
            color = lineWhite,
            radius = h * 0.16f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = stroke
        )
        drawCircle(
            color = lineWhite,
            radius = w * 0.007f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        drawRect(
            color = lineWhite,
            topLeft = Offset(pad, h * 0.5f - h * 0.32f),
            size = Size(w * 0.16f, h * 0.64f),
            style = stroke
        )
        drawRect(
            color = lineWhite,
            topLeft = Offset(w - pad - w * 0.16f, h * 0.5f - h * 0.32f),
            size = Size(w * 0.16f, h * 0.64f),
            style = stroke
        )

        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = w * 0.032f
            isFakeBoldText = true
        }

        assigned.forEach { (slot, player) ->
            val px = slot.x * w
            val py = slot.y * h
            val radius = w * 0.038f
            if (player != null) {
                drawCircle(color = Color(0xFF1B5E20), radius = radius, center = Offset(px, py))
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    center = Offset(px, py),
                    style = Stroke(width = w * 0.004f)
                )
                textPaint.color = android.graphics.Color.WHITE
                val label = if (player.jerseyNumber > 0) player.jerseyNumber.toString() else "?"
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    px,
                    py + textPaint.textSize * 0.35f,
                    textPaint
                )
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = radius,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = radius,
                    center = Offset(px, py),
                    style = Stroke(width = w * 0.003f)
                )
            }
        }
    }
}
