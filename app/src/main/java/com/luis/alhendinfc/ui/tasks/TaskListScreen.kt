package com.luis.alhendinfc.ui.tasks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.util.ImageViewer
import com.luis.alhendinfc.ui.util.LocalImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    team: Team?,
    tasks: List<Task>,
    taskImages: Map<String, Attachment>,
    boards: List<Board>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAdd: (Task, android.net.Uri?) -> Unit,
    onUpdate: (Task, android.net.Uri?, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onCreateBoard: (Task) -> Unit,
    onAssignBoard: (Task, Board) -> Unit,
    onOpenBoard: (Board) -> Unit,
    onClearBoard: (Task) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Task?>(null) }
    var viewingPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tasks, editing?.id) {
        val id = editing?.id ?: return@LaunchedEffect
        tasks.firstOrNull { it.id == id }?.let { editing = it }
    }

    if (creating || editing != null) {
        TaskEditDialog(
            current = editing,
            teamId = team?.id ?: 0,
            currentImage = (editing ?: null)?.syncId?.let { taskImages[it] },
            boards = boards,
            onConfirm = { task, imageUri, removeImage ->
                if (editing != null) onUpdate(task, imageUri, removeImage) else onAdd(task, imageUri)
                creating = false
                editing = null
            },
            onCreateBoard = onCreateBoard,
            onAssignBoard = onAssignBoard,
            onOpenBoard = onOpenBoard,
            onClearBoard = onClearBoard,
            onDismiss = {
                creating = false
                editing = null
            }
        )
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar «${task.name}»") },
            text = {
                Text("La tarea se ocultará de la biblioteca. ¿Eliminarla?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(task)
                    pendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    viewingPath?.let { path ->
        ImageViewer(source = path, title = "Imagen de tarea") {
            viewingPath = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tareas", fontWeight = FontWeight.Bold)
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
                Icon(Icons.Default.Add, contentDescription = "Añadir tarea")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Buscar por nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (tasks.isEmpty()) {
                Text(
                    if (searchQuery.isBlank()) {
                        "Aún no hay tareas. Pulsa + para crear un ejercicio reutilizable."
                    } else {
                        "Ninguna tarea coincide con «$searchQuery»."
                    },
                    color = AmberAccent
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tasks, key = { it.id }) { task ->
                            TaskRow(
                            task = task,
                            image = taskImages[task.syncId],
                            onOpen = { editing = task },
                            onViewImage = { viewingPath = it },
                            onEdit = { editing = task },
                            onDelete = { pendingDelete = task }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    image: Attachment?,
    onOpen: () -> Unit,
    onViewImage: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val meta = buildList {
        if (task.objective.isNotBlank()) add(task.objective)
        task.playerCount?.let { add("$it jug.") }
        task.durationMinutes?.let { add("$it min") }
    }.joinToString(" · ")
    val context = LocalContext.current
    var bitmap by remember(image?.localPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(image?.localPath) {
        bitmap = LocalImageLoader.load(context, image?.localPath, maxSidePx = 96)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = "Ver imagen",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            image?.localPath?.takeIf { it.isNotBlank() }?.let(onViewImage)
                        }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenMint
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = GreenMint)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
            }
        }
    }
}
