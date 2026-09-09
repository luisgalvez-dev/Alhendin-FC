package com.luis.alhendinfc.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.HomeLayoutConfig
import com.luis.alhendinfc.domain.model.HomeModule
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.team.TeamEditDialog
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.theme.TealSoft
import com.luis.alhendinfc.ui.util.LocalImageLoader

@Composable
fun HomeScreen(
    teams: List<Team>,
    selectedTeam: Team?,
    layoutConfig: HomeLayoutConfig,
    nextFixture: FixtureRow?,
    liveMatch: Match?,
    onNavigateToTeam: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToRivals: () -> Unit,
    onNavigateToPizarra: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLive: (matchId: Int) -> Unit,
    onNavigateToNextMatch: () -> Unit,
    onAddTeam: (Team) -> Unit,
    onSelectTeam: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val hasTeam = selectedTeam != null
    val lockedHint = "Añade un equipo primero"

    val visibleModules = remember(layoutConfig, liveMatch) {
        layoutConfig.visibleOrdered.filter { module ->
            module != HomeModule.LIVE_MATCH || liveMatch != null
        }
    }

    if (showAddDialog) {
        TeamEditDialog(
            currentTeam = null,
            onConfirm = { newTeam ->
                onAddTeam(newTeam)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A2410),
                        MaterialTheme.colorScheme.background,
                        Color(0xFF061408)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            TeamSelectorBar(
                teams = teams,
                selectedTeam = selectedTeam,
                onSelectTeam = onSelectTeam,
                onAddTeam = { showAddDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (visibleModules.isEmpty()) {
                Text(
                    "No hay módulos activos. Actívalos en Ajustes → Personalizar inicio.",
                    color = AmberAccent,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                val rows = visibleModules.chunked(3)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rows.forEach { rowModules ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            rowModules.forEach { module ->
                                HomeModuleCard(
                                    module = module,
                                    hasTeam = hasTeam,
                                    lockedHint = lockedHint,
                                    nextFixture = nextFixture,
                                    liveMatch = liveMatch,
                                    onNavigateToTeam = onNavigateToTeam,
                                    onNavigateToMatches = onNavigateToMatches,
                                    onNavigateToCalendar = onNavigateToCalendar,
                                    onNavigateToTasks = onNavigateToTasks,
                                    onNavigateToRivals = onNavigateToRivals,
                                    onNavigateToPizarra = onNavigateToPizarra,
                                    onNavigateToStatistics = onNavigateToStatistics,
                                    onNavigateToSettings = onNavigateToSettings,
                                    onNavigateToLive = onNavigateToLive,
                                    onNavigateToNextMatch = onNavigateToNextMatch,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                            repeat(3 - rowModules.size) {
                                Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeModuleCard(
    module: HomeModule,
    hasTeam: Boolean,
    lockedHint: String,
    nextFixture: FixtureRow?,
    liveMatch: Match?,
    onNavigateToTeam: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToRivals: () -> Unit,
    onNavigateToPizarra: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLive: (matchId: Int) -> Unit,
    onNavigateToNextMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (module) {
        HomeModule.TEAM -> MainCard(
            title = module.title,
            icon = Icons.Default.Person,
            enabled = hasTeam,
            accent = GreenAccent,
            gradient = listOf(Color(0xFF143D1F), Color(0xFF1F6B35)),
            lockedLabel = if (!hasTeam) lockedHint else null,
            onClick = onNavigateToTeam,
            modifier = modifier
        )
        HomeModule.MATCHES -> MainCard(
            title = module.title,
            icon = Icons.Default.PlayArrow,
            enabled = hasTeam,
            accent = GreenLime,
            gradient = listOf(Color(0xFF1A3A22), Color(0xFF2A6B3A)),
            lockedLabel = if (!hasTeam) lockedHint else null,
            onClick = onNavigateToMatches,
            modifier = modifier
        )
        HomeModule.CALENDAR -> MainCard(
            title = module.title,
            icon = Icons.Default.DateRange,
            enabled = hasTeam,
            accent = TealSoft,
            gradient = listOf(Color(0xFF0F3A2A), Color(0xFF1F6B55)),
            lockedLabel = if (!hasTeam) lockedHint else null,
            onClick = onNavigateToCalendar,
            modifier = modifier
        )
        HomeModule.TASKS -> MainCard(
            title = module.title,
            icon = Icons.Default.List,
            enabled = hasTeam,
            accent = GreenMint,
            gradient = listOf(Color(0xFF123528), Color(0xFF1E5A40)),
            lockedLabel = if (!hasTeam) lockedHint else null,
            onClick = onNavigateToTasks,
            modifier = modifier
        )
        HomeModule.RIVALS -> MainCard(
            title = module.title,
            icon = Icons.Default.Place,
            enabled = hasTeam,
            accent = TealSoft,
            gradient = listOf(Color(0xFF123528), Color(0xFF1F5A45)),
            lockedLabel = if (!hasTeam) lockedHint else null,
            onClick = onNavigateToRivals,
            modifier = modifier
        )
        HomeModule.PIZARRA -> MainCard(
            title = module.title,
            icon = Icons.Default.Edit,
            enabled = true,
            accent = GreenMint,
            gradient = listOf(Color(0xFF143528), Color(0xFF1F5A3A)),
            lockedLabel = null,
            onClick = onNavigateToPizarra,
            modifier = modifier
        )
        HomeModule.STATISTICS -> SecondaryCard(
            title = module.title,
            icon = Icons.Default.Info,
            accent = TealSoft,
            enabled = hasTeam,
            subtitle = module.description,
            lockedLabel = if (!hasTeam) lockedHint else null,
            onClick = onNavigateToStatistics,
            modifier = modifier
        )
        HomeModule.SETTINGS -> SecondaryCard(
            title = module.title,
            icon = Icons.Default.Settings,
            accent = AmberAccent,
            enabled = hasTeam,
            subtitle = module.description,
            lockedLabel = if (!hasTeam) lockedHint else null,
            onClick = onNavigateToSettings,
            modifier = modifier
        )
        HomeModule.NEXT_MATCH -> NextMatchCard(
            fixture = nextFixture,
            enabled = hasTeam,
            lockedLabel = if (!hasTeam) lockedHint else null,
            onClick = onNavigateToNextMatch,
            modifier = modifier
        )
        HomeModule.LIVE_MATCH -> {
            val match = liveMatch
            if (match != null) {
                LiveMatchCard(
                    match = match,
                    onClick = { onNavigateToLive(match.id) },
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun TeamSelectorBar(
    teams: List<Team>,
    selectedTeam: Team?,
    onSelectTeam: (Int) -> Unit,
    onAddTeam: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val teamName = selectedTeam?.name ?: "Añadir equipo"

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAddTeam,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir equipo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { if (teams.isNotEmpty()) expanded = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                TeamAvatar(team = selectedTeam, size = 40)

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = teamName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (teams.size > 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                teams.forEach { team ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TeamAvatar(team = team, size = 32)
                                Text(team.name)
                            }
                        },
                        trailingIcon = {
                            if (team.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onSelectTeam(team.id)
                            expanded = false
                        }
                    )
                }

                if (teams.isNotEmpty()) {
                    HorizontalDivider()
                }

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Añadir equipo",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onAddTeam()
                    }
                )
            }
        }
    }
}

@Composable
fun TeamAvatar(team: Team?, size: Int) {
    val context = LocalContext.current
    var bitmap by remember(team?.shieldUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(team?.shieldUri) {
        bitmap = LocalImageLoader.load(context, team?.shieldUri, maxSidePx = (size * 3).coerceAtLeast(128))
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                if (bitmap == null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = (team?.name ?: "?").take(2).uppercase(),
                style = if (size >= 40) MaterialTheme.typography.labelLarge
                else MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun MainCard(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    accent: Color,
    gradient: List<Color>,
    lockedLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .alpha(if (enabled) 1f else 0.45f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.5.dp, accent.copy(alpha = if (enabled) 0.55f else 0.2f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                if (lockedLabel != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = lockedLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    subtitle: String,
    lockedLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.55f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) 0.55f else 0.25f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            accent.copy(alpha = if (enabled) 0.28f else 0.12f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    if (lockedLabel != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = accent.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = lockedLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.copy(alpha = 0.85f)
                            )
                        }
                    } else {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextMatchCard(
    fixture: FixtureRow?,
    enabled: Boolean,
    lockedLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = GreenMint
    val rival = fixture?.club?.let { club ->
        club.displayShort.ifBlank { club.name }
    }.orEmpty().ifBlank { "Sin rival" }
    val venue = when {
        fixture == null -> "Sin próximos partidos"
        fixture.fixture.isHome -> "Local"
        else -> "Visitante"
    }
    val whenLabel = buildString {
        append("J${fixture?.fixture?.matchday ?: "—"}")
        val date = fixture?.fixture?.date.orEmpty()
        val time = fixture?.fixture?.time.orEmpty()
        if (date.isNotBlank()) append(" · $date")
        if (time.isNotBlank()) append(" $time")
    }
    val canClick = enabled && fixture != null

    Card(
        modifier = modifier
            .then(if (canClick) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.5.dp, accent.copy(alpha = if (enabled) 0.55f else 0.2f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF123528), Color(0xFF1F5A45)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "PRÓXIMO PARTIDO",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (lockedLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            lockedLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (fixture == null) {
                    Text(
                        "Sin próximos partidos",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                } else {
                    Text(
                        rival,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "$venue · $whenLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMatchCard(
    match: Match,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFFFF7043)
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF3D2414), Color(0xFF5A3020)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "EN VIVO",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = match.rival.ifBlank { "Partido en curso" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                val score = when {
                    match.homeScore != null && match.awayScore != null ->
                        "${match.homeScore} — ${match.awayScore}"
                    else -> "Continuar partido"
                }
                Text(
                    text = score,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}
