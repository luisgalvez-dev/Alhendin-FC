package com.luis.alhendinfc.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.theme.GreenPitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    team: Team?,
    fixtures: List<FixtureRow>,
    clubs: List<OpponentClub>,
    matches: List<Match>,
    onPrepareMatch: (FixtureRow) -> Unit,
    onAddClub: (name: String, shortName: String, stadium: String, shieldUri: String?) -> Unit,
    onUpdateClub: (OpponentClub) -> Unit,
    onDeleteClub: (OpponentClub) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingClub by remember { mutableStateOf<OpponentClub?>(null) }
    var creatingClub by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<OpponentClub?>(null) }

    val matchesByDay = remember(matches) {
        matches.groupBy { it.matchday }
    }

    if (creatingClub) {
        ClubEditorDialog(
            title = "Nuevo club rival",
            initial = null,
            onConfirm = { name, short, stadium, uri ->
                onAddClub(name, short, stadium, uri)
                creatingClub = false
            },
            onDismiss = { creatingClub = false }
        )
    }

    editingClub?.let { club ->
        ClubEditorDialog(
            title = "Editar club",
            initial = club,
            onConfirm = { name, short, stadium, uri ->
                onUpdateClub(
                    club.copy(
                        name = name.trim(),
                        shortName = short.trim(),
                        stadium = stadium.trim(),
                        shieldUri = uri?.trim()?.ifBlank { null }
                    )
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
                Text("Se eliminará el club de la lista. Las jornadas que lo usen quedarán sin rival.")
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
                        Text("Calendario", fontWeight = FontWeight.Bold)
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
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { creatingClub = true },
                    containerColor = GreenAccent,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir club")
                }
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
                    onPrepareMatch = onPrepareMatch
                )
                else -> ClubsTab(
                    clubs = clubs,
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
    onPrepareMatch: (FixtureRow) -> Unit
) {
    if (fixtures.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No hay jornadas. Se cargará el calendario de ejemplo al seleccionar equipo.",
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
            val openOrLive = related.firstOrNull {
                it.status == MatchStatus.OPEN || it.status == MatchStatus.LIVE
            }
            FixtureCard(
                row = row,
                existingLabel = when {
                    openOrLive?.status == MatchStatus.LIVE -> "En vivo"
                    openOrLive != null -> "Preparando"
                    related.any { it.status == MatchStatus.FINISHED } -> "Jugado"
                    else -> null
                },
                onPrepare = { onPrepareMatch(row) }
            )
        }
    }
}

@Composable
private fun FixtureCard(
    row: FixtureRow,
    existingLabel: String?,
    onPrepare: () -> Unit
) {
    val venue = if (row.fixture.isHome) "Local" else "Visitante"
    val clubName = row.club?.name ?: "Rival desconocido"
    val whenText = buildString {
        if (row.fixture.date.isNotBlank()) append(row.fixture.date)
        if (row.fixture.time.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(row.fixture.time)
        }
        if (isEmpty()) append("Fecha por definir")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, GreenAccent.copy(alpha = 0.4f)),
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
                    color = Color.White,
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

            Button(
                onClick = onPrepare,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPitch,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (existingLabel == "Preparando" || existingLabel == "En vivo") "Abrir" else "Preparar")
            }
        }
    }
}

@Composable
private fun ClubsTab(
    clubs: List<OpponentClub>,
    onEdit: (OpponentClub) -> Unit,
    onDelete: (OpponentClub) -> Unit
) {
    if (clubs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Aún no hay clubs rivales.",
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            club.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            listOfNotNull(
                                club.shortName.takeIf { it.isNotBlank() },
                                club.stadium.takeIf { it.isNotBlank() }
                            ).joinToString(" · ").ifBlank { "Sin datos extra" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        if (!club.shieldUri.isNullOrBlank()) {
                            Text(
                                "Escudo: ${club.shieldUri}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GreenMint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

@Composable
private fun ClubEditorDialog(
    title: String,
    initial: OpponentClub?,
    onConfirm: (name: String, shortName: String, stadium: String, shieldUri: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var shortName by remember { mutableStateOf(initial?.shortName.orEmpty()) }
    var stadium by remember { mutableStateOf(initial?.stadium.orEmpty()) }
    var shieldUri by remember { mutableStateOf(initial?.shieldUri.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    value = shieldUri,
                    onValueChange = { shieldUri = it },
                    label = { Text("URI escudo (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, shortName, stadium, shieldUri.ifBlank { null })
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
