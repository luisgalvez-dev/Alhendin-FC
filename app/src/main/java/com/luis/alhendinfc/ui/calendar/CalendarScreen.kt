package com.luis.alhendinfc.ui.calendar

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchLifecycle
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.SeasonFixture
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.theme.GreenPitch
import com.luis.alhendinfc.ui.util.LocalImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    team: Team?,
    fixtures: List<FixtureRow>,
    clubs: List<OpponentClub>,
    matches: List<Match>,
    opponentShields: Map<String, Attachment> = emptyMap(),
    onPrepareMatch: (FixtureRow) -> Unit,
    onSaveFixture: (
        existingId: Int,
        matchday: Int,
        opponentClubId: Int,
        isHome: Boolean,
        date: String,
        time: String,
        stadiumOverride: String
    ) -> Unit,
    onAddFixture: (
        matchday: Int,
        opponentClubId: Int,
        isHome: Boolean,
        date: String,
        time: String,
        stadiumOverride: String
    ) -> Unit,
    onDeleteFixture: (FixtureRow) -> Unit,
    onAddClub: (name: String, shortName: String, stadium: String, shieldUri: String?, kitColors: String) -> Unit,
    onUpdateClub: (OpponentClub, Uri?, Boolean) -> Unit,
    onDeleteClub: (OpponentClub) -> Unit,
    onOpenRivals: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingClub by remember { mutableStateOf<OpponentClub?>(null) }
    var creatingClub by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<OpponentClub?>(null) }
    var editingFixture by remember { mutableStateOf<FixtureRow?>(null) }
    var creatingFixture by remember { mutableStateOf(false) }
    var pendingDeleteFixture by remember { mutableStateOf<FixtureRow?>(null) }
    var needClubFirst by remember { mutableStateOf(false) }

    val matchesByDay = remember(matches) {
        matches.groupBy { it.matchday }
    }

    if (needClubFirst) {
        AlertDialog(
            onDismissRequest = { needClubFirst = false },
            title = { Text("Sin clubs rivales") },
            text = {
                Text("Primero añade un club en la pestaña «Clubs rivales» para poder asignarlo a una jornada.")
            },
            confirmButton = {
                TextButton(onClick = {
                    needClubFirst = false
                    selectedTab = 1
                }) { Text("Ir a clubs") }
            },
            dismissButton = {
                TextButton(onClick = { needClubFirst = false }) { Text("Cerrar") }
            }
        )
    }

    if (creatingFixture) {
        FixtureEditorDialog(
            title = "Nueva jornada",
            initial = null,
            nextMatchday = (fixtures.maxOfOrNull { it.fixture.matchday } ?: 0) + 1,
            clubs = clubs,
            onConfirm = { _, matchday, clubId, isHome, date, time, stadium ->
                onAddFixture(matchday, clubId, isHome, date, time, stadium)
                creatingFixture = false
            },
            onDismiss = { creatingFixture = false }
        )
    }

    editingFixture?.let { row ->
        FixtureEditorDialog(
            title = "Editar jornada ${row.fixture.matchday}",
            initial = row,
            nextMatchday = row.fixture.matchday,
            clubs = clubs,
            onConfirm = { id, matchday, clubId, isHome, date, time, stadium ->
                onSaveFixture(id, matchday, clubId, isHome, date, time, stadium)
                editingFixture = null
            },
            onDismiss = { editingFixture = null }
        )
    }

    pendingDeleteFixture?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFixture = null },
            title = { Text("Eliminar jornada ${row.fixture.matchday}") },
            text = { Text("Se quitará del calendario. No elimina partidos ya creados.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFixture(row)
                    pendingDeleteFixture = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFixture = null }) { Text("Cancelar") }
            }
        )
    }

    if (creatingClub) {
        ClubEditorDialog(
            title = "Nuevo club rival",
            initial = null,
            onConfirm = { name, short, stadium, uri, kitColors, picked, _ ->
                onAddClub(name, short, stadium, picked?.toString() ?: uri, kitColors)
                creatingClub = false
            },
            onDismiss = { creatingClub = false }
        )
    }

    editingClub?.let { club ->
        ClubEditorDialog(
            title = "Editar club",
            initial = club.copy(
                shieldUri = SharedMedia.displayPath(opponentShields[club.syncId], club.shieldUri)
            ),
            onConfirm = { name, short, stadium, _, kitColors, picked, clear ->
                onUpdateClub(
                    club.copy(
                        name = name.trim(),
                        shortName = short.trim(),
                        stadium = stadium.trim(),
                        kitColors = kitColors.trim()
                    ),
                    picked,
                    clear
                )
                editingClub = null
            },
            onDismiss = { editingClub = null }
        )
    }

    pendingDelete?.let { club ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar «${club.name}»") },
            text = {
                Text("Se eliminará el club. Las jornadas que lo usen quedarán sin rival; podrás editarlas y asignar otro.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClub(club)
                    pendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Jornadas", fontWeight = FontWeight.Bold)
                        Text(
                            text = team?.name ?: "Sin equipo",
                            style = MaterialTheme.typography.labelMedium,
                            color = GreenMint
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E2414),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            if (clubs.isEmpty()) needClubFirst = true
                            else creatingFixture = true
                        }
                        else -> creatingClub = true
                    }
                },
                containerColor = GreenAccent,
                contentColor = Color.Black
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "Añadir jornada" else "Añadir club"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A2410), Color(0xFF0F2A14), Color(0xFF061408))
                    )
                )
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF143D1F),
                contentColor = GreenLime,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GreenAccent
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Jornadas") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Clubs rivales") },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> FixturesTab(
                    fixtures = fixtures,
                    matchesByDay = matchesByDay,
                    onPrepareMatch = onPrepareMatch,
                    onEdit = { editingFixture = it },
                    onDelete = { pendingDeleteFixture = it }
                )
                else -> ClubsTab(
                    clubs = clubs,
                    opponentShields = opponentShields,
                    onOpenRivals = onOpenRivals,
                    onEdit = { editingClub = it },
                    onDelete = { pendingDelete = it }
                )
            }
        }
    }
}

