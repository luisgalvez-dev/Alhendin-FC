package com.luis.alhendinfc.ui.rivals

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
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
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.RfafStandings
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.util.ImageViewer
import com.luis.alhendinfc.ui.util.LocalImageLoader
import com.luis.alhendinfc.ui.util.openExternalUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RivalListScreen(
    team: Team?,
    clubs: List<OpponentClub>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenClub: (OpponentClub) -> Unit,
    onAddClub: (name: String, shortName: String, stadium: String, shieldUri: String?, kitColors: String) -> Unit,
    onDeleteClub: (OpponentClub) -> Unit,
    onBack: () -> Unit,
    opponentShields: Map<String, Attachment> = emptyMap()
) {
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<OpponentClub?>(null) }
    var viewingShield by remember { mutableStateOf<String?>(null) }
    var standingsError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun openStandings() {
        standingsError = !openExternalUrl(context, RfafStandings.URL)
    }

    if (creating) {
        RivalEditorDialog(
            title = "Nuevo rival",
            initial = null,
            onConfirm = { name, short, stadium, shield, kit ->
                onAddClub(name, short, stadium, shield, kit)
                creating = false
            },
            onDismiss = { creating = false }
        )
    }

    pendingDelete?.let { club ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar «${club.name}»") },
            text = { Text("El rival se ocultará. El análisis, enlaces y archivos asociados se conservan en tombstone según el contrato.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClub(club)
                    pendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    viewingShield?.let { source ->
        ImageViewer(source = source, title = "Escudo") { viewingShield = null }
    }

    if (standingsError) {
        AlertDialog(
            onDismissRequest = { standingsError = false },
            title = { Text("No se pudo abrir") },
            text = { Text("No hay ninguna aplicación para abrir la clasificación RFAF.") },
            confirmButton = {
                TextButton(onClick = { standingsError = false }) { Text("Aceptar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rivales", fontWeight = FontWeight.Bold)
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
                actions = {
                    IconButton(onClick = { openStandings() }) {
                        Icon(Icons.Default.Star, contentDescription = "Ver clasificación")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E2414),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { creating = true },
                containerColor = GreenAccent,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear rival")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0A2410), Color(0xFF0F2A14), Color(0xFF061408)))
                )
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Buscar por nombre") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { openStandings() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = AmberAccent)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ver clasificación", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("RFAF · se abre en el navegador", style = MaterialTheme.typography.bodySmall, color = GreenMint)
                    }
                }
            }
            if (clubs.isEmpty()) {
                Text(
                    if (searchQuery.isBlank()) "Aún no hay rivales. Crea el primero."
                    else "Ningún rival coincide con la búsqueda.",
                    color = AmberAccent,
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clubs, key = { it.id }) { club ->
                        val shieldPath = SharedMedia.displayPath(opponentShields[club.syncId], club.shieldUri)
                        RivalRow(
                            club = club,
                            shieldPath = shieldPath,
                            onOpen = { onOpenClub(club) },
                            onViewShield = { shieldPath?.let { viewingShield = it } },
                            onDelete = { pendingDelete = club }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RivalRow(
    club: OpponentClub,
    shieldPath: String?,
    onOpen: () -> Unit,
    onViewShield: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A22)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RivalShieldThumb(
                shieldUri = shieldPath,
                fallback = club.displayShort.take(1).uppercase(),
                size = 48,
                onClick = if (!shieldPath.isNullOrBlank()) onViewShield else null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(club.name, fontWeight = FontWeight.Bold, color = Color.White)
                val meta = listOfNotNull(
                    club.shortName.takeIf { it.isNotBlank() },
                    club.stadium.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = GreenMint)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
            }
        }
    }
}

@Composable
internal fun RivalShieldThumb(
    shieldUri: String?,
    fallback: String,
    size: Int,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var bitmap by remember(shieldUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(shieldUri) {
        bitmap = LocalImageLoader.load(context, shieldUri, maxSidePx = (size * 3).coerceAtLeast(96))
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(GreenAccent.copy(alpha = 0.25f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Escudo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(fallback.ifBlank { "?" }, fontWeight = FontWeight.Bold, color = GreenMint)
        }
    }
}

@Composable
internal fun RivalEditorDialog(
    title: String,
    initial: OpponentClub?,
    onConfirm: (name: String, shortName: String, stadium: String, shieldUri: String?, kitColors: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var shortName by remember { mutableStateOf(initial?.shortName.orEmpty()) }
    var stadium by remember { mutableStateOf(initial?.stadium.orEmpty()) }
    var kitColors by remember { mutableStateOf(initial?.kitColors.orEmpty()) }
    var shieldUri by remember { mutableStateOf(initial?.shieldUri) }
    var viewing by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
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
        shieldUri = uri.toString()
    }

    if (viewing && !shieldUri.isNullOrBlank()) {
        ImageViewer(source = shieldUri!!, title = "Escudo") { viewing = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RivalShieldThumb(
                        shieldUri = shieldUri,
                        fallback = shortName.ifBlank { name }.take(1).uppercase(),
                        size = 64,
                        onClick = {
                            if (!shieldUri.isNullOrBlank()) viewing = true
                            else imagePicker.launch(arrayOf("image/*"))
                        }
                    )
                    Column {
                        TextButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                            Text(if (shieldUri == null) "Añadir escudo" else "Cambiar escudo")
                        }
                        if (shieldUri != null) {
                            TextButton(onClick = { shieldUri = null }) { Text("Quitar escudo") }
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = shortName, onValueChange = { shortName = it }, label = { Text("Nombre corto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stadium, onValueChange = { stadium = it }, label = { Text("Estadio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = kitColors, onValueChange = { kitColors = it }, label = { Text("Equipación / colores") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), shortName.trim(), stadium.trim(), shieldUri, kitColors.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
