package com.luis.alhendinfc.ui.pizarra

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.luis.alhendinfc.domain.model.BoardObject
import com.luis.alhendinfc.domain.model.BoardObjectType
import com.luis.alhendinfc.ui.util.LocalImageLoader
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.theme.GreenPitch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PizarraScreen(
    state: PizarraUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onTool: (BoardEditorTool) -> Unit,
    onColor: (Long) -> Unit,
    onStrokeWidth: (Float) -> Unit,
    onUseField: () -> Unit,
    onImagePicked: (Uri) -> Unit,
    onVideoPicked: (Uri) -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onPress: (BoardPoint) -> Unit,
    onDrag: (BoardPoint) -> Unit,
    onRelease: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onPendingText: (String?) -> Unit,
    onDeleteSelected: () -> Unit,
    onUpdateNumber: (String) -> Unit,
    onUpdateText: (String) -> Unit,
    onSavedConsumed: () -> Unit = {}
) {
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onImagePicked(uri) }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onVideoPicked(uri) }

    var askingText by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }
    var editingToken by remember { mutableStateOf(false) }
    var tokenValue by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbarHostState.showSnackbar("Pizarra guardada")
            onSavedConsumed()
        }
    }

    if (askingText) {
        AlertDialog(
            onDismissRequest = { askingText = false },
            title = { Text("Texto") },
            text = {
                OutlinedTextField(value = textValue, onValueChange = { textValue = it }, label = { Text("Texto") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    onPendingText(textValue)
                    onTool(BoardEditorTool.TEXT)
                    askingText = false
                }) { Text("Colocar") }
            },
            dismissButton = { TextButton(onClick = { askingText = false }) { Text("Cancelar") } }
        )
    }
    if (editingToken) {
        AlertDialog(
            onDismissRequest = { editingToken = false },
            title = { Text("Editar") },
            text = {
                OutlinedTextField(value = tokenValue, onValueChange = { tokenValue = it }, label = { Text("Número o texto") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val selected = state.objects.firstOrNull { it.objectId == state.selectedObjectId }
                    if (selected?.type == BoardObjectType.TEXT) onUpdateText(tokenValue) else onUpdateNumber(tokenValue)
                    editingToken = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { onDeleteSelected(); editingToken = false }) { Text("Eliminar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.boardName + if (state.dirty) " •" else "",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A2410),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF061408)
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PizarraToolbar(
                state = state,
                onTool = { tool ->
                    if (tool == BoardEditorTool.TEXT) {
                        textValue = state.pendingText.orEmpty()
                        askingText = true
                    } else onTool(tool)
                },
                onColor = onColor,
                onStrokeWidth = onStrokeWidth,
                onUseField = onUseField,
                onPickImage = { imagePicker.launch("image/*") },
                onPickVideo = { videoPicker.launch("video/*") },
                onTogglePlay = onTogglePlay,
                onSeekBack = onSeekBack,
                onSeekForward = onSeekForward,
                onUndo = onUndo,
                onClear = onClear,
                onEditSelected = {
                    val selected = state.objects.firstOrNull { it.objectId == state.selectedObjectId } ?: return@PizarraToolbar
                    tokenValue = selected.number ?: selected.text.orEmpty()
                    editingToken = true
                },
                onDeleteSelected = onDeleteSelected,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(132.dp)
            )

            BoardArea(
                state = state,
                onPress = onPress,
                onDrag = onDrag,
                onRelease = onRelease,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PizarraToolbar(
    state: PizarraUiState,
    onTool: (BoardEditorTool) -> Unit,
    onColor: (Long) -> Unit,
    onStrokeWidth: (Float) -> Unit,
    onUseField: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onEditSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0F2A14))
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionLabel("Fondo")
        ToolChip("Campo", selected = state.background == BoardBackground.FIELD, onClick = onUseField)
        ToolChip("Imagen", selected = state.background == BoardBackground.IMAGE, onClick = onPickImage)
        ToolChip("Vídeo", selected = state.background == BoardBackground.VIDEO, onClick = onPickVideo)

        if (state.background == BoardBackground.VIDEO && state.mediaUri != null) {
            ToolChip(
                label = if (state.isVideoPlaying) "Pausa" else "Play",
                selected = state.isVideoPlaying,
                onClick = onTogglePlay,
                accent = AmberAccent
            )
            ToolChip(label = "-10s", selected = false, onClick = onSeekBack, accent = GreenMint)
            ToolChip(label = "+10s", selected = false, onClick = onSeekForward, accent = GreenMint)
        }

        SectionLabel("Herramienta")
        ToolChip("Seleccionar", selected = state.tool == BoardEditorTool.SELECT, onClick = { onTool(BoardEditorTool.SELECT) })
        ToolChip(
            label = "Lápiz",
            selected = state.tool == BoardEditorTool.PEN,
            onClick = { onTool(BoardEditorTool.PEN) },
            icon = { Icon(Icons.Default.Edit, null, Modifier.size(16.dp), tint = Color.White) }
        )
        ToolChip(
            label = "Flecha",
            selected = state.tool == BoardEditorTool.ARROW,
            onClick = { onTool(BoardEditorTool.ARROW) }
        )
        ToolChip(
            label = "Óvalo",
            selected = state.tool == BoardEditorTool.OVAL,
            onClick = { onTool(BoardEditorTool.OVAL) }
        )
        ToolChip(
            label = "Borrador",
            selected = state.tool == BoardEditorTool.ERASER,
            onClick = { onTool(BoardEditorTool.ERASER) },
            icon = { Icon(Icons.Default.Clear, null, Modifier.size(16.dp), tint = Color.White) }
        )
        SectionLabel("Objetos")
        ToolChip("Azul", selected = state.tool == BoardEditorTool.BLUE_PLAYER, onClick = { onTool(BoardEditorTool.BLUE_PLAYER) })
        ToolChip("Rojo", selected = state.tool == BoardEditorTool.RED_PLAYER, onClick = { onTool(BoardEditorTool.RED_PLAYER) })
        ToolChip("Balón", selected = state.tool == BoardEditorTool.BALL, onClick = { onTool(BoardEditorTool.BALL) })
        ToolChip("Cono", selected = state.tool == BoardEditorTool.CONE, onClick = { onTool(BoardEditorTool.CONE) })
        ToolChip("Portería", selected = state.tool == BoardEditorTool.GOAL, onClick = { onTool(BoardEditorTool.GOAL) })
        ToolChip("Mini", selected = state.tool == BoardEditorTool.MINI_GOAL, onClick = { onTool(BoardEditorTool.MINI_GOAL) })
        ToolChip("Texto", selected = state.tool == BoardEditorTool.TEXT, onClick = { onTool(BoardEditorTool.TEXT) })
        if (state.selectedObjectId != null) {
            ToolChip("Editar ficha", selected = false, onClick = onEditSelected, accent = AmberAccent)
            ToolChip("Quitar ficha", selected = false, onClick = onDeleteSelected, accent = Color(0xFFFF6B7A))
        }

        SectionLabel("Grosor")
        listOf(4f to "Fino", 8f to "Medio", 14f to "Grueso").forEach { (w, label) ->
            ToolChip(
                label = label,
                selected = state.strokeWidth == w,
                onClick = { onStrokeWidth(w) }
            )
        }

        SectionLabel("Color")
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                PizarraColors.WHITE,
                PizarraColors.YELLOW,
                PizarraColors.RED,
                PizarraColors.BLUE,
                PizarraColors.BLACK
            ).forEach { argb ->
                val selected = state.colorArgb == argb
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(argb.toComposeColor())
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) GreenLime else Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable { onColor(argb) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        ToolChip("Deshacer", selected = false, onClick = onUndo, accent = GreenMint)
        ToolChip(
            label = "Limpiar",
            selected = false,
            onClick = onClear,
            accent = Color(0xFFFF6B7A),
            icon = { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = Color.White) }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = GreenAccent,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color = GreenAccent,
    icon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = 1.dp,
                color = if (selected) accent else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun BoardArea(
    state: PizarraUiState,
    onPress: (BoardPoint) -> Unit,
    onDrag: (BoardPoint) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(state.mediaUri, state.background) {
        if (state.background == BoardBackground.IMAGE && state.mediaUri != null) {
            imageBitmap = LocalImageLoader.load(
                context,
                state.mediaUri.toString(),
                maxSidePx = 1280
            )
        } else {
            imageBitmap = null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A1A0C))
    ) {
        when (state.background) {
            BoardBackground.FIELD -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawFootballPitch()
                }
            }
            BoardBackground.IMAGE -> {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Imagen de pizarra",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PlaceholderMessage("Elige una imagen de la galería")
                }
            }
            BoardBackground.VIDEO -> {
                if (state.mediaUri != null) {
                    VideoBackground(
                        uri = state.mediaUri,
                        playing = state.isVideoPlaying,
                        seekPulse = state.videoSeekPulse,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PlaceholderMessage("Elige un vídeo de la galería")
                }
            }
        }

        DrawingOverlay(
            objects = state.objects,
            currentStroke = state.currentStroke,
            selectedObjectId = state.selectedObjectId,
            onPress = onPress,
            onDrag = onDrag,
            onRelease = onRelease,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PlaceholderMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White.copy(alpha = 0.7f))
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoBackground(
    uri: Uri,
    playing: Boolean,
    seekPulse: VideoSeekPulse?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
        }
    }

    DisposableEffect(uri) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(playing) {
        exoPlayer.playWhenReady = playing
        if (playing) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(seekPulse?.id) {
        val pulse = seekPulse ?: return@LaunchedEffect
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val target = (exoPlayer.currentPosition + pulse.deltaMs).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        exoPlayer.seekTo(target)
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            view.player = exoPlayer
        },
        modifier = modifier
    )
}

@Composable
private fun DrawingOverlay(
    objects: List<BoardObject>,
    currentStroke: DrawStroke?,
    selectedObjectId: String?,
    onPress: (BoardPoint) -> Unit,
    onDrag: (BoardPoint) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    fun norm(offset: Offset) = BoardPoint(offset.x / w, offset.y / h)
                    onPress(norm(down.position))
                    val slop = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                    }
                    if (slop != null) {
                        onDrag(norm(slop.position))
                        drag(slop.id) { change ->
                            change.consume()
                            onDrag(norm(change.position))
                        }
                    }
                    onRelease()
                }
            }
    ) {
        objects.forEach { drawBoardObject(it, selected = it.objectId == selectedObjectId) }
        currentStroke?.let { drawStrokeNormalized(it) }
    }
}

