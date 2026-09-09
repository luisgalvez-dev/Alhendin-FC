package com.luis.alhendinfc.ui.statistics

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.PlayerSeasonStats
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.matches.JerseyIcon
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    team: Team?,
    stats: List<PlayerSeasonStats>,
    finishedMatchCount: Int,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Estadísticas", fontWeight = FontWeight.Bold)
                        Text(
                            team?.name ?: "Equipo",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF12351A))
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0E2614), Color(0xFF071A0A)))
                )
                .padding(20.dp)
        ) {
            Text(
                "Resumen de partidos finalizados ($finishedMatchCount). " +
                    "Minutos reales en campo, goles, asistencias y tarjetas aquí; " +
                    "las personalizadas (robos, paradas…) se ven en la ficha de cada jugador.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))

            StatsHeaderRow()
            HorizontalDivider(color = GreenAccent.copy(alpha = 0.3f))

            if (stats.all {
                    it.goals == 0 && it.assists == 0 && it.yellowCards == 0 && it.redCards == 0 &&
                        it.matchesPlayed == 0 && it.minutesPlayed == 0 && it.starts == 0 &&
                        it.customStats.all { c -> c.value == 0 }
                }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aún no hay estadísticas.\nFinaliza un partido en vivo para empezar a acumular datos.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn {
                    itemsIndexed(stats, key = { _, s -> s.player.id }) { index, row ->
                        StatsPlayerRow(index + 1, row)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", Modifier.width(28.dp), fontWeight = FontWeight.Bold, color = GreenMint)
        Spacer(Modifier.width(48.dp))
        Text("Jugador", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = GreenMint)
        StatCol("PJ", GreenMint)
        StatCol("Min", GreenMint)
        StatCol("T", GreenMint)
        StatCol("G", GreenAccent)
        StatCol("A", GreenMint)
        StatCol("TA", AmberAccent)
        StatCol("TR", Color(0xFFFF5252))
    }
}

@Composable
private fun StatsPlayerRow(rank: Int, stats: PlayerSeasonStats) {
    val player = stats.player
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rank.toString(),
            modifier = Modifier.width(28.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        JerseyIcon(player.jerseyNumber, CallupStatus.TITULAR, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                player.alias.ifBlank { player.name },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                player.position.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        StatCol(stats.matchesPlayed.toString(), Color.White)
        StatCol(stats.minutesPlayed.toString(), Color.White)
        StatCol(stats.starts.toString(), Color.White)
        StatCol(stats.goals.toString(), GreenAccent)
        StatCol(stats.assists.toString(), GreenMint)
        StatCol(stats.yellowCards.toString(), AmberAccent)
        StatCol(stats.redCards.toString(), Color(0xFFFF5252))
    }
}

@Composable
private fun StatCol(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier.width(36.dp),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = color
    )
}
