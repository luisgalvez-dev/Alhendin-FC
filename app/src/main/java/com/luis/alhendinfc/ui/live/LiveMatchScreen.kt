package com.luis.alhendinfc.ui.live

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.CustomStatAppliesTo
import com.luis.alhendinfc.domain.model.CustomStatType
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.matches.JerseyIcon
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.theme.GreenPitch
import com.luis.alhendinfc.ui.theme.TealSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Distancia relativa para considerar soltar “encima” de otro jugador. */
private const val SWAP_HIT_DISTANCE = 0.12f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMatchScreen(
    match: Match,
    team: Team?,
    ui: LiveMatchUiState,
    events: List<MatchEvent>,
    customStatTypes: List<CustomStatType>,
    playersOnField: List<Player>,
    playersOnBench: List<Player>,
    allPlayers: List<Player>,
    yellowCards: Map<Int, Int>,
    redCards: Map<Int, Int>,
    onToggleTimer: () -> Unit,
    onNextPeriod: () -> Unit,
    onAddEvent: (StatisticType, Int?) -> Unit,
    onAddCustomEvent: (typeCode: String, playerId: Int?) -> Unit,
    eventLabel: (MatchEvent) -> String,
    onSubstitution: (outId: Int, inId: Int) -> Unit,
    onMovePlayer: (playerId: Int, x: Float, y: Float) -> Unit,
    onSetShowJerseyNumbers: (Boolean) -> Unit,
    onSetShowStarterTime: (Boolean) -> Unit,
    onClearFeedback: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val teamName = team?.name ?: "Equipo"
    val rivalName = match.rival.ifBlank { "Rival" }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val rivalCustomTypes = remember(customStatTypes) {
        customStatTypes.filter { it.appliesTo == CustomStatAppliesTo.RIVAL }
    }

    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var showRivalSheet by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var showEvents by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var pendingSubOut by remember { mutableStateOf<Player?>(null) }
    var pendingSubIn by remember { mutableStateOf<Player?>(null) }
    var draggingBenchPlayer by remember { mutableStateOf<Player?>(null) }
    var exporting by remember { mutableStateOf(false) }

    fun exportMatchReport() {
        if (exporting) return
        exporting = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                MatchReportExporter(context).exportAll(
                    match = match,
                    team = team,
                    events = events,
                    players = allPlayers,
                    teamGoals = ui.teamGoals,
                    rivalGoals = ui.rivalGoals,
                    period = ui.period,
                    elapsedSeconds = ui.elapsedSeconds,
                    secondsOnField = ui.secondsOnField,
                    eventLabel = eventLabel
                )
            }
            exporting = false
            val uris = listOfNotNull(result.pdfUri, result.csvUri)
            if (uris.isEmpty()) {
                snackbarHostState.showSnackbar("No se pudo exportar el acta")
                return@launch
            }
            val share = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, "Acta $teamName vs $rivalName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Acta del partido $teamName ${ui.teamGoals}-${ui.rivalGoals} $rivalName (PDF + CSV para Excel/IA)."
                )
            }
            context.startActivity(Intent.createChooser(share, "Exportar acta del partido"))
            snackbarHostState.showSnackbar("Acta lista (PDF + CSV)")
        }
    }

    LaunchedEffect(ui.lastFeedback) {
        val msg = ui.lastFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onClearFeedback()
    }

    if (selectedPlayer != null) {
        val sp = selectedPlayer!!
        val yCount = yellowCards[sp.id] ?: 0
        val sentOff = (redCards[sp.id] ?: 0) > 0 || yCount >= 2
        PlayerActionsSheet(
            player = sp,
            yellowCount = yCount,
            canAddYellow = yCount < 2 && !sentOff,
            canAddRed = !sentOff,
            customStatTypes = customStatTypes.filter { it.appliesTo.matches(sp.position) },
            onAction = { type ->
                onAddEvent(type, sp.id)
                selectedPlayer = null
            },
            onCustomAction = { code ->
                onAddCustomEvent(code, sp.id)
                selectedPlayer = null
            },
            onDismiss = { selectedPlayer = null }
        )
    }

    if (showRivalSheet) {
        RivalActionsSheet(
            rivalName = rivalName,
            customStatTypes = rivalCustomTypes,
            onRivalGoal = {
                onAddEvent(StatisticType.RIVAL_GOAL, null)
                showRivalSheet = false
            },
            onCustomAction = { code ->
                onAddCustomEvent(code, null)
                showRivalSheet = false
            },
            onDismiss = { showRivalSheet = false }
        )
    }

    if (showOptions) {
        OptionsDialog(
            showJerseyNumbers = ui.showJerseyNumbers,
            showStarterTime = ui.showStarterTime,
            exporting = exporting,
            onShowJerseyNumbers = onSetShowJerseyNumbers,
            onShowStarterTime = onSetShowStarterTime,
            onExport = {
                showOptions = false
                exportMatchReport()
            },
            onFinish = { showOptions = false; showFinishDialog = true },
            onDismiss = { showOptions = false }
        )
    }

    if (showEvents) {
        EventsDialog(
            events = events,
            players = allPlayers,
            eventLabel = eventLabel,
            onUndo = onUndo,
            onDismiss = { showEvents = false }
        )
    }

    if (pendingSubOut != null && pendingSubIn != null) {
        val outP = pendingSubOut!!
        val inP = pendingSubIn!!
        AlertDialog(
            onDismissRequest = {
                pendingSubOut = null
                pendingSubIn = null
                draggingBenchPlayer = null
            },
            title = { Text("Confirmar cambio") },
            text = {
                Text(
                    "¿Saca a ${outP.alias.ifBlank { outP.name }} y entra ${inP.alias.ifBlank { inP.name }}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSubstitution(outP.id, inP.id)
                        pendingSubOut = null
                        pendingSubIn = null
                        draggingBenchPlayer = null
                    }
                ) { Text("Cambiar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingSubOut = null
                        pendingSubIn = null
                        draggingBenchPlayer = null
                    }
                ) { Text("Cancelar") }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finalizar partido") },
            text = {
                Text("Resultado ${ui.teamGoals}-${ui.rivalGoals}. ¿Terminar y guardar estadísticas?")
            },
            confirmButton = {
                TextButton(onClick = { showFinishDialog = false; onFinish() }) {
                    Text("Terminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Seguir") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A1F0C))) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                teamName = teamName,
                rivalName = rivalName,
                rivalShieldUri = match.rivalShieldUri,
                ui = ui,
                numParts = match.numParts,
                onBack = onBack,
                onToggleTimer = onToggleTimer,
                onNextPeriod = onNextPeriod,
                onOpenRival = { showRivalSheet = true },
                onOpenOptions = { showOptions = true },
                onOpenEvents = { showEvents = true }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                PitchWithPlayers(
                    players = playersOnField,
                    positions = ui.fieldPositions,
                    showNumbers = ui.showJerseyNumbers,
                    showTime = ui.showStarterTime,
                    secondsOnField = ui.secondsOnField,
                    yellowCards = yellowCards,
                    redCards = redCards,
                    highlightForSub = draggingBenchPlayer != null,
                    onPlayerClick = { selectedPlayer = it },
                    onPlayerMoved = onMovePlayer,
                    onDropBenchOntoPlayer = { fieldPlayer ->
                        val bench = draggingBenchPlayer
                        if (bench != null) {
                            pendingSubOut = fieldPlayer
                            pendingSubIn = bench
                        }
                    }
                )

                // FAB acciones rival
                FloatingActionButton(
                    onClick = { showRivalSheet = true },
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 88.dp)
                        .size(56.dp)
                ) {
                    Text("vs", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }

                FloatingActionButton(
                    onClick = { showEvents = true },
                    containerColor = GreenPitch,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Eventos del partido")
                }
            }

            // Banquillo + feedback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xCC121212))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.72f)) {
                    Text(
                        "BANQUILLO",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenMint,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        playersOnBench.forEach { player ->
                            BenchPlayerChip(
                                player = player,
                                showNumber = ui.showJerseyNumbers,
                                selected = draggingBenchPlayer?.id == player.id,
                                onClick = {
                                    draggingBenchPlayer =
                                        if (draggingBenchPlayer?.id == player.id) null else player
                                    if (draggingBenchPlayer != null) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Toca un titular en el campo para cambiar"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SnackbarHost(hostState = snackbarHostState)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    teamName: String,
    rivalName: String,
    rivalShieldUri: String?,
    ui: LiveMatchUiState,
    numParts: Int,
    onBack: () -> Unit,
    onToggleTimer: () -> Unit,
    onNextPeriod: () -> Unit,
    onOpenRival: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenEvents: () -> Unit
) {
    val mm = (ui.elapsedSeconds / 60).toString().padStart(2, '0')
    val ss = (ui.elapsedSeconds % 60).toString().padStart(2, '0')

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E2414))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }

        // Reloj + controles alineados en una sola columna centrada
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(148.dp)
        ) {
            Text(
                "$mm:$ss",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "Parte ${ui.period}/$numParts",
                style = MaterialTheme.typography.labelSmall,
                color = AmberAccent,
                textAlign = TextAlign.Center
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onToggleTimer),
                    contentAlignment = Alignment.Center
                ) {
                    if (ui.isRunning) {
                        Text("❚❚", color = AmberAccent, fontSize = 14.sp)
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = GreenAccent
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clickable(
                            enabled = ui.period < numParts,
                            onClick = onNextPeriod
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Parte+",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ui.period < numParts) GreenMint else Color.Gray
                    )
                }
            }
        }

        // Marcador central
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(40.dp))
                .border(1.dp, GreenAccent.copy(alpha = 0.4f), RoundedCornerShape(40.dp))
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    teamName,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    "  ${ui.teamGoals} - ${ui.rivalGoals}  ",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenLime
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenRival)
                        .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RivalShieldAvatar(
                            shieldUri = rivalShieldUri,
                            rivalName = rivalName,
                            size = 28
                        )
                        Text(
                            rivalName,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        "Tocar eventos",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        }

        IconButton(onClick = onOpenEvents) {
            Icon(Icons.Default.Info, contentDescription = "Estadísticas", tint = GreenMint)
        }
        IconButton(onClick = onOpenOptions) {
            Icon(Icons.Default.Menu, contentDescription = "Opciones", tint = Color.White)
        }
    }
}

