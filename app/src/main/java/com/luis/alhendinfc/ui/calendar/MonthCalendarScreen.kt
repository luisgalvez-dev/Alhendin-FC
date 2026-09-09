package com.luis.alhendinfc.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.domain.model.CalendarDayContent
import com.luis.alhendinfc.domain.model.CalendarDayEntry
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
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
    onClick: () -> Unit
) {
    val matchEntry = content?.matchOrFixture
    val training = content?.training
    val border = when {
        isToday -> GreenLime
        else -> Color.White.copy(alpha = 0.12f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF143D1F))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = if (isToday) GreenLime else Color.White,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
        if (matchEntry != null) {
            val label = when (matchEntry) {
                is CalendarDayEntry.MatchEntry ->
                    matchEntry.match.rival.ifBlank { "Partido" }
                is CalendarDayEntry.FixtureEntry ->
                    matchEntry.row.club?.displayShort
                        ?: matchEntry.row.club?.name
                        ?: "J${matchEntry.row.fixture.matchday}"
                else -> "Partido"
            }
            DayChip(label, GreenAccent)
        }
        if (training != null) {
            DayChip("Entreno", AmberAccent)
        }
    }
}

@Composable
private fun DayChip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 3.dp, vertical = 1.dp)
    )
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
