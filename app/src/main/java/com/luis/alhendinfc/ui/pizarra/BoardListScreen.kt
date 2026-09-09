package com.luis.alhendinfc.ui.pizarra

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenMint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListScreen(
    boards: List<Board>,
    onBack: () -> Unit,
    onOpen: (Board) -> Unit,
    onCreate: (String, (Int) -> Unit) -> Unit,
    onRename: (Board, String) -> Unit,
    onDuplicate: (Board, (Int) -> Unit) -> Unit,
    onDelete: (Board) -> Unit,
    tasksUsing: suspend (Board) -> Int
) {
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Board?>(null) }
    var pendingDelete by remember { mutableStateOf<Board?>(null) }
    var deleteMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (creating || renaming != null) {
        var name by remember { mutableStateOf(renaming?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { creating = false; renaming = null },
            title = { Text(if (renaming == null) "Nueva pizarra" else "Renombrar") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) return@TextButton
                        val current = renaming
                        if (current != null) onRename(current, trimmed)
                        else onCreate(trimmed) { }
                        creating = false
                        renaming = null
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { creating = false; renaming = null }) { Text("Cancelar") } }
        )
    }

    pendingDelete?.let { board ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar «${board.name}»") },
            text = { Text(deleteMessage.ifBlank { "La pizarra se ocultará." }) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(board)
                    pendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pizarras", fontWeight = FontWeight.Bold) },
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
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva pizarra")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A2410), Color(0xFF0F2A14), Color(0xFF061408))))
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (boards.isEmpty()) {
                Text("Aún no hay pizarras. Crea la primera.", color = AmberAccent, modifier = Modifier.padding(top = 24.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    items(boards, key = { it.id }) { board ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A22)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(board.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onOpen(board) }) { Text("Abrir") }
                                    IconButton(onClick = { renaming = board }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Renombrar", tint = GreenMint)
                                    }
                                    TextButton(onClick = { onDuplicate(board) { } }) { Text("Duplicar") }
                                    IconButton(onClick = {
                                        scope.launch {
                                            val used = tasksUsing(board)
                                            deleteMessage = if (used > 0) {
                                                "Está asociada a $used tarea(s). Al eliminar se quitará la asociación."
                                            } else {
                                                "La pizarra se ocultará."
                                            }
                                            pendingDelete = board
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