@Composable
private fun RivalShieldAvatar(
    shieldUri: String?,
    rivalName: String,
    size: Int
) {
    val context = LocalContext.current
    var bitmap by remember(shieldUri) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }

    LaunchedEffect(shieldUri) {
        if (shieldUri.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(android.net.Uri.parse(shieldUri))?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                        ?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!,
                contentDescription = "Escudo $rivalName",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = rivalName.take(2).uppercase().ifBlank { "?" },
                fontSize = (size / 3).sp,
                fontWeight = FontWeight.Bold,
                color = AmberAccent
            )
        }
    }
}

@Composable
private fun PitchWithPlayers(
    players: List<Player>,
    positions: Map<Int, Offset>,
    showNumbers: Boolean,
    showTime: Boolean,
    secondsOnField: Map<Int, Int>,
    yellowCards: Map<Int, Int>,
    redCards: Map<Int, Int>,
    highlightForSub: Boolean,
    onPlayerClick: (Player) -> Unit,
    onPlayerMoved: (Int, Float, Float) -> Unit,
    onDropBenchOntoPlayer: (Player) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPitch(size)
        }

        players.forEach { player ->
            key(player.id) {
                FieldPlayerMarker(
                    player = player,
                    pos = positions[player.id]
                        ?: Offset(player.position.fieldX, player.position.fieldY),
                    allPositions = positions,
                    fieldW = w,
                    fieldH = h,
                    showNumbers = showNumbers,
                    showTime = showTime,
                    secondsOnField = secondsOnField[player.id] ?: 0,
                    yellowCount = yellowCards[player.id] ?: 0,
                    redCount = redCards[player.id] ?: 0,
                    highlightForSub = highlightForSub,
                    onPlayerClick = onPlayerClick,
                    onPlayerMoved = onPlayerMoved,
                    onDropBenchOntoPlayer = onDropBenchOntoPlayer
                )
            }
        }
    }
}

