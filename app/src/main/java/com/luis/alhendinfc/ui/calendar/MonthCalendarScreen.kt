package com.luis.alhendinfc.ui.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.CalendarDayContent
import com.luis.alhendinfc.domain.model.CalendarDayEntry
import com.luis.alhendinfc.domain.model.CalendarDayVisual
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.util.LocalImageLoader
import java.time.LocalDate
import java.time.YearMonth

private val MONTH_NAMES = listOf(
    "", "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)
private val WEEKDAYS = listOf("L", "M", "X", "J", "V", "S", "D")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthCalendarScreen(
    team: Team?,
    visibleMonth: YearMonth,
    dayContents: Map<Long, CalendarDayContent>,
    clubs: List<OpponentClub> = emptyList(),
    fixtures: List<FixtureRow> = emptyList(),
    rivalShields: Map<String, Attachment> = emptyMap(),
    teamShields: Map<String, Attachment> = emptyMap(),
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    onOpenMatch: (Int) -> Unit,
    onPrepareFixture: (FixtureRow) -> Unit,
    onOpenTraining: (Int) -> Unit,
    onAddTraining: (Long) -> Unit,
    onOpenFixtures: () -> Unit,
    onBack: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val clubsById = remember(clubs) { clubs.associateBy { it.id } }
    val clubsByMatchday = remember(fixtures) {
        fixtures.associate { it.fixture.matchday to it.club }
    }
    var pendingEmptyDay by remember { mutableStateOf<Long?>(null) }
    var pendingChoice by remember { mutableStateOf<CalendarDayContent?>(null) }

    pendingEmptyDay?.let { epoch ->
        AlertDialog(
            onDismissRequest = { pendingEmptyDay = null },
            title = { Text("Día libre") },
            text = { Text("¿Añadir un entrenamiento en esta fecha?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingEmptyDay = null
                    onAddTraining(epoch)
                }) { Text("Añadir entrenamiento") }
            },
            dismissButton = {
                TextButton(onClick = { pendingEmptyDay = null }) { Text("Cancelar") }
            }
        )
    }

    pendingChoice?.let { content ->
        AlertDialog(
            onDismissRequest = { pendingChoice = null },
            title = { Text("Este día") },
            text = { Text("Hay un partido y un entrenamiento. ¿Qué quieres abrir?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingChoice = null
                    when (val entry = content.matchOrFixture) {
                        is CalendarDayEntry.MatchEntry -> onOpenMatch(entry.match.id)
                        is CalendarDayEntry.FixtureEntry -> onPrepareFixture(entry.row)
                        else -> Unit
                    }
                }) { Text("Partido") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val training = content.training
                    pendingChoice = null
                    if (training != null) onOpenTraining(training.id)
                }) { Text("Entrenamiento") }
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
                            team?.name ?: "Sin equipo",
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
                actions = {
                    TextButton(onClick = onOpenFixtures) {
                        Text("Jornadas", color = GreenLime)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E2414),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Mes anterior",
                        tint = Color.White
                    )
                }
                Text(
                    "${MONTH_NAMES[visibleMonth.monthValue]} ${visibleMonth.year}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onNextMonth) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Mes siguiente",
                        tint = Color.White
                    )
                }
            }
            if (visibleMonth != YearMonth.from(today)) {
                TextButton(
                    onClick = onGoToToday,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Mes actual", color = GreenMint)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAYS.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = GreenMint,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            monthCells(visibleMonth).chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { date ->
                        Box(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                            if (date != null) {
                                val epoch = date.toEpochDay()
                                MonthDayCell(
                                    date = date,
                                    isToday = date == today,
                                    content = dayContents[epoch],
                                    clubsById = clubsById,
                                    clubsByMatchday = clubsByMatchday,
                                    team = team,
                                    rivalShields = rivalShields,
                                    teamShields = teamShields,
                                    onClick = {
                                        val day = dayContents[epoch]
                                            ?: CalendarDayContent(epoch, null, null)
                                        when {
                                            day.hasMatchOrFixture && day.hasTraining ->
                                                pendingChoice = day
                                            day.matchOrFixture is CalendarDayEntry.MatchEntry ->
                                                onOpenMatch(
                                                    (day.matchOrFixture as CalendarDayEntry.MatchEntry).match.id
                                                )
                                            day.matchOrFixture is CalendarDayEntry.FixtureEntry ->
                                                onPrepareFixture(
                                                    (day.matchOrFixture as CalendarDayEntry.FixtureEntry).row
                                                )
                                            day.hasTraining ->
                                                onOpenTraining(day.training!!.id)
                                            else -> pendingEmptyDay = epoch
                                        }
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.aspectRatio(0.85f).fillMaxWidth())
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(GreenAccent, "Partido")
                LegendDot(AmberAccent, "Entrenamiento")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isToday: Boolean,
    content: CalendarDayContent?,
    clubsById: Map<Int, OpponentClub>,
    clubsByMatchday: Map<Int, OpponentClub?>,
    team: Team?,
    rivalShields: Map<String, Attachment>,
    teamShields: Map<String, Attachment>,
    onClick: () -> Unit
) {
    val matchVisual = CalendarDayVisual.matchVisual(
        content?.matchOrFixture,
        clubsById,
        team,
        rivalShields,
        teamShields,
        clubsByMatchday
    )
    val training = content?.training
    val border = when {
        isToday -> GreenLime
        else -> Color.White.copy(alpha = 0.12f)
    }
    val background = when {
        matchVisual != null -> Color(0xFF1A4A28)
        training != null -> Color(0xFF2A2814)
        else -> Color(0xFF143D1F)
    }
    val a11y = buildString {
        append("Día ${date.dayOfMonth}")
        if (isToday) append(", hoy")
        matchVisual?.let { append(", ${it.contentDescription}") }
        if (training != null) append(", entrenamiento")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .semantics(mergeDescendants = true) { contentDescription = a11y }
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = if (isToday) GreenLime else Color.White,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = if (isToday) 16.sp else 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (matchVisual != null) {
                Text(
                    "P",
                    color = GreenAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            } else if (training != null) {
                Text(
                    "E",
                    color = AmberAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val cellWidth = maxWidth
            val cellHeight = maxHeight
            val shortest = minOf(cellWidth, cellHeight)
            val shieldSize = when {
                shortest >= 72.dp -> 58.dp
                shortest >= 60.dp -> 52.dp
                shortest >= 48.dp -> 48.dp
                else -> (shortest * 0.88f).coerceAtLeast(36.dp)
            }.coerceAtMost(minOf(cellWidth * 0.92f, cellHeight * 0.80f, 64.dp))
            val nameSize = when {
                cellWidth >= 72.dp -> 14.sp
                cellWidth >= 52.dp -> 13.sp
                else -> 12.sp
            }
            val resultShield = minOf(
                cellWidth * 0.34f,
                cellHeight * 0.58f,
                if (cellWidth >= 96.dp) 50.dp else if (cellWidth >= 72.dp) 46.dp else 40.dp
            ).coerceIn(26.dp, 52.dp)
            val scoreSize = when {
                cellWidth >= 72.dp -> 18.sp
                else -> 16.sp
            }
            if (matchVisual != null && matchVisual.showsResult) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CalendarShieldBadge(
                            shieldUri = matchVisual.ourShieldUri,
                            initials = matchVisual.ourInitials,
                            contentDescription = "Escudo ${team?.name ?: "Alhendín"}",
                            size = resultShield
                        )
                        Text(
                            matchVisual.scoreLabel,
                            color = Color.White,
                            fontSize = scoreSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                        )
                        CalendarShieldBadge(
                            shieldUri = matchVisual.shieldUri,
                            initials = matchVisual.initials,
                            contentDescription = if (matchVisual.hasRival) {
                                "Escudo ${matchVisual.displayName}"
                            } else {
                                "Rival"
                            },
                            size = resultShield
                        )
                    }
                    Text(
                        matchVisual.displayName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = nameSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (matchVisual != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalendarShieldBadge(
                        shieldUri = matchVisual.shieldUri,
                        initials = matchVisual.initials,
                        contentDescription = if (matchVisual.hasRival) {
                            "Escudo ${matchVisual.displayName}"
                        } else {
                            "Partido"
                        },
                        size = shieldSize
                    )
                    Text(
                        matchVisual.displayName,
                        color = Color.White,
                        fontSize = nameSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (training != null) {
                Text(
                    "Entreno",
                    color = AmberAccent,
                    fontSize = nameSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberAccent.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
        if (matchVisual != null && training != null) {
            Text(
                "E",
                color = AmberAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun CalendarShieldBadge(
    shieldUri: String?,
    initials: String,
    contentDescription: String,
    size: Dp
) {
    val context = LocalContext.current
    var bitmap by remember(shieldUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(shieldUri) {
        bitmap = LocalImageLoader.load(
            context,
            shieldUri,
            maxSidePx = (size.value * 3).toInt().coerceAtLeast(144)
        )
    }
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(GreenAccent.copy(alpha = 0.22f))
                    .border(1.dp, GreenAccent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initials,
                    fontWeight = FontWeight.Bold,
                    color = GreenMint,
                    fontSize = (size.value * 0.32f).sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(10.dp)
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
                .padding(horizontal = 8.dp)
        )
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
    }
}

private fun monthCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value + 6) % 7
    return buildList {
        repeat(lead) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
}
