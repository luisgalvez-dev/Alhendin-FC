package com.luis.alhendinfc.ui.matches

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.domain.model.CallupStatus

/**
 * Draws a realistic football jersey using Canvas bezier paths.
 *
 * Shape: crew/round collar, wide shoulders, short sleeves with concave armhole,
 * rectangular body with slight taper. All coordinates are relative to canvas size.
 *
 * Visual style per status (matching reference images):
 *   TITULAR  → solid dark-green fill, mint number
 *   SUPLENTE → transparent fill with thick amber outline, dark-amber number
 *   NONE     → light-grey fill with thin grey outline, grey number
 */
@Composable
fun JerseyIcon(
    number: Int,
    status: CallupStatus,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val fillColor = when (status) {
        CallupStatus.TITULAR  -> Color(0xFF1B5E20)
        CallupStatus.SUPLENTE -> Color(0xFFF9A825).copy(alpha = 0.10f)
        CallupStatus.NONE     -> Color(0xFFEEEEEE)
    }
    val borderColor = when (status) {
        CallupStatus.TITULAR  -> Color(0xFF2E7D32)
        CallupStatus.SUPLENTE -> Color(0xFFF9A825)
        CallupStatus.NONE     -> Color(0xFFBDBDBD)
    }
    val numberColor = when (status) {
        CallupStatus.TITULAR  -> Color(0xFF80CBC4)   // mint
        CallupStatus.SUPLENTE -> Color(0xFFE65100)   // deep amber
        CallupStatus.NONE     -> Color(0xFF9E9E9E)
    }
    val borderWidth = when (status) {
        CallupStatus.SUPLENTE -> 0.050f  // thick outline for the yellow style
        else                  -> 0.030f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // ── Jersey silhouette path (clockwise) ─────────────────────────
            //
            //         ╭──collar──╮
            //    ╱──────╮       ╭──────╲
            //   │ sleeve│       │sleeve │
            //    ╲──────╯       ╰──────╱
            //        │               │
            //        └───────────────┘
            //
            val jerseyPath = Path().apply {
                // Left collar edge
                moveTo(w * 0.30f, h * 0.17f)

                // Round crew-neck arc, peaking near the top
                cubicTo(
                    w * 0.34f, h * 0.02f,
                    w * 0.66f, h * 0.02f,
                    w * 0.70f, h * 0.17f
                )

                // Right shoulder – diagonal outward line
                lineTo(w * 0.92f, h * 0.13f)

                // Right sleeve outer edge (top → bottom)
                lineTo(w * 1.00f, h * 0.28f)
                lineTo(w * 1.00f, h * 0.52f)

                // Right sleeve inner / cuff
                lineTo(w * 0.82f, h * 0.52f)

                // Right armhole – concave curve into the body
                // Control point pulled RIGHT creates the inward concave dip
                quadraticTo(
                    w * 0.89f, h * 0.45f,
                    w * 0.79f, h * 0.40f
                )

                // Right body side down to hem (very slight inward taper)
                lineTo(w * 0.84f, h * 0.97f)

                // Bottom hem
                lineTo(w * 0.16f, h * 0.97f)

                // Left body side up to armhole junction
                lineTo(w * 0.21f, h * 0.40f)

                // Left armhole – concave curve, control point pulled LEFT
                quadraticTo(
                    w * 0.11f, h * 0.45f,
                    w * 0.18f, h * 0.52f
                )

                // Left sleeve inner / cuff
                lineTo(w * 0.00f, h * 0.52f)

                // Left sleeve outer edge (bottom → top)
                lineTo(w * 0.00f, h * 0.28f)

                // Left shoulder – diagonal inward line
                lineTo(w * 0.08f, h * 0.13f)

                // Close back to left collar
                lineTo(w * 0.30f, h * 0.17f)
                close()
            }

            // Fill
            drawPath(jerseyPath, fillColor)

            // Outline border
            drawPath(
                jerseyPath,
                borderColor,
                style = Stroke(
                    width = w * borderWidth,
                    join  = StrokeJoin.Round,
                    cap   = StrokeCap.Round
                )
            )

            // ── Collar band ────────────────────────────────────────────────
            // Draw the neckline arc again with a thick line to simulate the
            // collar rib, then overlay with fill colour for the inner highlight.
            val collarPath = Path().apply {
                moveTo(w * 0.30f, h * 0.17f)
                cubicTo(
                    w * 0.34f, h * 0.02f,
                    w * 0.66f, h * 0.02f,
                    w * 0.70f, h * 0.17f
                )
            }
            // Outer collar band
            drawPath(
                collarPath,
                borderColor,
                style = Stroke(width = w * (borderWidth + 0.028f), cap = StrokeCap.Round)
            )
            // Inner collar highlight (same as fill, creates band illusion)
            drawPath(
                collarPath,
                fillColor,
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )

            // ── Sleeve cuff lines ──────────────────────────────────────────
            // Thin horizontal lines at the end of each sleeve for definition
            drawLine(
                color = borderColor,
                start = androidx.compose.ui.geometry.Offset(w * 0.00f, h * 0.48f),
                end   = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.48f),
                strokeWidth = w * 0.022f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = borderColor,
                start = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.48f),
                end   = androidx.compose.ui.geometry.Offset(w * 1.00f, h * 0.48f),
                strokeWidth = w * 0.022f,
                cap = StrokeCap.Round
            )
        }

        // Number centred in the body area (offset down past the collar)
        Text(
            text  = if (number > 0) number.toString() else "?",
            fontSize   = (size.value * 0.28f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = numberColor,
            modifier = Modifier.offset(y = size * 0.13f)
        )
    }
}