private fun DrawScope.drawStrokeNormalized(stroke: DrawStroke) {
    val color = stroke.colorArgb.toComposeColor()
    val px = stroke.points.map { BoardPoint(it.x * size.width, it.y * size.height) }
    val width = strokeWidthPx(stroke.width)
    val pixelStroke = stroke.copy(points = px, width = width)
    drawStroke(pixelStroke)
}

private fun DrawScope.strokeWidthPx(stored: Float): Float =
    (stored / 400f) * minOf(size.width, size.height)

private fun DrawScope.drawBoardObject(obj: BoardObject, selected: Boolean) {
    val cx = obj.x * size.width
    val cy = obj.y * size.height
    val min = minOf(size.width, size.height)
    val r = min * 0.028f
    val selectedRing = if (selected) Color.White.copy(alpha = 0.85f) else null
    when (obj.type) {
        BoardObjectType.BLUE_PLAYER, BoardObjectType.RED_PLAYER -> {
            val fill = if (obj.type == BoardObjectType.BLUE_PLAYER) Color(0xFF40C4FF) else Color(0xFFFF5252)
            drawCircle(fill, r, Offset(cx, cy))
            selectedRing?.let { drawCircle(it, r + 4f, Offset(cx, cy), style = Stroke(width = 3f)) }
            val number = obj.number.orEmpty()
            if (number.isNotBlank()) {
                drawContext.canvas.nativeCanvas.drawText(
                    number,
                    cx,
                    cy + r * 0.35f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = r * 0.9f
                        isFakeBoldText = true
                    }
                )
            }
        }
        BoardObjectType.BALL -> {
            val ballR = min * 0.015f
            drawFootball(Offset(cx, cy), ballR)
            selectedRing?.let { drawCircle(it, ballR + 4f, Offset(cx, cy), style = Stroke(width = 2.5f)) }
        }
        BoardObjectType.CONE -> {
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx - r * 0.7f, cy + r * 0.7f)
                lineTo(cx + r * 0.7f, cy + r * 0.7f)
                close()
            }
            drawPath(path, Color(0xFFFF9800))
            selectedRing?.let { drawCircle(it, r + 4f, Offset(cx, cy), style = Stroke(width = 3f)) }
        }
        BoardObjectType.GOAL, BoardObjectType.MINI_GOAL -> {
            val isFull = obj.type == BoardObjectType.GOAL
            val w = if (isFull) min * 0.16f else min * 0.09f
            val h = if (isFull) min * 0.14f else min * 0.08f
            drawFootballGoal(Offset(cx, cy), w, h)
            selectedRing?.let {
                drawCircle(it, maxOf(w, h) * 0.72f, Offset(cx, cy), style = Stroke(width = 3f))
            }
        }
        BoardObjectType.TEXT -> {
            val label = obj.text.orEmpty()
            if (label.isNotBlank()) {
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    cx,
                    cy,
                    android.graphics.Paint().apply {
                        color = (obj.colorArgb ?: PizarraColors.WHITE).toInt()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = min * 0.035f
                        isFakeBoldText = true
                    }
                )
            }
            selectedRing?.let { drawCircle(it, r + 6f, Offset(cx, cy), style = Stroke(width = 3f)) }
        }
        BoardObjectType.PATH, BoardObjectType.ARROW, BoardObjectType.OVAL, BoardObjectType.ERASER -> {
            val tool = when (obj.type) {
                BoardObjectType.ARROW -> DrawTool.ARROW
                BoardObjectType.OVAL -> DrawTool.OVAL
                BoardObjectType.ERASER -> DrawTool.ERASER
                else -> DrawTool.PEN
            }
            drawStrokeNormalized(
                DrawStroke(
                    tool = tool,
                    colorArgb = obj.colorArgb ?: PizarraColors.WHITE,
                    width = obj.width ?: 8f,
                    points = obj.points.map { BoardPoint(it.x, it.y) }
                )
            )
        }
    }
}

