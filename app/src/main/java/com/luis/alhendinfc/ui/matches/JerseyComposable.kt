package com.luis.alhendinfc.ui.matches

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.R
import com.luis.alhendinfc.domain.model.CallupStatus

@Composable
fun JerseyIcon(
    number: Int,
    status: CallupStatus,
    size: Dp = 56.dp,
    showNumber: Boolean = true,
    tint: Color? = null,
    modifier: Modifier = Modifier
) {
    val jerseyColor = tint ?: when (status) {
        CallupStatus.TITULAR -> Color(0xFF2E9B4F)
        CallupStatus.SUPLENTE -> Color(0xFFFFB300)
        CallupStatus.NONE -> Color(0xFF8A9A8C)
    }
    val numberColor = when {
        tint != null -> Color.White.copy(alpha = 0.55f)
        status == CallupStatus.TITULAR -> Color.White
        status == CallupStatus.SUPLENTE -> Color(0xFF4E342E)
        else -> Color(0xFF616161)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_jersey),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(jerseyColor)
        )
        if (showNumber) {
            Text(
                text = if (number > 0) number.toString() else "?",
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = numberColor,
                modifier = Modifier.offset(y = size * 0.02f)
            )
        }
    }
}
