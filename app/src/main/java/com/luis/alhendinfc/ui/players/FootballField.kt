package com.luis.alhendinfc.ui.players

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.luis.alhendinfc.domain.model.PlayerPosition
import kotlin.math.sqrt

@Composable
fun FootballField(
    markedPosition: PlayerPosition?,
    modifier: Modifier = Modifier,
    onPositionClick: ((PlayerPosition) -> Unit)? = null
) {
    val fieldGreen = Color(0xFF2D7A2D)
    val lineWhite = Color(0xFFFFFFFF)
    val markerRed = Color(0xFFE53935)

    val interactiveModifier = if (onPositionClick != null) {
        modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val closest = PlayerPosition.entries.minByOrNull { pos ->
                        val px = pos.fieldX * w
                        val py = pos.fieldY * h
                        val dx = offset.x - px
                        val dy = offset.y - py
                        sqrt(dx * dx + dy * dy)
                    }
                    closest?.let { onPositionClick(it) }
                }
            }
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(2f)
    }

    Canvas(modifier = interactiveModifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.004f)
        val thickStroke = Stroke(width = w * 0.006f)

        // Fondo verde
        drawRect(color = fieldGreen, size = size)

        // Borde del campo
        val borderPad = w * 0.02f
        drawRect(
            color = lineWhite,
            topLeft = Offset(borderPad, borderPad),
            size = Size(w - borderPad * 2, h - borderPad * 2),
            style = thickStroke
        )

        // Línea central vertical
        drawLine(
            color = lineWhite,
            start = Offset(w * 0.5f, borderPad),
            end = Offset(w * 0.5f, h - borderPad),
            strokeWidth = w * 0.004f
        )

        // Círculo central
        drawCircle(
            color = lineWhite,
            radius = h * 0.18f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = stroke
        )

        // Punto central
        drawCircle(
            color = lineWhite,
            radius = w * 0.006f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Área grande izquierda
        drawFieldArea(
            leftX = borderPad,
            centerY = h * 0.5f,
            areaWidth = w * 0.18f,
            areaHalfHeight = h * 0.38f,
            color = lineWhite,
            stroke = stroke
        )

        // Área pequeña izquierda
        drawFieldArea(
            leftX = borderPad,
            centerY = h * 0.5f,
            areaWidth = w * 0.07f,
            areaHalfHeight = h * 0.20f,
            color = lineWhite,
            stroke = stroke
        )

        // Área grande derecha
        drawFieldArea(
            leftX = w - borderPad - w * 0.18f,
            centerY = h * 0.5f,
            areaWidth = w * 0.18f,
            areaHalfHeight = h * 0.38f,
            color = lineWhite,
            stroke = stroke,
            isRight = true
        )

        // Área pequeña derecha
        drawFieldArea(
            leftX = w - borderPad - w * 0.07f,
            centerY = h * 0.5f,
            areaWidth = w * 0.07f,
            areaHalfHeight = h * 0.20f,
            color = lineWhite,
            stroke = stroke,
            isRight = true
        )

        // Portería izquierda
        val goalHalfH = h * 0.12f
        val goalDepth = w * 0.025f
        drawRect(
            color = lineWhite,
            topLeft = Offset(borderPad - goalDepth, h * 0.5f - goalHalfH),
            size = Size(goalDepth, goalHalfH * 2),
            style = stroke
        )

        // Portería derecha
        drawRect(
            color = lineWhite,
            topLeft = Offset(w - borderPad, h * 0.5f - goalHalfH),
            size = Size(goalDepth, goalHalfH * 2),
            style = stroke
        )

        // Punto de penalti izquierdo
        drawCircle(
            color = lineWhite,
            radius = w * 0.005f,
            center = Offset(w * 0.12f, h * 0.5f)
        )

        // Punto de penalti derecho
        drawCircle(
            color = lineWhite,
            radius = w * 0.005f,
            center = Offset(w * 0.88f, h * 0.5f)
        )

        // Puntos de todas las posiciones disponibles
        PlayerPosition.entries.forEach { pos ->
            val px = pos.fieldX * w
            val py = pos.fieldY * h
            val isSelected = pos == markedPosition
            if (isSelected) {
                // Posición seleccionada: rojo grande con borde blanco
                drawCircle(
                    color = markerRed,
                    radius = w * 0.024f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.024f,
                    center = Offset(px, py),
                    style = Stroke(width = w * 0.003f)
                )
            } else {
                // Posición disponible: círculo blanco pequeño semitransparente
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = w * 0.014f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = w * 0.014f,
                    center = Offset(px, py),
                    style = Stroke(width = w * 0.002f)
                )
            }
        }
    }
}

private fun DrawScope.drawFieldArea(
    leftX: Float,
    centerY: Float,
    areaWidth: Float,
    areaHalfHeight: Float,
    color: Color,
    stroke: Stroke,
    isRight: Boolean = false
) {
    val rect = Rect(
        left = leftX,
        top = centerY - areaHalfHeight,
        right = leftX + areaWidth,
        bottom = centerY + areaHalfHeight
    )
    drawRect(
        color = color,
        topLeft = rect.topLeft,
        size = rect.size,
        style = stroke
    )
}