private fun DrawScope.drawFootballGoal(center: Offset, frontW: Float, frontH: Float) {
    val depthX = frontW * 0.42f
    val depthY = frontH * 0.22f
    val backScale = 0.74f
    fun backOf(front: Offset): Offset {
        val from = Offset(front.x - center.x, front.y - center.y)
        return Offset(
            center.x + from.x * backScale + depthX,
            center.y + from.y * backScale - depthY
        )
    }
    val fl = Offset(center.x - frontW / 2f, center.y - frontH / 2f)
    val fr = Offset(center.x + frontW / 2f, center.y - frontH / 2f)
    val bl = Offset(center.x - frontW / 2f, center.y + frontH / 2f)
    val br = Offset(center.x + frontW / 2f, center.y + frontH / 2f)
    val bfl = backOf(fl)
    val bfr = backOf(fr)
    val bbl = backOf(bl)
    val bbr = backOf(br)

    val frame = Color.White
    val net = Color.White.copy(alpha = 0.78f)
    val frameW = (minOf(frontW, frontH) * 0.055f).coerceAtLeast(2.6f)
    val netW = (minOf(frontW, frontH) * 0.012f).coerceAtLeast(0.8f)

    drawQuadNet(fl, fr, bfr, bfl, 5, 4, net, netW)
    drawQuadNet(bfl, bfr, bbr, bbl, 5, 6, net, netW)
    drawQuadNet(fl, bfl, bbl, bl, 4, 6, net, netW)
    drawQuadNet(fr, bfr, bbr, br, 4, 6, net, netW)

    drawLine(frame, fl, fr, frameW, StrokeCap.Round)
    drawLine(frame, fl, bl, frameW, StrokeCap.Round)
    drawLine(frame, fr, br, frameW, StrokeCap.Round)
    drawLine(frame, bfl, bfr, frameW * 0.85f, StrokeCap.Round)
    drawLine(frame, bbl, bbr, frameW * 0.85f, StrokeCap.Round)
    drawLine(frame, bfl, bbl, frameW * 0.85f, StrokeCap.Round)
    drawLine(frame, bfr, bbr, frameW * 0.85f, StrokeCap.Round)
    drawLine(frame, fl, bfl, frameW * 0.9f, StrokeCap.Round)
    drawLine(frame, fr, bfr, frameW * 0.9f, StrokeCap.Round)
    drawLine(frame, bl, bbl, frameW * 0.9f, StrokeCap.Round)
    drawLine(frame, br, bbr, frameW * 0.9f, StrokeCap.Round)
}

