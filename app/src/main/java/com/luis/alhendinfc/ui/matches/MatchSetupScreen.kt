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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.luis.alhendinfc.domain.model.EventLabels
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.Formation
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.live.MatchReportExporter
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.theme.GreenPitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    match: Match,
    matchPlayers: List<MatchPlayer>,
    teamPlayers: List<Player>,
    team: Team?,
    fixtures: List<FixtureRow> = emptyList(),
    matchEvents: List<MatchEvent> = emptyList(),
    eventLabel: (MatchEvent) -> String = { EventLabels.resolve(it) },
    onSave: (Match) -> Unit,
    onPlayerCallup: (playerId: Int, status: CallupStatus) -> Unit,
    onDelete: () -> Unit,
    onContinue: (Match) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var rival by rememberSaveable(match.id) { mutableStateOf(match.rival) }
    var stadium by rememberSaveable(match.id) { mutableStateOf(match.stadium) }
    var date by rememberSaveable(match.id) { mutableStateOf(match.date) }
    var time by rememberSaveable(match.id) { mutableStateOf(match.time) }
    var matchday by rememberSaveable(match.id) { mutableStateOf(match.matchday.toString()) }
    var isHome by rememberSaveable(match.id) { mutableStateOf(match.isHome) }
    var durationPerPart by rememberSaveable(match.id) { mutableStateOf(match.durationPerPart.toString()) }
    var numParts by rememberSaveable(match.id) { mutableStateOf(match.numParts.toString()) }
    var formation by rememberSaveable(match.id) {
        mutableStateOf(match.formation.ifBlank { Formation.F_4_3_3.label })
    }
    var notes by rememberSaveable(match.id) { mutableStateOf(match.notes) }
    var opponentClubId by remember(match.id) { mutableStateOf(match.opponentClubId) }
    var rivalShieldUri by remember(match.id) { mutableStateOf(match.rivalShieldUri) }

    var activeMode by remember { mutableStateOf(CallupStatus.TITULAR) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var formationExpanded by remember { mutableStateOf(false) }
    var lastAutoFilledMatchday by rememberSaveable(match.id) {
        mutableStateOf(if (match.rival.isNotBlank()) match.matchday else -1)
    }

    // Auto-rellenar desde calendario al cambiar jornada (si hay fixture)
    LaunchedEffect(matchday, fixtures) {
        val day = matchday.toIntOrNull() ?: return@LaunchedEffect
        if (day == lastAutoFilledMatchday) return@LaunchedEffect
        val row = fixtures.firstOrNull { it.fixture.matchday == day } ?: return@LaunchedEffect
        val club = row.club
        rival = club?.name.orEmpty()
        stadium = row.stadium
        date = row.fixture.date
        time = row.fixture.time.ifBlank { time }
        isHome = row.fixture.isHome
        opponentClubId = club?.id
        rivalShieldUri = club?.shieldUri
        lastAutoFilledMatchday = day
    }

    // Map playerId → callup status for quick lookup
    val callupMap: Map<Int, CallupStatus> = remember(matchPlayers) {
        matchPlayers.associate { it.playerId to it.callupStatus }
    }

    val titulares = teamPlayers.filter { callupMap[it.id] == CallupStatus.TITULAR }
    val suplentes = teamPlayers.filter { callupMap[it.id] == CallupStatus.SUPLENTE }
    val selectedFormation = Formation.fromLabel(formation)
    val titularesOk = titulares.size == 11

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
        notes = notes,
        opponentClubId = opponentClubId,
        rivalShieldUri = rivalShieldUri
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateToMillis(date) ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { date = formatDate(it) }
                        showDatePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val parsed = parseTime(time)
        val timePickerState = rememberTimePickerState(
            initialHour = parsed.first,
            initialMinute = parsed.second,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        time = formatTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            },
            title = { Text("Hora del partido") },
            text = { TimePicker(state = timePickerState) }
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
                    if (match.status == MatchStatus.FINISHED) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    exportConvocatoriaAndActa(
                                        context = context,
                                        match = buildCurrentMatch(),
                                        team = team,
                                        players = teamPlayers,
                                        callupMap = callupMap,
                                        events = matchEvents,
                                        eventLabel = eventLabel,
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Exportar convocatoria y acta",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF12351A)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0E2A14), Color(0xFF0A1F0E))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (activeMode == CallupStatus.TITULAR)
                                GreenAccent.copy(alpha = 0.22f)
                            else
                                AmberAccent.copy(alpha = 0.22f)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        color = if (activeMode == CallupStatus.TITULAR) GreenLime else AmberAccent
                    )
                    Text(
                        text = "${teamPlayers.filter { callupMap[it.id] == CallupStatus.NONE || callupMap[it.id] == null }.size} sin convocar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
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
                                    if (newStatus == CallupStatus.TITULAR &&
                                        currentStatus != CallupStatus.TITULAR &&
                                        titulares.size >= 11
                                    ) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Ya hay 11 titulares")
                                        }
                                    } else {
                                        onPlayerCallup(player.id, newStatus)
                                    }
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
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF122B18), Color(0xFF0C1E12))
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Información del partido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GreenMint
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
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = date.ifBlank { "Elige fecha" },
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Fecha") },
                            singleLine = true,
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha")
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = GreenMint,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                    Box(modifier = Modifier.weight(0.6f)) {
                        OutlinedTextField(
                            value = time.ifBlank { "Elige hora" },
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Hora") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConvocadosCounter(
                        label = "Titulares",
                        count = titulares.size,
                        color = if (titularesOk) GreenAccent else Color(0xFFFF6B7A),
                        modifier = Modifier.weight(1f)
                    )
                    ConvocadosCounter(
                        label = "Suplentes",
                        count = suplentes.size,
                        color = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!titularesOk) {
                    Text(
                        text = "La alineación necesita exactamente 11 titulares (${titulares.size}/11)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC62828)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = formationExpanded,
                    onExpandedChange = { formationExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Alineación") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formationExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = formationExpanded,
                        onDismissRequest = { formationExpanded = false }
                    ) {
                        Formation.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = {
                                    formation = item.label
                                    formationExpanded = false
                                }
                            )
                        }
                    }
                }

                LineupField(
                    formation = selectedFormation,
                    titulares = titulares
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

                if (match.status != MatchStatus.FINISHED) {
                    androidx.compose.material3.Button(
                        onClick = {
                            if (!titularesOk) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Necesitas exactamente 11 titulares para empezar el partido"
                                    )
                                }
                            } else {
                                val current = buildCurrentMatch()
                                onSave(current)
                                onContinue(current)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenAccent,
                            contentColor = Color(0xFF06210C)
                        )
                    ) {
                        Text("CONTINUAR → PARTIDO EN VIVO", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                    exportConvocatoriaAndActa(
                                        context = context,
                                        match = buildCurrentMatch(),
                                        team = team,
                                        players = teamPlayers,
                                        callupMap = callupMap,
                                        events = matchEvents,
                                        eventLabel = eventLabel,
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, GreenMint),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenMint)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("EXPORTAR CONVOCATORIA + ACTA", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
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
    val jerseyColor = if (mode == CallupStatus.TITULAR) GreenAccent else AmberAccent
    val bgColor = if (isActive) jerseyColor.copy(alpha = 0.2f) else Color.Transparent
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
                CallupStatus.TITULAR -> GreenAccent.copy(alpha = 0.16f)
                CallupStatus.SUPLENTE -> AmberAccent.copy(alpha = 0.16f)
                CallupStatus.NONE -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isConvoked) BorderStroke(
            1.5.dp,
            if (status == CallupStatus.TITULAR) GreenAccent else AmberAccent
        ) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
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

private fun parseDateToMillis(date: String): Long? {
    val parts = date.split("/")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatDate(millis: Long): String {
    val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val year = calendar.get(Calendar.YEAR)
    return "$day/$month/$year"
}

private fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return hour to minute
}

private fun formatTime(hour: Int, minute: Int): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private suspend fun exportConvocatoriaAndActa(
    context: Context,
    match: Match,
    team: Team?,
    players: List<Player>,
    callupMap: Map<Int, CallupStatus>,
    events: List<MatchEvent>,
    eventLabel: (MatchEvent) -> String,
    snackbarHostState: SnackbarHostState
) {
    try {
        val uris = withContext(Dispatchers.IO) {
            val convUri = ConvocatoriaPdfExporter(context).export(
                match = match,
                team = team,
                players = players,
                callupMap = callupMap
            )
            val teamGoals = if (match.isHome) (match.homeScore ?: 0) else (match.awayScore ?: 0)
            val rivalGoals = if (match.isHome) (match.awayScore ?: 0) else (match.homeScore ?: 0)
            val maxPeriod = events.maxOfOrNull { it.period } ?: match.numParts.coerceAtLeast(1)
            val actaUri = MatchReportExporter(context).exportAll(
                match = match,
                team = team,
                events = events,
                players = players,
                teamGoals = teamGoals,
                rivalGoals = rivalGoals,
                period = maxPeriod,
                elapsedSeconds = 0,
                secondsOnField = emptyMap(),
                eventLabel = eventLabel
            ).pdfUri
            listOfNotNull(convUri, actaUri)
        }
        if (uris.isEmpty()) {
            snackbarHostState.showSnackbar("No se pudieron generar los PDF")
            return
        }
        val share = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Convocatoria y acta · ${team?.name ?: "Equipo"} vs ${match.rival.ifBlank { "Rival" }}"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Adjuntos: convocatoria + acta del partido (PDF)."
            )
        }
        context.startActivity(Intent.createChooser(share, "Exportar convocatoria y acta"))
        snackbarHostState.showSnackbar("Convocatoria + acta listas")
    } catch (_: Exception) {
        snackbarHostState.showSnackbar("No se pudieron exportar los PDF")
    }
}
