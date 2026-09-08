package com.luis.alhendinfc.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.TaskRules

@Composable
fun TaskEditDialog(
    current: Task?,
    teamId: Int,
    onConfirm: (Task) -> Unit,
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
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = androidx.compose.ui.graphics.Color(0xFFFF8A80))
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
                            boardSyncId = current?.boardSyncId,
                            syncId = current?.syncId.orEmpty(),
                            createdAt = current?.createdAt ?: 0L,
                            updatedAt = current?.updatedAt ?: 0L,
                            deletedAt = current?.deletedAt
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
