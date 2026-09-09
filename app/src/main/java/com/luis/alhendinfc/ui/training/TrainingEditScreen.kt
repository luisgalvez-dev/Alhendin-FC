package com.luis.alhendinfc.ui.training

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.Training
import com.luis.alhendinfc.domain.model.TrainingTaskItem
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.util.ImageViewer
import com.luis.alhendinfc.ui.util.openAttachment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingEditScreen(
    isNew: Boolean,
    dateLabel: String,
    training: Training?,
    clubs: List<OpponentClub>,
    libraryTasks: List<Task>,
    sessionTasks: List<TrainingTaskItem>,
    attachments: List<Attachment>,
    pendingFiles: List<PendingAttachment>,
    opponentClubId: Int?,
    notes: String,
    error: String?,
    busy: Boolean,
    onOpponentChange: (Int?) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddTask: (Int) -> Unit,
    onRemoveTask: (Int) -> Unit,
    onMoveTask: (Int, Boolean) -> Unit,
    onAddFile: (android.net.Uri, String, String) -> Unit,
    onRemovePendingFile: (android.net.Uri) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var clubMenu by remember { mutableStateOf(false) }
    var pickingTask by remember { mutableStateOf(false) }
    var viewingPath by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val visibleDate = training?.date?.takeIf { it.isNotBlank() } ?: dateLabel
    val selectedClub = clubs.firstOrNull { it.id == opponentClubId }
    val usedTaskIds = sessionTasks.map { it.task.id }.toSet()
    val availableTasks = libraryTasks.filter { it.id !in usedTaskIds }

    val filePicker = rememberLauncherForActivityResult(
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
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
        onAddFile(uri, name, mime)
    }

    if (pickingTask) {
        AlertDialog(
            onDismissRequest = { pickingTask = false },
            title = { Text("Añadir tarea") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (availableTasks.isEmpty()) {
                        Text("No hay más tareas en la biblioteca. Créalas primero en Tareas.")
                    } else {
                        availableTasks.forEach { task ->
                            TextButton(onClick = {
                                onAddTask(task.id)
                                pickingTask = false
                            }) {
                                Text(task.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingTask = false }) { Text("Cerrar") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar entrenamiento") },
            text = {
                Text("Se ocultará el entrenamiento, sus tareas de sesión y sus archivos. Las tareas de la biblioteca no se borran.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }

    viewingPath?.let { path ->
        ImageViewer(source = path, title = "Imagen del entrenamiento") {
            viewingPath = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) "Nuevo entrenamiento" else "Entrenamiento",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E2414),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0A2410), Color(0xFF0F2A14)))
                )
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Fecha", color = GreenMint, style = MaterialTheme.typography.labelMedium)
            Text(visibleDate.ifBlank { "—" }, color = Color.White, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(
                expanded = clubMenu,
                onExpandedChange = { clubMenu = it }
            ) {
                OutlinedTextField(
                    value = selectedClub?.name ?: "Sin rival",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rival (opcional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(clubMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = clubMenu,
                    onDismissRequest = { clubMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin rival") },
                        onClick = {
                            onOpponentChange(null)
                            clubMenu = false
                        }
                    )
                    clubs.forEach { club ->
                        DropdownMenuItem(
                            text = { Text(club.name) },
                            onClick = {
                                onOpponentChange(club.id)
                                clubMenu = false
                            }
                        )
                    }
                }
            }

            Text("Tareas", color = GreenMint, fontWeight = FontWeight.SemiBold)
            if (sessionTasks.isEmpty()) {
                Text("Ninguna tarea en esta sesión.", color = Color.White.copy(alpha = 0.7f))
            } else {
                sessionTasks.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.task.name, color = Color.White, fontWeight = FontWeight.Medium)
                            if (item.task.deletedAt != null) {
                                Text("Eliminada de la biblioteca", color = AmberAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(
                            onClick = { onMoveTask(item.task.id, true) },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = GreenMint)
                        }
                        IconButton(
                            onClick = { onMoveTask(item.task.id, false) },
                            enabled = index < sessionTasks.lastIndex
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar", tint = GreenMint)
                        }
                        IconButton(onClick = { onRemoveTask(item.task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = Color(0xFFFF8A80))
                        }
                    }
                }
            }
            TextButton(onClick = { pickingTask = true }) {
                Text("Añadir tarea de la biblioteca")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notas") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Fotos y documentos", color = GreenMint, fontWeight = FontWeight.SemiBold)
            attachments.forEach { att ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (att.isImage) viewingPath = att.localPath
                            else openAttachment(context, att)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(att.name.ifBlank { att.mimeType }, color = Color.White)
                    }
                    IconButton(onClick = { onDeleteAttachment(att) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar archivo", tint = Color(0xFFFF8A80))
                    }
                }
            }
            pendingFiles.forEach { pending ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(pending.name, color = AmberAccent, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemovePendingFile(pending.uri) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = Color(0xFFFF8A80))
                    }
                }
            }
            TextButton(
                onClick = {
                    filePicker.launch(
                        arrayOf(
                            "image/*",
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "text/plain"
                        )
                    )
                }
            ) { Text("Añadir archivo") }

            if (error != null) {
                Text(error, color = Color(0xFFFF8A80))
            }

            Button(
                onClick = onSave,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Guardando…" else "Guardar")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