@Composable
private fun FixturesTab(
    fixtures: List<FixtureRow>,
    matchesByDay: Map<Int, List<Match>>,
    onPrepareMatch: (FixtureRow) -> Unit,
    onEdit: (FixtureRow) -> Unit,
    onDelete: (FixtureRow) -> Unit
) {
    if (fixtures.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No hay jornadas. Pulsa + para añadir la primera.",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(fixtures, key = { it.fixture.id }) { row ->
            val related = matchesByDay[row.fixture.matchday].orEmpty()
            val associated = MatchLifecycle.resolveMatchForFixture(related, row.fixture.matchday)
            val openOrLive = related.firstOrNull {
                it.status == MatchStatus.OPEN || it.status == MatchStatus.LIVE
            }
            FixtureCard(
                row = row,
                existingLabel = when {
                    openOrLive != null && MatchLifecycle.isShownAsLive(openOrLive) -> "En vivo"
                    openOrLive != null -> "Preparación"
                    related.any { it.status == MatchStatus.FINISHED } -> "Jugado"
                    else -> null
                },
                actionLabel = MatchLifecycle.fixtureActionLabel(associated),
                onPrepare = { onPrepareMatch(row) },
                onEdit = { onEdit(row) },
                onDelete = { onDelete(row) }
            )
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun FixtureCard(
    row: FixtureRow,
    existingLabel: String?,
    actionLabel: String,
    onPrepare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val venue = if (row.fixture.isHome) "Local" else "Visitante"
    val missingRival = row.club == null
    val clubName = row.club?.name ?: "Sin rival — editar"
    val whenText = buildString {
        if (row.fixture.date.isNotBlank()) append(row.fixture.date)
        if (row.fixture.time.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(row.fixture.time)
        }
        if (isEmpty()) append("Fecha por definir")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            if (missingRival) AmberAccent.copy(alpha = 0.7f) else GreenAccent.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF143D1F), Color(0xFF1A4A28))
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "J${row.fixture.matchday}",
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenLime,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    venue,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (row.fixture.isHome) GreenMint else AmberAccent
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    clubName,
                    fontWeight = FontWeight.Bold,
                    color = if (missingRival) AmberAccent else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    row.stadium.ifBlank { "Estadio por definir" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    whenText,
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenMint
                )
                if (existingLabel != null) {
                    Text(
                        existingLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar jornada", tint = GreenMint)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar jornada", tint = Color(0xFFFF8A80))
            }
            Button(
                onClick = onPrepare,
                enabled = !missingRival,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPitch,
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.12f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixtureEditorDialog(
    title: String,
    initial: FixtureRow?,
    nextMatchday: Int,
    clubs: List<OpponentClub>,
    onConfirm: (
        id: Int,
        matchday: Int,
        opponentClubId: Int,
        isHome: Boolean,
        date: String,
        time: String,
        stadiumOverride: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var matchdayText by remember {
        mutableStateOf((initial?.fixture?.matchday ?: nextMatchday).toString())
    }
    var selectedClubId by remember {
        mutableStateOf(
            initial?.club?.id
                ?: initial?.fixture?.opponentClubId?.takeIf { id -> clubs.any { it.id == id } }
                ?: clubs.firstOrNull()?.id
                ?: 0
        )
    }
    var isHome by remember { mutableStateOf(initial?.fixture?.isHome ?: true) }
    var date by remember { mutableStateOf(initial?.fixture?.date.orEmpty()) }
    var time by remember { mutableStateOf(initial?.fixture?.time.orEmpty()) }
    var stadium by remember {
        mutableStateOf(initial?.fixture?.stadiumOverride.orEmpty())
    }
    var clubMenuExpanded by remember { mutableStateOf(false) }

    val selectedClub = clubs.firstOrNull { it.id == selectedClubId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = matchdayText,
                    onValueChange = { matchdayText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Jornada") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = clubMenuExpanded,
                    onExpandedChange = { clubMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedClub?.name ?: "Elige rival",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rival") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(clubMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = selectedClub == null
                    )
                    ExposedDropdownMenu(
                        expanded = clubMenuExpanded,
                        onDismissRequest = { clubMenuExpanded = false }
                    ) {
                        clubs.forEach { club ->
                            DropdownMenuItem(
                                text = { Text(club.name) },
                                onClick = {
                                    selectedClubId = club.id
                                    clubMenuExpanded = false
                                    if (stadium.isBlank() && !isHome) {
                                        stadium = club.stadium
                                    }
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isHome,
                        onClick = { isHome = true },
                        label = { Text("Local") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenAccent.copy(alpha = 0.35f)
                        )
                    )
                    FilterChip(
                        selected = !isHome,
                        onClick = { isHome = false },
                        label = { Text("Visitante") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberAccent.copy(alpha = 0.35f)
                        )
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha (opcional)") },
                    placeholder = { Text("dd/mm/aaaa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Hora (opcional)") },
                    placeholder = { Text("HH:mm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stadium,
                    onValueChange = { stadium = it },
                    label = { Text("Estadio (opcional)") },
                    placeholder = {
                        Text(
                            if (isHome) "Vacío = campo propio"
                            else selectedClub?.stadium?.ifBlank { "Estadio del rival" }
                                ?: "Estadio del rival"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val day = matchdayText.toIntOrNull() ?: return@TextButton
                    if (selectedClubId <= 0 || day <= 0) return@TextButton
                    onConfirm(
                        initial?.fixture?.id ?: 0,
                        day,
                        selectedClubId,
                        isHome,
                        date,
                        time,
                        stadium
                    )
                },
                enabled = selectedClubId > 0 && (matchdayText.toIntOrNull() ?: 0) > 0
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ClubsTab(
    clubs: List<OpponentClub>,
    opponentShields: Map<String, Attachment>,
    onOpenRivals: () -> Unit,
    onEdit: (OpponentClub) -> Unit,
    onDelete: (OpponentClub) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TextButton(onClick = onOpenRivals) {
                Text("Abrir módulo Rivales")
            }
        }
        if (clubs.isEmpty()) {
            item {
                Text(
                    "Aún no hay clubs rivales.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        items(clubs, key = { it.id }) { club ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A22)),
                border = BorderStroke(1.dp, GreenMint.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val shieldPath = SharedMedia.displayPath(opponentShields[club.syncId], club.shieldUri)
                    LocalShieldThumb(
                        shieldUri = shieldPath,
                        fallback = club.shortName.ifBlank { club.name }.take(1).uppercase(),
                        size = 48
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            club.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            listOfNotNull(
                                club.shortName.takeIf { it.isNotBlank() },
                                club.stadium.takeIf { it.isNotBlank() },
                                club.kitColors.takeIf { it.isNotBlank() }?.let { "Equipación: $it" }
                            ).joinToString(" · ").ifBlank { "Sin datos extra" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            if (SharedMedia.isDisplayableLocal(shieldPath)) "Escudo"
                            else "Sin escudo",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (SharedMedia.isDisplayableLocal(shieldPath)) GreenMint
                            else Color.White.copy(alpha = 0.45f)
                        )
                    }
                    IconButton(onClick = { onEdit(club) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = GreenMint)
                    }
                    IconButton(onClick = { onDelete(club) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFFF8A80)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

private fun isLocalShieldUri(uri: String?): Boolean = SharedMedia.isDisplayableLocal(uri)

@Composable
private fun LocalShieldThumb(
    shieldUri: String?,
    fallback: String,
    size: Int
) {
    val context = LocalContext.current
    var bitmap by remember(shieldUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(shieldUri) {
        bitmap = if (isLocalShieldUri(shieldUri)) {
            LocalImageLoader.load(context, shieldUri, maxSidePx = (size * 3).coerceAtLeast(96))
        } else null
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(GreenAccent.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
            )
        } else {
            Text(
                fallback.ifBlank { "?" },
                fontWeight = FontWeight.Bold,
                color = GreenLime
            )
        }
    }
}

@Composable
private fun ClubEditorDialog(
    title: String,
    initial: OpponentClub?,
    onConfirm: (
        name: String,
        shortName: String,
        stadium: String,
        shieldUri: String?,
        kitColors: String,
        picked: Uri?,
        clearShield: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var shortName by remember { mutableStateOf(initial?.shortName.orEmpty()) }
    var stadium by remember { mutableStateOf(initial?.stadium.orEmpty()) }
    var kitColors by remember { mutableStateOf(initial?.kitColors.orEmpty()) }
    var shieldUri by remember {
        mutableStateOf(initial?.shieldUri?.takeIf { isLocalShieldUri(it) })
    }
    var shieldBitmap by remember(shieldUri) { mutableStateOf<ImageBitmap?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var clearedShield by remember { mutableStateOf(false) }

    LaunchedEffect(shieldUri) {
        val uri = shieldUri
        shieldBitmap = if (uri != null && isLocalShieldUri(uri)) {
            LocalImageLoader.load(context, uri, maxSidePx = 384)
        } else {
            null
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        shieldUri = uri.toString()
        pickedUri = uri
        clearedShield = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePicker.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (shieldBitmap != null) {
                            Image(
                                bitmap = shieldBitmap!!,
                                contentDescription = "Escudo rival",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    "Escudo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Escudo del rival", fontWeight = FontWeight.Medium)
                        Text(
                            "Elige una imagen de la galería (solo local, sin internet).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (shieldUri != null) {
                            TextButton(onClick = {
                                shieldUri = null
                                shieldBitmap = null
                                pickedUri = null
                                clearedShield = true
                            }) {
                                Text("Quitar escudo")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("Nombre corto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stadium,
                    onValueChange = { stadium = it },
                    label = { Text("Estadio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kitColors,
                    onValueChange = { kitColors = it },
                    label = { Text("Color equipación") },
                    placeholder = { Text("Ej. Rojo y blanco") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            shortName,
                            stadium,
                            shieldUri?.takeIf { isLocalShieldUri(it) },
                            kitColors,
                            pickedUri,
                            clearedShield
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
