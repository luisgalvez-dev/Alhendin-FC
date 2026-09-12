package com.luis.alhendinfc.ui.tasks

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.TaskRules
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.util.ImageViewer
import com.luis.alhendinfc.ui.util.LocalImageLoader

@Composable
fun TaskEditDialog(
    current: Task?,
    teamId: Int,
    currentImage: Attachment?,
    boards: List<Board>,
    onConfirm: (Task, imageUri: Uri?, removeImage: Boolean) -> Unit,
    onCreateBoard: (Task) -> Unit,
    onAssignBoard: (Task, Board) -> Unit,
    onOpenBoard: (Board) -> Unit,
    onClearBoard: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(current?.name ?: "") }
    var objective by remember { mutableStateOf(current?.objective ?: "") }
    var playerCountText by remember {
        mutableStateOf(current?.playerCount?.toString().orEmpty())
    }
    var durationText by remember {
        mutableStateOf(current?.durationMinutes?.toString().orEmpty())
    }
    var description by remember { mutableStateOf(current?.description ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingImage by remember { mutableStateOf<Uri?>(null) }
    var removeImage by remember { mutableStateOf(false) }
    var viewingSource by remember { mutableStateOf<String?>(null) }
    var selectedBoardSyncId by remember { mutableStateOf(current?.boardSyncId) }
    val context = LocalContext.current
    val previewPath = when {
        pendingImage != null -> pendingImage.toString()
        removeImage -> null
        else -> currentImage?.localPath
    }
    var preview by remember(previewPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(previewPath) {
        preview = LocalImageLoader.load(context, previewPath, maxSidePx = 256)
    }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImage = uri
        removeImage = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (current == null) "Nueva tarea" else "Editar tarea",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it },
                    label = { Text("Objetivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = playerCountText,
                    onValueChange = { playerCountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Número de jugadores") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duración (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Imagen de referencia (opcional)", fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A3A22))
                            .clickable {
                                if (previewPath != null) viewingSource = previewPath
                                else imagePicker.launch(arrayOf("image/*"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (preview != null) {
                            Image(
                                bitmap = preview!!,
                                contentDescription = "Imagen de la tarea",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(72.dp)
                            )
                        } else {
                            Text("Foto", color = GreenMint)
                        }
                    }
                    TextButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                        Text(if (preview != null) "Cambiar imagen" else "Añadir imagen")
                    }
                    if (preview != null) {
                        TextButton(onClick = {
                            pendingImage = null
                            removeImage = true
                        }) { Text("Eliminar") }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Pizarra", fontWeight = FontWeight.Medium)
                val linked = boards.firstOrNull { it.syncId == selectedBoardSyncId && it.syncId.isNotBlank() }
                if (current == null || current.id <= 0) {
                    Text("Guarda la tarea para asociar una pizarra.", color = GreenMint)
                } else {
                    if (linked != null) {
                        Text(linked.name, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { onOpenBoard(linked) }) { Text("Abrir pizarra") }
                        TextButton(onClick = {
                            selectedBoardSyncId = null
                            onClearBoard(current)
                        }) { Text("Quitar asociación") }
                    } else {
                        Text("Sin pizarra asociada", color = GreenMint)
                    }
                    TextButton(onClick = { onCreateBoard(current) }) { Text("Crear pizarra") }
                    if (boards.isNotEmpty()) {
                        Text("Elegir pizarra existente", color = GreenMint)
                        boards.forEach { board ->
                            val selected = board.syncId.isNotBlank() && board.syncId == selectedBoardSyncId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) Color(0xFF1F6B35) else Color(0xFF1A3A22)
                                    )
                                    .clickable {
                                        selectedBoardSyncId = board.syncId
                                        onAssignBoard(current, board)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    board.name,
                                    color = Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selected) {
                                    Text("Seleccionada", color = GreenMint, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = Color(0xFFFF8A80))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val playerCount = playerCountText.trim().toIntOrNull()
                    val durationMinutes = durationText.trim().toIntOrNull()
                    val validation = TaskRules.validate(name, playerCount, durationMinutes)
                    if (validation != null) {
                        error = validation
                        return@TextButton
                    }
                    onConfirm(
                        Task(
                            id = current?.id ?: 0,
                            teamId = current?.teamId ?: teamId,
                            name = name.trim(),
                            objective = objective.trim(),
                            playerCount = playerCount,
                            durationMinutes = durationMinutes,
                            description = description.trim(),
                            boardSyncId = selectedBoardSyncId,
                            syncId = current?.syncId.orEmpty(),
                            createdAt = current?.createdAt ?: 0L,
                            updatedAt = current?.updatedAt ?: 0L,
                            deletedAt = current?.deletedAt
                        ),
                        pendingImage,
                        removeImage
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
    viewingSource?.let { source ->
        ImageViewer(source = source, title = name.ifBlank { "Imagen" }) {
            viewingSource = null
        }
    }
}
