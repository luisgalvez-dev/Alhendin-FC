package com.luis.alhendinfc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.CustomStatAppliesTo
import com.luis.alhendinfc.domain.model.CustomStatType
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    team: Team?,
    types: List<CustomStatType>,
    backupBusy: Boolean = false,
    backupMessage: BackupUiMessage? = null,
    onClearBackupMessage: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onAdd: (label: String, shortLabel: String, appliesTo: CustomStatAppliesTo) -> Unit,
    onUpdate: (CustomStatType, label: String, shortLabel: String, appliesTo: CustomStatAppliesTo) -> Unit,
    onToggleActive: (CustomStatType, Boolean) -> Unit,
    onDelete: (CustomStatType) -> Unit,
    onCustomizeHome: () -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<CustomStatType?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CustomStatType?>(null) }
    var confirmImport by remember { mutableStateOf(false) }

    if (backupMessage != null) {
        AlertDialog(
            onDismissRequest = onClearBackupMessage,
            title = {
                Text(if (backupMessage.isError) "Error" else "Copia de seguridad")
            },
            text = { Text(backupMessage.text) },
            confirmButton = {
                TextButton(onClick = onClearBackupMessage) { Text("OK") }
            }
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Importar datos") },
            text = {
                Text(
                    "Esto sustituye TODOS los datos actuales de la app por el backup. " +
                        "No se puede deshacer. ¿Continuar?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    onImportBackup()
                }) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text("Cancelar") }
            }
        )
    }

    if (creating) {
        StatTypeEditorDialog(
            title = "Nuevo tipo de evento",
            initialLabel = "",
            initialShort = "",
            initialAppliesTo = CustomStatAppliesTo.ALL,
            onConfirm = { label, short, appliesTo ->
                onAdd(label, short, appliesTo)
                creating = false
            },
            onDismiss = { creating = false }
        )
    }

    editing?.let { type ->
        StatTypeEditorDialog(
            title = "Editar tipo",
            initialLabel = type.label,
            initialShort = type.shortLabel,
            initialAppliesTo = type.appliesTo,
            onConfirm = { label, short, appliesTo ->
                onUpdate(type, label, short, appliesTo)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    pendingDelete?.let { type ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar «${type.label}»") },
            text = {
                Text(
                    "Si ya se usó en partidos, se desactivará para no romper el historial. " +
                        "Si no se usó, se borrará del todo."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(type)
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
                        Text("Ajustes", fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E2414),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { creating = true },
                containerColor = GreenAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir tipo")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A2410), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(padding)
                .padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCustomizeHome)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = GreenAccent,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Personalizar inicio",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Atajos del Home: mostrar, ocultar y ordenar",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenMint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Copia de seguridad",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Exporta absolutamente toda la base de datos: equipos, jugadores, " +
                            "partidos, convocatorias, eventos (goles, asistencias, tarjetas, " +
                            "paradas, robos…), tipos personalizados, clubs, calendario, tareas e inicio. " +
                            "Así recuperas también las estadísticas. Guarda el ZIP en el PC.",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenMint
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onExportBackup,
                            enabled = !backupBusy
                        ) {
                            Text(if (backupBusy) "…" else "Exportar")
                        }
                        TextButton(
                            onClick = { confirmImport = true },
                            enabled = !backupBusy
                        ) {
                            Text("Importar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Tipos de evento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Aparecerán en el menú del jugador o del rival en el partido. Puedes crear tipos solo para porteros o solo para el rival.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (types.isEmpty()) {
                Text(
                    "Aún no hay tipos. Pulsa + (ej. Parada para porteros, Ataque por banda para rival…).",
                    color = AmberAccent
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(types, key = { it.id }) { type ->
                        StatTypeRow(
                            type = type,
                            onEdit = { editing = type },
                            onToggle = { onToggleActive(type, it) },
                            onDelete = { pendingDelete = type }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTypeRow(
    type: CustomStatType,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2A1E)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    type.label,
                    fontWeight = FontWeight.Bold,
                    color = if (type.isActive) Color.White else Color.White.copy(alpha = 0.45f)
                )
                Text(
                    "Atajo: ${type.shortLabel} · ${type.appliesTo.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenMint.copy(alpha = if (type.isActive) 1f else 0.5f)
                )
            }
            Switch(checked = type.isActive, onCheckedChange = onToggle)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = GreenMint)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
            }
        }
    }
}

@Composable
private fun StatTypeEditorDialog(
    title: String,
    initialLabel: String,
    initialShort: String,
    initialAppliesTo: CustomStatAppliesTo,
    onConfirm: (label: String, shortLabel: String, appliesTo: CustomStatAppliesTo) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(initialLabel) }
    var shortLabel by remember { mutableStateOf(initialShort) }
    var appliesTo by remember { mutableStateOf(initialAppliesTo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = shortLabel,
                    onValueChange = { shortLabel = it.take(12) },
                    label = { Text("Nombre corto (botón)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Aplicable a", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CustomStatAppliesTo.entries.forEach { option ->
                        FilterChip(
                            selected = appliesTo == option,
                            onClick = { appliesTo = option },
                            label = { Text(option.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenAccent.copy(alpha = 0.35f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label, shortLabel, appliesTo) },
                enabled = label.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