private fun DrawScope.drawQuadNet(
    a: Offset,
    b: Offset,
    c: Offset,
    d: Offset,
    linesU: Int,
    linesV: Int,
    color: Color,
    stroke: Float
) {
    val clip = Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        lineTo(d.x, d.y)
        close()
    }
    clipPath(clip) {
        for (i in 1 until linesU) {
            val t = i / linesU.toFloat()
            drawLine(color, lerpOffset(a, b, t), lerpOffset(d, c, t), stroke)
        }
        for (j in 1 until linesV) {
            val t = j / linesV.toFloat()
            drawLine(color, lerpOffset(a, d, t), lerpOffset(b, c, t), stroke)
        }
    }
}

private fun lerpOffset(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun DrawScope.drawFootball(center: Offset, radius: Float) {
    val leather = Color(0xFFF7F5F0)
    val patch = Color(0xFF111111)
    val seamWidth = (radius * 0.08f).coerceAtLeast(1.1f)
    val outline = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }

    drawCircle(leather, radius, center)
    drawCircle(Color.White, radius, center, style = Stroke(width = seamWidth * 0.7f))

    val pentagonR = radius * 0.32f
    val vertexAngles = FloatArray(5) { i -> (-PI / 2.0 + i * 2.0 * PI / 5.0).toFloat() }
    drawPath(regularPolygon(center, pentagonR, vertexAngles[0], 5), patch)

    clipPath(outline) {
        vertexAngles.forEach { angle ->
            val outerCenter = Offset(
                center.x + radius * 0.86f * cos(angle),
                center.y + radius * 0.86f * sin(angle)
            )
            drawPath(regularPolygon(outerCenter, radius * 0.26f, angle, 5), patch)
        }
    }

    vertexAngles.forEach { angle ->
        val vx = center.x + pentagonR * cos(angle)
        val vy = center.y + pentagonR * sin(angle)
        val left = angle - 0.36f
        val right = angle + 0.36f
        val rim = radius * 0.92f
        drawLine(patch, Offset(vx, vy), Offset(center.x + rim * cos(left), center.y + rim * sin(left)), seamWidth, StrokeCap.Round)
        drawLine(patch, Offset(vx, vy), Offset(center.x + rim * cos(right), center.y + rim * sin(right)), seamWidth, StrokeCap.Round)
        val mid = angle + (2.0 * PI / 5.0).toFloat() / 2f
        drawLine(
            patch,
            Offset(center.x + rim * cos(right), center.y + rim * sin(right)),
            Offset(center.x + radius * 0.70f * cos(mid), center.y + radius * 0.70f * sin(mid)),
            seamWidth * 0.85f,
            StrokeCap.Round
        )
    }
    drawCircle(patch, radius, center, style = Stroke(width = seamWidth * 0.9f))
}

