package com.luis.alhendinfc.ui.matches

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.Team

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    match: Match,
    matchPlayers: List<MatchPlayer>,
    teamPlayers: List<Player>,
    team: Team?,
    onSave: (Match) -> Unit,
    onPlayerCallup: (playerId: Int, status: CallupStatus) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var rival by rememberSaveable(match.id) { mutableStateOf(match.rival) }
    var stadium by rememberSaveable(match.id) { mutableStateOf(match.stadium) }
    var date by rememberSaveable(match.id) { mutableStateOf(match.date) }
    var time by rememberSaveable(match.id) { mutableStateOf(match.time) }
    var matchday by rememberSaveable(match.id) { mutableStateOf(match.matchday.toString()) }
    var isHome by rememberSaveable(match.id) { mutableStateOf(match.isHome) }
    var durationPerPart by rememberSaveable(match.id) { mutableStateOf(match.durationPerPart.toString()) }
    var numParts by rememberSaveable(match.id) { mutableStateOf(match.numParts.toString()) }
    var formation by rememberSaveable(match.id) { mutableStateOf(match.formation) }
    var notes by rememberSaveable(match.id) { mutableStateOf(match.notes) }

    // Mode: TITULAR or SUPLENTE for jersey assignment
    var activeMode by remember { mutableStateOf(CallupStatus.TITULAR) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Map playerId → callup status for quick lookup
    val callupMap: Map<Int, CallupStatus> = remember(matchPlayers) {
        matchPlayers.associate { it.playerId to it.callupStatus }
    }

    val titulares = teamPlayers.filter { callupMap[it.id] == CallupStatus.TITULAR }
    val suplentes = teamPlayers.filter { callupMap[it.id] == CallupStatus.SUPLENTE }

    fun buildCurrentMatch() = match.copy(
        rival = rival,
        stadium = stadium,
        date = date,
        time = time,
        matchday = matchday.toIntOrNull() ?: 1,
        isHome = isHome,
        durationPerPart = durationPerPart.toIntOrNull() ?: 45,
        numParts = numParts.toIntOrNull() ?: 2,
        formation = formation,
        notes = notes
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar partido") },
            text = { Text("¿Seguro que quieres eliminar este partido? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (rival.isBlank()) "Nuevo partido" else "vs ${rival}",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onSave(buildCurrentMatch()); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Guardar y volver")
                    }
                },
                actions = {
                    // Mode buttons: green jersey = titular, yellow jersey = suplente
                    JerseyModeButton(
                        mode = CallupStatus.TITULAR,
                        isActive = activeMode == CallupStatus.TITULAR,
                        count = titulares.size,
                        onClick = { activeMode = CallupStatus.TITULAR }
                    )
                    Spacer(Modifier.width(4.dp))
                    JerseyModeButton(
                        mode = CallupStatus.SUPLENTE,
                        isActive = activeMode == CallupStatus.SUPLENTE,
                        count = suplentes.size,
                        onClick = { activeMode = CallupStatus.SUPLENTE }
                    )
                    Spacer(Modifier.width(8.dp))
                    // Export PDF
                    IconButton(
                        onClick = {
                            onSave(buildCurrentMatch())
                            exportConvocatoriaPdf(
                                context = context,
                                match = buildCurrentMatch(),
                                team = team,
                                players = teamPlayers,
                                callupMap = callupMap
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Exportar PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Left panel: player grid
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                // Mode indicator bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (activeMode == CallupStatus.TITULAR)
                                Color(0xFF1B5E20).copy(alpha = 0.12f)
                            else
                                Color(0xFFF57F17).copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeMode == CallupStatus.TITULAR)
                            "Toca jugadores → Titulares (${titulares.size})"
                        else
                            "Toca jugadores → Suplentes (${suplentes.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (activeMode == CallupStatus.TITULAR) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                    Text(
                        text = "${teamPlayers.filter { callupMap[it.id] == CallupStatus.NONE || callupMap[it.id] == null }.size} sin convocar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                if (teamPlayers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay jugadores en la plantilla",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(teamPlayers, key = { it.id }) { player ->
                            val currentStatus = callupMap[player.id] ?: CallupStatus.NONE
                            CallupPlayerCard(
                                player = player,
                                status = currentStatus,
                                activeMode = activeMode,
                                onClick = {
                                    val newStatus = when {
                                        currentStatus == activeMode -> CallupStatus.NONE
                                        else -> activeMode
                                    }
                                    onPlayerCallup(player.id, newStatus)
                                }
                            )
                        }
                    }
                }
            }

            // Vertical separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Right panel: match info form
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Información del partido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Local / Visitante selector
                FormSectionLabel("¿Dónde se juega?")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToggleOptionButton(
                        label = "Local",
                        selected = isHome,
                        onClick = { isHome = true },
                        modifier = Modifier.weight(1f)
                    )
                    ToggleOptionButton(
                        label = "Visitante",
                        selected = !isHome,
                        onClick = { isHome = false },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = rival,
                    onValueChange = { rival = it },
                    label = { Text("Rival") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Fecha") },
                        placeholder = { Text("dd/mm/aaaa") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Hora") },
                        placeholder = { Text("hh:mm") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                OutlinedTextField(
                    value = stadium,
                    onValueChange = { stadium = it },
                    label = { Text("Estadio / Campo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = matchday,
                        onValueChange = { matchday = it.filter { c -> c.isDigit() } },
                        label = { Text("Jornada") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedTextField(
                        value = durationPerPart,
                        onValueChange = { durationPerPart = it.filter { c -> c.isDigit() } },
                        label = { Text("Min./parte") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedTextField(
                        value = numParts,
                        onValueChange = { numParts = it.filter { c -> c.isDigit() } },
                        label = { Text("Partes") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                // Auto counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConvocadosCounter(
                        label = "Titulares",
                        count = titulares.size,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    ConvocadosCounter(
                        label = "Suplentes",
                        count = suplentes.size,
                        color = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = formation,
                    onValueChange = { formation = it },
                    label = { Text("Alineación") },
                    placeholder = { Text("4-3-3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(Modifier.height(4.dp))

                // CONTINUAR (placeholder for future live match)
                androidx.compose.material3.Button(
                    onClick = { onSave(buildCurrentMatch()) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("GUARDAR PARTIDO", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar partido")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun JerseyModeButton(
    mode: CallupStatus,
    isActive: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    val jerseyColor = if (mode == CallupStatus.TITULAR) Color(0xFF2E7D32) else Color(0xFFF9A825)
    val bgColor = if (isActive) jerseyColor.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (isActive) jerseyColor else Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            JerseyIcon(
                number = count,
                status = mode,
                size = 26.dp
            )
            Text(
                text = if (mode == CallupStatus.TITULAR) "Titulares" else "Suplentes",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) jerseyColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CallupPlayerCard(
    player: Player,
    status: CallupStatus,
    activeMode: CallupStatus,
    onClick: () -> Unit
) {
    val isConvoked = status != CallupStatus.NONE
    val dimAlpha = if (!isConvoked && activeMode != status) 0.6f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(dimAlpha),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isConvoked) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                CallupStatus.TITULAR -> Color(0xFF1B5E20).copy(alpha = 0.08f)
                CallupStatus.SUPLENTE -> Color(0xFFF57F17).copy(alpha = 0.08f)
                CallupStatus.NONE -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isConvoked) BorderStroke(
            1.5.dp,
            if (status == CallupStatus.TITULAR) Color(0xFF2E7D32) else Color(0xFFF9A825)
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            JerseyIcon(
                number = player.jerseyNumber,
                status = status,
                size = 52.dp
            )
            Text(
                text = player.alias.ifBlank { player.name.split(" ").first() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = player.position.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ToggleOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp)
        ) { Text(label, fontWeight = FontWeight.Bold) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp)
        ) { Text(label) }
    }
}

@Composable
private fun ConvocadosCounter(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

private fun exportConvocatoriaPdf(
    context: Context,
    match: Match,
    team: Team?,
    players: List<Player>,
    callupMap: Map<Int, CallupStatus>
) {
    try {
        val pdfUri = ConvocatoriaPdfExporter(context).export(
            match = match,
            team = team,
            players = players,
            callupMap = callupMap
        )
        if (pdfUri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir convocatoria"))
        }
    } catch (_: Exception) {
        // Silent fail: FileProvider not yet configured or other error
    }
}
