package com.luis.alhendinfc.ui.util

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luis.alhendinfc.ui.theme.GreenMint

/**
 * Visor a pantalla completa de una imagen local (Attachment.localPath o URI de escudo).
 * No copia el fichero. Si no existe, muestra un error y no lanza.
 */
@Composable
fun ImageViewer(
    source: String,
    title: String = "Imagen",
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)
        val context = LocalContext.current
        var bitmap by remember(source) { mutableStateOf<ImageBitmap?>(null) }
        var loaded by remember(source) { mutableStateOf(false) }
        var scale by remember(source) { mutableFloatStateOf(1f) }
        var offset by remember(source) { mutableStateOf(Offset.Zero) }

        LaunchedEffect(source) {
            bitmap = LocalImageLoader.load(context, source, maxSidePx = 2048)
            loaded = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                !loaded -> {
                    Text(
                        "Cargando…",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                bitmap == null -> {
                    Text(
                        "No se pudo abrir la imagen.",
                        color = Color(0xFFFF8A80),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
                else -> {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(source) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val next = (scale * zoom).coerceIn(1f, 5f)
                                    scale = next
                                    offset = if (next > 1f) offset + pan else Offset.Zero
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Cerrar",
                    tint = GreenMint
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }
}
