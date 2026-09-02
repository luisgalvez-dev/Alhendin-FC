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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.domain.model.CallupStatus

/**
 * Football jersey icon drawn on Canvas.
 *
 * Path mirrors ic_jersey.xml (100×100 viewport → normalised 0..1 coords):
 *
 *   M 34,10 C 38,24 62,24 66,10   <- round crew collar (U-dip)
 *   L 100,10                       <- right shoulder/sleeve top (flat bar)
 *   L 100,44  L 79,44              <- right sleeve + horizontal cuff
 *   L 79,95   L 21,95              <- right body + bottom hem
 *   L 21,44   L 0,44               <- left body + horizontal cuff
 *   L 0,10                         <- left sleeve/shoulder top
 *   Z                              <- close back to left collar
 *
 * Styles:
 *   TITULAR  → solid dark-green fill, white number
 *   SUPLENTE → no fill, thick amber outline, amber number
 *   NONE     → light-grey fill, thin grey outline, grey number
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
        CallupStatus.SUPLENTE -> Color.Transparent
        CallupStatus.NONE     -> Color(0xFFE8E8E8)
    }
    val strokeColor = when (status) {
        CallupStatus.TITULAR  -> Color(0xFF2E7D32)
        CallupStatus.SUPLENTE -> Color(0xFFF9A825)
        CallupStatus.NONE     -> Color(0xFFBDBDBD)
    }
    val numberColor = when (status) {
        CallupStatus.TITULAR  -> Color.White
        CallupStatus.SUPLENTE -> Color(0xFFF9A825)
        CallupStatus.NONE     -> Color(0xFF9E9E9E)
    }
    val strokeWidthFraction = when (status) {
        CallupStatus.SUPLENTE -> 0.060f
        CallupStatus.NONE     -> 0.035f
        CallupStatus.TITULAR  -> 0.028f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val jerseyPath = jerseyPath(this.size.width, this.size.height)

            // Fill
            drawPath(jerseyPath, fillColor)

            // Outline
            drawPath(
                jerseyPath,
                strokeColor,
                style = Stroke(
                    width = this.size.width * strokeWidthFraction,
                    join  = StrokeJoin.Round,
                    cap   = StrokeCap.Round
                )
            )
        }

        // Number centred over the body area (~68% of canvas height → offset ~18%)
        Text(
            text       = if (number > 0) number.toString() else "?",
            fontSize   = (size.value * 0.30f).sp,
            fontWeight = FontWeight.ExtraBold,
            color      = numberColor,
            modifier   = Modifier.offset(y = size * 0.14f)
        )
    }
}

/**
 * Builds the jersey Path for a given canvas [w] × [h].
 *
 * Viewport: 100×100
 *   - Collar:  M 34,10  C 38,24 62,24 66,10
 *   - Top bar: L 100,10
 *   - R sleeve:L 100,44  L 79,44
 *   - Body:    L 79,95   L 21,95
 *   - L sleeve:L 21,44   L 0,44
 *   - Close:   L 0,10    Z
 */
private fun DrawScope.jerseyPath(w: Float, h: Float): Path = Path().apply {
    // Left collar edge (top-left of the collar arc)
    moveTo(w * 0.34f, h * 0.10f)

    // Crew-neck collar: cubic bezier dipping to ~24% height
    cubicTo(
        w * 0.38f, h * 0.24f,
        w * 0.62f, h * 0.24f,
        w * 0.66f, h * 0.10f
    )

    // Flat top bar → right edge (shoulder + sleeve top)
    lineTo(w * 1.00f, h * 0.10f)

    // Right sleeve: vertical drop to cuff
    lineTo(w * 1.00f, h * 0.44f)

    // Right sleeve cuff: horizontal line inward to body width
    lineTo(w * 0.79f, h * 0.44f)

    // Right body side: straight down to hem
    lineTo(w * 0.79f, h * 0.95f)

    // Bottom hem
    lineTo(w * 0.21f, h * 0.95f)

    // Left body side: straight up to cuff
    lineTo(w * 0.21f, h * 0.44f)

    // Left sleeve cuff: horizontal line outward to edge
    lineTo(w * 0.00f, h * 0.44f)

    // Left sleeve: vertical rise to top
    lineTo(w * 0.00f, h * 0.10f)

    // Close path (returns to left collar edge at 34%, 10%)
    close()
}