private fun regularPolygon(center: Offset, radius: Float, rotation: Float, sides: Int): Path {
    val path = Path()
    val step = (2.0 * PI / sides).toFloat()
    for (i in 0 until sides) {
        val a = rotation + i * step
        val x = center.x + radius * cos(a)
        val y = center.y + radius * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.drawStroke(stroke: DrawStroke) {
    val color = stroke.colorArgb.toComposeColor()
    when (stroke.tool) {
        DrawTool.PEN -> drawFreehand(stroke.points, color, stroke.width, BlendMode.SrcOver)
        DrawTool.ERASER -> drawFreehand(stroke.points, Color.Black, stroke.width * 1.4f, BlendMode.Clear)
        DrawTool.ARROW -> {
            if (stroke.points.size >= 2) {
                val start = stroke.points.first().toOffset()
                val end = stroke.points.last().toOffset()
                drawArrow(start, end, color, stroke.width)
            }
        }
        DrawTool.OVAL -> {
            if (stroke.points.size >= 2) {
                val a = stroke.points.first()
                val b = stroke.points.last()
                val left = minOf(a.x, b.x)
                val top = minOf(a.y, b.y)
                val w = kotlin.math.abs(a.x - b.x)
                val h = kotlin.math.abs(a.y - b.y)
                drawOval(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    style = Stroke(width = stroke.width, cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun DrawScope.drawFreehand(
    points: List<BoardPoint>,
    color: Color,
    width: Float,
    blendMode: BlendMode
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(
            color = color,
            radius = width / 2f,
            center = points.first().toOffset(),
            blendMode = blendMode
        )
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        blendMode = blendMode
    )
}

private fun DrawScope.drawArrow(start: Offset, end: Offset, color: Color, width: Float) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = width,
        cap = StrokeCap.Round
    )
    val angle = atan2(end.y - start.y, end.x - start.x)
    val headLen = width * 4.5f
    val left = Offset(
        end.x - headLen * cos(angle - 0.45f),
        end.y - headLen * sin(angle - 0.45f)
    )
    val right = Offset(
        end.x - headLen * cos(angle + 0.45f),
        end.y - headLen * sin(angle + 0.45f)
    )
    drawLine(color, end, left, width, StrokeCap.Round)
    drawLine(color, end, right, width, StrokeCap.Round)
}

private fun BoardPoint.toOffset() = Offset(x, y)

private fun Long.toComposeColor(): Color = Color(this.toInt())

private fun DrawScope.drawFootballPitch() {
    val fieldGreen = GreenPitch
    val stripe = Color(0xFF267A42)
    val lineWhite = Color.White.copy(alpha = 0.92f)
    val w = size.width
    val h = size.height

    drawRect(color = fieldGreen)
    val stripes = 10
    for (i in 0 until stripes) {
        if (i % 2 == 0) {
            drawRect(
                color = stripe.copy(alpha = 0.35f),
                topLeft = Offset(0f, h * i / stripes),
                size = Size(w, h / stripes)
            )
        }
    }

    val pad = minOf(w, h) * 0.03f
    val stroke = Stroke(width = minOf(w, h) * 0.006f)
    drawRect(
        color = lineWhite,
        topLeft = Offset(pad, pad),
        size = Size(w - pad * 2, h - pad * 2),
        style = stroke
    )
    drawLine(lineWhite, Offset(w / 2f, pad), Offset(w / 2f, h - pad), stroke.width)
    val centerR = minOf(w, h) * 0.1f
    drawCircle(lineWhite, centerR, Offset(w / 2f, h / 2f), style = stroke)
    drawCircle(lineWhite, stroke.width, Offset(w / 2f, h / 2f))

    fun drawBox(leftSide: Boolean) {
        val boxW = w * 0.16f
        val boxH = h * 0.44f
        val left = if (leftSide) pad else w - pad - boxW
        val top = (h - boxH) / 2f
        drawRect(lineWhite, Offset(left, top), Size(boxW, boxH), style = stroke)
        val sixW = w * 0.06f
        val sixH = h * 0.22f
        val sixLeft = if (leftSide) pad else w - pad - sixW
        val sixTop = (h - sixH) / 2f
        drawRect(lineWhite, Offset(sixLeft, sixTop), Size(sixW, sixH), style = stroke)
        val spotX = if (leftSide) pad + w * 0.11f else w - pad - w * 0.11f
        drawCircle(lineWhite, stroke.width * 1.2f, Offset(spotX, h / 2f))
    }
    drawBox(true)
    drawBox(false)

    // Esquinas
    val cornerR = minOf(w, h) * 0.035f
    drawArc(lineWhite, 0f, 90f, false, Offset(pad - cornerR, pad - cornerR), Size(cornerR * 2, cornerR * 2), style = stroke)
    drawArc(lineWhite, 90f, 90f, false, Offset(w - pad - cornerR, pad - cornerR), Size(cornerR * 2, cornerR * 2), style = stroke)
    drawArc(lineWhite, 180f, 90f, false, Offset(w - pad - cornerR, h - pad - cornerR), Size(cornerR * 2, cornerR * 2), style = stroke)
    drawArc(lineWhite, 270f, 90f, false, Offset(pad - cornerR, h - pad - cornerR), Size(cornerR * 2, cornerR * 2), style = stroke)
}