@Composable
private fun FieldPlayerMarker(
    player: Player,
    pos: Offset,
    allPositions: Map<Int, Offset>,
    fieldW: Float,
    fieldH: Float,
    showNumbers: Boolean,
    showTime: Boolean,
    secondsOnField: Int,
    yellowCount: Int,
    redCount: Int,
    highlightForSub: Boolean,
    onPlayerClick: (Player) -> Unit,
    onPlayerMoved: (Int, Float, Float) -> Unit,
    onDropBenchOntoPlayer: (Player) -> Unit
) {
    var displayPos by remember { mutableStateOf(pos) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOriginPos by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(pos, isDragging) {
        if (!isDragging) displayPos = pos
    }

    var markerW by remember { mutableFloatStateOf(72f) }
    var markerH by remember { mutableFloatStateOf(80f) }
    var suppressClick by remember { mutableStateOf(false) }

    val latestCommittedPos by rememberUpdatedState(pos)
    val latestAllPositions by rememberUpdatedState(allPositions)
    val latestW by rememberUpdatedState(fieldW)
    val latestH by rememberUpdatedState(fieldH)
    val latestHighlight by rememberUpdatedState(highlightForSub)
    val latestOnMoved by rememberUpdatedState(onPlayerMoved)
    val latestOnClick by rememberUpdatedState(onPlayerClick)
    val latestOnDrop by rememberUpdatedState(onDropBenchOntoPlayer)

    val hasRed = redCount > 0 || yellowCount >= 2
    val px = displayPos.x * fieldW
    val py = displayPos.y * fieldH
    val ghost = dragOriginPos

    // Fantasma gris en el hueco de origen + jugador arrastrado
    Box {
        if (isDragging && ghost != null) {
            val gx = ghost.x * fieldW
            val gy = ghost.y * fieldH
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (gx - markerW / 2f).roundToInt(),
                            (gy - markerH / 2f).roundToInt()
                        )
                    }
                    .zIndex(0f)
                    .alpha(0.55f)
                    .padding(4.dp)
            ) {
                JerseyIcon(
                    number = player.jerseyNumber,
                    status = CallupStatus.NONE,
                    size = 60.dp,
                    showNumber = showNumbers,
                    tint = Color(0xFF9E9E9E)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .onSizeChanged {
                    markerW = it.width.toFloat().coerceAtLeast(1f)
                    markerH = it.height.toFloat().coerceAtLeast(1f)
                }
                .offset {
                    IntOffset(
                        (px - markerW / 2f).roundToInt(),
                        (py - markerH / 2f).roundToInt()
                    )
                }
                .zIndex(if (isDragging) 10f else 1f)
                .pointerInput(player.id) {
                    var current = latestCommittedPos
                    var origin = latestCommittedPos
                    var moved = false
                    detectDragGestures(
                        onDragStart = {
                            origin = latestCommittedPos
                            current = origin
                            moved = false
                            dragOriginPos = origin
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            dragOriginPos = null
                            if (moved) {
                                suppressClick = true
                                val swapTarget = latestAllPositions
                                    .asSequence()
                                    .filter { it.key != player.id }
                                    .minByOrNull { (_, p) ->
                                        val dx = p.x - current.x
                                        val dy = p.y - current.y
                                        dx * dx + dy * dy
                                    }
                                    ?.takeIf { (_, p) ->
                                        val dx = p.x - current.x
                                        val dy = p.y - current.y
                                        dx * dx + dy * dy < SWAP_HIT_DISTANCE * SWAP_HIT_DISTANCE
                                    }
                                displayPos = swapTarget?.value ?: origin
                                latestOnMoved(player.id, current.x, current.y)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOriginPos = null
                            displayPos = origin
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            val w = latestW.coerceAtLeast(1f)
                            val h = latestH.coerceAtLeast(1f)
                            if (abs(amount.x) > 0.5f || abs(amount.y) > 0.5f) {
                                moved = true
                            }
                            current = Offset(
                                (current.x + amount.x / w).coerceIn(0.06f, 0.94f),
                                (current.y + amount.y / h).coerceIn(0.10f, 0.90f)
                            )
                            displayPos = current
                        }
                    )
                }
                .clickable {
                    if (suppressClick) {
                        suppressClick = false
                        return@clickable
                    }
                    if (latestHighlight) latestOnDrop(player)
                    else latestOnClick(player)
                }
                .then(
                    if (highlightForSub) Modifier.border(2.dp, GreenLime, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .padding(4.dp)
        ) {
        Box {
            JerseyIcon(
                number = player.jerseyNumber,
                status = CallupStatus.TITULAR,
                size = 60.dp,
                showNumber = showNumbers
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!hasRed) {
                    repeat(yellowCount.coerceAtMost(2)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp, 12.dp)
                                .background(AmberAccent, RoundedCornerShape(1.dp))
                        )
                    }
                }
                if (hasRed) {
                    Box(
                        modifier = Modifier
                            .size(12.dp, 16.dp)
                            .background(Color(0xFFFF5252), RoundedCornerShape(2.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
        Text(
            player.alias.ifBlank { player.name.split(" ").first() },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (hasRed) Color(0xFFB71C1C) else Color(0xFF0A0A0A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showTime) {
            Text(
                "%d'%02d".format(secondsOnField / 60, secondsOnField % 60),
                fontSize = 13.sp,
                color = if (hasRed) Color(0xFF616161) else Color(0xFF1A237E),
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPitch(size: Size) {
    val stripe = size.width / 12f
    for (i in 0 until 12) {
        drawRect(
            color = if (i % 2 == 0) Color(0xFF2E7D32) else Color(0xFF388E3C),
            topLeft = Offset(i * stripe, 0f),
            size = Size(stripe, size.height)
        )
    }
    val pad = size.width * 0.02f
    val stroke = Stroke(width = size.width * 0.004f)
    val white = Color.White.copy(alpha = 0.9f)
    drawRect(
        color = white,
        topLeft = Offset(pad, pad),
        size = Size(size.width - pad * 2, size.height - pad * 2),
        style = stroke
    )
    drawLine(white, Offset(size.width / 2, pad), Offset(size.width / 2, size.height - pad), size.width * 0.004f)
    drawCircle(white, size.height * 0.12f, Offset(size.width / 2, size.height / 2), style = stroke)
    drawRect(white, Offset(pad, size.height * 0.22f), Size(size.width * 0.16f, size.height * 0.56f), style = stroke)
    drawRect(
        white,
        Offset(size.width - pad - size.width * 0.16f, size.height * 0.22f),
        Size(size.width * 0.16f, size.height * 0.56f),
        style = stroke
    )
}

@Composable
private fun BenchPlayerChip(
    player: Player,
    showNumber: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, AmberAccent, RoundedCornerShape(8.dp))
                else Modifier
            )
            .padding(4.dp)
    ) {
        JerseyIcon(
            number = player.jerseyNumber,
            status = CallupStatus.SUPLENTE,
            size = 48.dp,
            showNumber = showNumber
        )
        Text(
            player.alias.ifBlank { player.name.split(" ").first() },
            fontSize = 11.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerActionsSheet(
    player: Player,
    yellowCount: Int,
    canAddYellow: Boolean,
    canAddRed: Boolean,
    customStatTypes: List<CustomStatType>,
    onAction: (StatisticType) -> Unit,
    onCustomAction: (typeCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A2A1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JerseyIcon(player.jerseyNumber, CallupStatus.TITULAR, 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        player.alias.ifBlank { player.name },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(player.position.label, color = GreenMint)
                    if (yellowCount > 0 || !canAddRed) {
                        Text(
                            when {
                                !canAddRed && yellowCount >= 2 -> "Expulsado (doble amarilla)"
                                !canAddRed -> "Expulsado (roja)"
                                else -> "Amarillas: $yellowCount/2"
                            },
                            color = if (!canAddRed) Color(0xFFFF5252) else AmberAccent,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Acciones", color = GreenMint, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionChip("GOL", GreenAccent, Modifier.weight(1f)) { onAction(StatisticType.GOAL) }
                ActionChip("ASIST.", GreenMint, Modifier.weight(1f)) { onAction(StatisticType.ASSIST) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionChip(
                    label = "AMARILLA",
                    color = AmberAccent,
                    modifier = Modifier.weight(1f),
                    enabled = canAddYellow
                ) { onAction(StatisticType.YELLOW_CARD) }
                ActionChip(
                    label = "ROJA",
                    color = Color(0xFFFF5252),
                    modifier = Modifier.weight(1f),
                    enabled = canAddRed
                ) { onAction(StatisticType.RED_CARD) }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Personalizadas", color = GreenMint, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            if (customStatTypes.isEmpty()) {
                Text(
                    "Sin tipos para esta posición. En Ajustes puedes crear p. ej. «Parada» solo para porteros.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                customStatTypes.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { type ->
                            ActionChip(
                                label = type.shortLabel.uppercase(),
                                color = TealSoft,
                                modifier = Modifier.weight(1f)
                            ) { onCustomAction(type.code) }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RivalActionsSheet(
    rivalName: String,
    customStatTypes: List<CustomStatType>,
    onRivalGoal: () -> Unit,
    onCustomAction: (typeCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A2A1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rivalName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text("Eventos del rival", color = AmberAccent)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Básicos", color = GreenMint, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            ActionChip(
                label = "GOL RIVAL",
                color = Color(0xFFFF5252),
                modifier = Modifier.fillMaxWidth()
            ) { onRivalGoal() }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Personalizadas", color = GreenMint, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            if (customStatTypes.isEmpty()) {
                Text(
                    "Crea tipos «Solo rival» en Ajustes (p. ej. Ataque por banda, Presión…).",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                customStatTypes.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { type ->
                            ActionChip(
                                label = type.shortLabel.uppercase(),
                                color = AmberAccent,
                                modifier = Modifier.weight(1f)
                            ) { onCustomAction(type.code) }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color,
            disabledContainerColor = Color.White.copy(alpha = 0.06f),
            disabledContentColor = Color.White.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OptionsDialog(
    showJerseyNumbers: Boolean,
    showStarterTime: Boolean,
    exporting: Boolean,
    onShowJerseyNumbers: (Boolean) -> Unit,
    onShowStarterTime: (Boolean) -> Unit,
    onExport: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OPCIONES", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OptionRow(
                    title = "Dorsales",
                    subtitle = "Mostrar/ocultar números en la camiseta",
                    checked = showJerseyNumbers,
                    onChecked = onShowJerseyNumbers
                )
                HorizontalDivider()
                OptionRow(
                    title = "Tiempo titular",
                    subtitle = "Mostrar minutos en campo bajo cada jugador",
                    checked = showStarterTime,
                    onChecked = onShowStarterTime
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onExport,
                    enabled = !exporting,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (exporting) "Exportando…" else "Exportar acta (PDF + CSV)",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Terminar partido", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun EventsDialog(
    events: List<MatchEvent>,
    players: List<Player>,
    eventLabel: (MatchEvent) -> String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    fun nameOf(id: Int?): String {
        if (id == null) return "—"
        val p = players.firstOrNull { it.id == id } ?: return "?"
        return p.alias.ifBlank { p.name.split(" ").first() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eventos del partido") },
        text = {
            if (events.isEmpty()) {
                Text("Todavía no hay eventos.")
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    events.asReversed().forEach { e ->
                        val detail = when (e.type) {
                            StatisticType.SUBSTITUTION -> "${nameOf(e.playerId)} → ${nameOf(e.relatedPlayerId)}"
                            StatisticType.RIVAL_GOAL -> "Rival"
                            else -> if (e.playerId == null) "Rival" else nameOf(e.playerId)
                        }
                        Text(
                            "${e.minute}' · ${eventLabel(e)} · $detail",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        dismissButton = {
            TextButton(onClick = onUndo, enabled = events.isNotEmpty()) { Text("Deshacer último") }
        }
    )
}
