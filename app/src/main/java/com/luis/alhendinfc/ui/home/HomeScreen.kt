package com.luis.alhendinfc.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.team.TeamEditDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    teams: List<Team>,
    selectedTeam: Team?,
    onNavigateToTeam: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onAddTeam: (Team) -> Unit,
    onSelectTeam: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
            .background(MaterialTheme.colorScheme.background)
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

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.62f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MainCard(
                        title = "EQUIPO",
                        icon = Icons.Default.Person,
                        enabled = selectedTeam != null,
                        onClick = onNavigateToTeam,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MainCard(
                        title = "PARTIDOS",
                        icon = Icons.Default.PlayArrow,
                        enabled = selectedTeam != null,
                        onClick = onNavigateToMatches,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.38f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SecondaryCard(
                        title = "ESTADÍSTICAS",
                        icon = Icons.Default.Info,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    SecondaryCard(
                        title = "AJUSTES",
                        icon = Icons.Default.Settings,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
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
        // Botón "+" para añadir equipo
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

        // Selector de equipo activo
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
        val uri = team?.shieldUri
        if (uri != null) {
            bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (e: Exception) { null }
            }
        } else {
            bitmap = null
        }
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 2.sp
                )

                if (!enabled) {
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
                            text = if (title == "EQUIPO") "Añade un equipo primero"
                                   else "Próximamente",
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.alpha(0.4f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Próximamente",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
