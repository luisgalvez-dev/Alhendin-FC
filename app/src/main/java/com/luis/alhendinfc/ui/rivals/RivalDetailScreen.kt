package com.luis.alhendinfc.ui.rivals

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.MatchReportRef
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.OpponentPlayer
import com.luis.alhendinfc.domain.model.OpponentPlayerRules
import com.luis.alhendinfc.domain.model.PlayRfaf
import com.luis.alhendinfc.domain.model.RivalAnalysis
import com.luis.alhendinfc.domain.model.RivalLink
import com.luis.alhendinfc.domain.model.RivalLinkRules
import com.luis.alhendinfc.domain.model.RivalLinkType
import com.luis.alhendinfc.ui.theme.AmberAccent
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenLime
import com.luis.alhendinfc.ui.theme.GreenMint
import com.luis.alhendinfc.ui.util.ImageViewer
import com.luis.alhendinfc.ui.util.openOrDownloadAttachment
import com.luis.alhendinfc.ui.util.openWebUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RivalDetailScreen(
    club: OpponentClub?,
    analysis: RivalAnalysis?,
    players: List<OpponentPlayer>,
    playerQuery: String,
    links: List<RivalLink>,
    attachments: List<Attachment>,
    matchReports: List<MatchReportRef>,
    onSaveClub: (OpponentClub, Uri?, Boolean) -> Unit,
    onSaveAnalysis: (RivalAnalysis) -> Unit,
    onPlayerQuery: (String) -> Unit,
    onAddPlayer: (String) -> Unit,
    onUpdatePlayer: (OpponentPlayer) -> Unit,
    onDeletePlayer: (OpponentPlayer) -> Unit,
    onAddLink: (type: String, label: String, url: String) -> Unit,
    onUpdateLink: (RivalLink) -> Unit,
    onDeleteLink: (RivalLink) -> Unit,
    onMoveLink: (linkId: Int, up: Boolean) -> Unit,
    onAddFile: (Uri, String, String) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onBack: () -> Unit,
    shieldPath: String? = club?.shieldUri
) {
    var tab by remember { mutableIntStateOf(0) }
    var viewingPath by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tabs = listOf("Resumen", "Plantilla", "Análisis", "Archivos", "Enlaces")

    viewingPath?.let { path ->
        ImageViewer(source = path, title = club?.name ?: "Imagen") { viewingPath = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(club?.name ?: "Rival", fontWeight = FontWeight.Bold)
                        Text(
                            club?.displayShort ?: "",
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0A2410), Color(0xFF0F2A14), Color(0xFF061408)))
                )
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color(0xFF143D1F),
                contentColor = GreenLime,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[tab]),
                        color = GreenAccent
                    )
                }
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> if (club != null) SummaryTab(
                    club = club,
                    shieldPath = shieldPath,
                    onSave = onSaveClub,
                    onViewShield = { viewingPath = it }
                )
                1 -> PlantillaTab(
                    players = players,
                    query = playerQuery,
                    onQuery = onPlayerQuery,
                    onAdd = onAddPlayer,
                    onUpdate = onUpdatePlayer,
                    onDelete = onDeletePlayer
                )
                2 -> if (club != null) AnalysisTab(
                    clubId = club.id,
                    analysis = analysis,
                    onSave = {
                        onSaveAnalysis(it)
                        scope.launch { snackbarHostState.showSnackbar("Análisis guardado") }
                    }
                )
                3 -> FilesTab(
                    attachments = attachments,
                    matchReports = matchReports,
                    onAddFile = onAddFile,
                    onDelete = onDeleteAttachment,
                    onViewImage = { viewingPath = it }
                )
                else -> LinksTab(
                    rivalName = club?.name.orEmpty(),
                    links = links,
                    onAdd = onAddLink,
                    onUpdate = onUpdateLink,
                    onDelete = onDeleteLink,
                    onMove = onMoveLink,
                    onOpenFailed = {
                        scope.launch {
                            snackbarHostState.showSnackbar("No hay ninguna aplicación para abrir este enlace.")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryTab(
    club: OpponentClub,
    shieldPath: String?,
    onSave: (OpponentClub, Uri?, Boolean) -> Unit,
    onViewShield: (String) -> Unit
) {
    val context = LocalContext.current
    var name by remember(club.id, club.updatedAt) { mutableStateOf(club.name) }
    var shortName by remember(club.id, club.updatedAt) { mutableStateOf(club.shortName) }
    var stadium by remember(club.id, club.updatedAt) { mutableStateOf(club.stadium) }
    var kitColors by remember(club.id, club.updatedAt) { mutableStateOf(club.kitColors) }
    var shieldUri by remember(club.id, club.updatedAt, shieldPath) { mutableStateOf(shieldPath) }
    var pickedUri by remember(club.id, club.updatedAt) { mutableStateOf<Uri?>(null) }
    var clearedShield by remember(club.id, club.updatedAt) { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
        shieldUri = uri.toString()
        pickedUri = uri
        clearedShield = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RivalShieldThumb(
                shieldUri = shieldUri,
                fallback = shortName.ifBlank { name }.take(1).uppercase(),
                size = 72,
                onClick = {
                    if (!shieldUri.isNullOrBlank()) onViewShield(shieldUri!!)
                    else picker.launch(arrayOf("image/*"))
                }
            )
            Column {
                TextButton(onClick = { picker.launch(arrayOf("image/*")) }) {
                    Text(if (shieldUri == null) "Añadir escudo" else "Cambiar escudo")
                }
                if (shieldUri != null) {
                    TextButton(onClick = {
                        shieldUri = null
                        pickedUri = null
                        clearedShield = true
                    }) { Text("Quitar escudo") }
                }
            }
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = shortName, onValueChange = { shortName = it }, label = { Text("Nombre corto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = stadium, onValueChange = { stadium = it }, label = { Text("Estadio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = kitColors, onValueChange = { kitColors = it }, label = { Text("Equipación / colores") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                onSave(
                    club.copy(
                        name = name.trim(),
                        shortName = shortName.trim(),
                        stadium = stadium.trim(),
                        kitColors = kitColors.trim()
                    ),
                    pickedUri,
                    clearedShield
                )
            },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar resumen") }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PlantillaTab(
    players: List<OpponentPlayer>,
    query: String,
    onQuery: (String) -> Unit,
    onAdd: (String) -> Unit,
    onUpdate: (OpponentPlayer) -> Unit,
    onDelete: (OpponentPlayer) -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<OpponentPlayer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (creating || editing != null) {
        var name by remember { mutableStateOf(editing?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = {
                creating = false
                editing = null
                error = null
            },
            title = { Text(if (editing == null) "Nuevo jugador" else "Editar nombre") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) Text(error!!, color = Color(0xFFFF8A80))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val validation = OpponentPlayerRules.validate(name)
                        if (validation != null) {
                            error = validation
                            return@TextButton
                        }
                        if (editing != null) onUpdate(editing!!.copy(name = name.trim()))
                        else onAdd(name.trim())
                        creating = false
                        editing = null
                        error = null
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    creating = false
                    editing = null
                    error = null
                }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Plantilla", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            if (players.size == 1) "1 jugador registrado"
            else "${players.size} jugadores registrados",
            color = GreenMint,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Buscar por nombre") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        if (players.isEmpty()) {
            Text(
                if (query.isBlank()) "Aún no hay jugadores. Añade los nombres que conozcas."
                else "Ningún jugador coincide con la búsqueda.",
                color = AmberAccent
            )
        }
        players.forEach { player ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(player.name, color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = { editing = player }) { Text("Editar") }
                IconButton(onClick = { onDelete(player) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
                }
            }
        }
        TextButton(onClick = { creating = true }) { Text("Añadir jugador") }
    }
}

@Composable
private fun AnalysisTab(
    clubId: Int,
    analysis: RivalAnalysis?,
    onSave: (RivalAnalysis) -> Unit
) {
    var usualSystem by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.usualSystem.orEmpty()) }
    var variants by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.variants.orEmpty()) }
    var buildUp by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.buildUp.orEmpty()) }
    var progression by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.progression.orEmpty()) }
    var finalThird by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.finalThird.orEmpty()) }
    var highPress by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.highPress.orEmpty()) }
    var midBlock by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.midBlock.orEmpty()) }
    var lowBlock by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.lowBlock.orEmpty()) }
    var transAd by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.transAttackToDefense.orEmpty()) }
    var transDa by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.transDefenseToAttack.orEmpty()) }
    var cornersOff by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.cornersOffensive.orEmpty()) }
    var cornersDef by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.cornersDefensive.orEmpty()) }
    var setPieces by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.setPieces.orEmpty()) }
    var strengths by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.strengths.orEmpty()) }
    var weaknesses by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.weaknesses.orEmpty()) }
    var keyPlayers by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.keyPlayers.orEmpty()) }
    var notes by remember(analysis?.id, analysis?.updatedAt) { mutableStateOf(analysis?.generalNotes.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Section("Sistema")
        Multi("Sistema habitual", usualSystem) { usualSystem = it }
        Multi("Variantes", variants) { variants = it }
        Section("Con balón")
        Multi("Salida de balón", buildUp) { buildUp = it }
        Multi("Progresión", progression) { progression = it }
        Multi("Último tercio / finalización", finalThird) { finalThird = it }
        Section("Sin balón")
        Multi("Presión alta", highPress) { highPress = it }
        Multi("Bloque medio", midBlock) { midBlock = it }
        Multi("Bloque bajo", lowBlock) { lowBlock = it }
        Section("Transiciones")
        Multi("Ataque → defensa", transAd) { transAd = it }
        Multi("Defensa → ataque", transDa) { transDa = it }
        Section("A balón parado")
        Multi("Córners ofensivos", cornersOff) { cornersOff = it }
        Multi("Córners defensivos", cornersDef) { cornersDef = it }
        Multi("Faltas y otras acciones a balón parado", setPieces) { setPieces = it }
        Section("Evaluación")
        Multi("Fortalezas", strengths) { strengths = it }
        Multi("Debilidades", weaknesses) { weaknesses = it }
        Multi("Jugadores clave", keyPlayers) { keyPlayers = it }
        Multi("Notas generales", notes) { notes = it }
        Button(
            onClick = {
                onSave(
                    (analysis ?: RivalAnalysis(opponentClubId = clubId)).copy(
                        usualSystem = usualSystem,
                        variants = variants,
                        buildUp = buildUp,
                        progression = progression,
                        finalThird = finalThird,
                        highPress = highPress,
                        midBlock = midBlock,
                        lowBlock = lowBlock,
                        transAttackToDefense = transAd,
                        transDefenseToAttack = transDa,
                        cornersOffensive = cornersOff,
                        cornersDefensive = cornersDef,
                        setPieces = setPieces,
                        strengths = strengths,
                        weaknesses = weaknesses,
                        keyPlayers = keyPlayers,
                        generalNotes = notes
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar análisis") }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = GreenMint, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Multi(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FilesTab(
    attachments: List<Attachment>,
    matchReports: List<MatchReportRef>,
    onAddFile: (Uri, String, String) -> Unit,
    onDelete: (Attachment) -> Unit,
    onViewImage: (String) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
        onAddFile(uri, name, mime)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Archivos del rival", color = GreenMint, fontWeight = FontWeight.SemiBold)
        if (attachments.isEmpty()) {
            Text("Sin archivos. Puedes añadir imágenes, PDF u otros documentos.", color = AmberAccent)
        }
        attachments.forEach { att ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { openOrDownloadAttachment(context, att, onViewImage) },
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(att.name.ifBlank { att.mimeType }, color = Color.White)
                        att.transferHint?.let { hint ->
                            Text(hint, color = AmberAccent, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                IconButton(onClick = { onDelete(att) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
                }
            }
        }
        TextButton(
            onClick = {
                picker.launch(
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

        Spacer(Modifier.height(16.dp))
        Text("Informes de partidos", color = GreenMint, fontWeight = FontWeight.SemiBold)
        Text(
            "Archivos que hayas adjuntado en un partido contra este rival (el mismo documento, no una copia). El análisis táctico sigue en la pestaña Análisis.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
        if (matchReports.isEmpty()) {
            Text("Ningún partido contra este rival tiene archivos adjuntos.", color = AmberAccent)
        }
        matchReports.forEach { report ->
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(report.matchHeading, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                TextButton(
                    onClick = {
                        openOrDownloadAttachment(context, report.attachment, onViewImage)
                    }
                ) {
                    Text(
                        "${report.attachment.name.ifBlank { report.attachment.mimeType }} · ${report.attachment.typeLabel}",
                        color = Color.White
                    )
                    report.attachment.transferHint?.let { hint ->
                        Text(hint, color = AmberAccent, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinksTab(
    rivalName: String,
    links: List<RivalLink>,
    onAdd: (type: String, label: String, url: String) -> Unit,
    onUpdate: (RivalLink) -> Unit,
    onDelete: (RivalLink) -> Unit,
    onMove: (linkId: Int, up: Boolean) -> Unit,
    onOpenFailed: () -> Unit
) {
    val context = LocalContext.current
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RivalLink?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    if (creating || editing != null) {
        LinkEditorDialog(
            current = editing,
            onConfirm = { type, label, url ->
                if (!RivalLinkRules.isOpenableUrl(url)) {
                    urlError = "La URL debe empezar por http:// o https://"
                    return@LinkEditorDialog
                }
                urlError = null
                if (editing != null) onUpdate(editing!!.copy(type = type, label = label, url = url))
                else onAdd(type, label, url)
                creating = false
                editing = null
            },
            onDismiss = {
                creating = false
                editing = null
                urlError = null
            },
            extraError = urlError
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ENLACES", color = GreenMint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A22)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!openWebUrl(context, PlayRfaf.URL)) onOpenFailed()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ver partidos en PlayRFAF", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        PlayRfaf.searchHint(rivalName),
                        color = GreenMint,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = {
                        if (!openWebUrl(context, PlayRfaf.URL)) onOpenFailed()
                    }
                ) { Text("Abrir PlayRFAF") }
            }
        }
        if (links.isEmpty()) {
            Text("Sin otros enlaces. Añade RFAF, YouTube u otros de prueba.", color = Color.White.copy(alpha = 0.7f))
        }
        links.forEachIndexed { index, link ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        link.label.ifBlank { RivalLinkType.labelOf(link.type) },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(RivalLinkType.labelOf(link.type), color = GreenMint, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                    onClick = {
                        if (!openWebUrl(context, link.url)) onOpenFailed()
                    },
                    enabled = RivalLinkRules.isOpenableUrl(link.url)
                ) { Text("Abrir") }
                IconButton(onClick = { onMove(link.id, true) }, enabled = index > 0) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = GreenMint)
                }
                IconButton(onClick = { onMove(link.id, false) }, enabled = index < links.lastIndex) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar", tint = GreenMint)
                }
                TextButton(onClick = { editing = link }) { Text("Editar") }
                IconButton(onClick = { onDelete(link) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
                }
            }
        }
        TextButton(onClick = { creating = true }) { Text("Añadir otro enlace") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkEditorDialog(
    current: RivalLink?,
    onConfirm: (type: String, label: String, url: String) -> Unit,
    onDismiss: () -> Unit,
    extraError: String?
) {
    var type by remember { mutableStateOf(current?.type ?: RivalLinkType.CUSTOM) }
    var label by remember { mutableStateOf(current?.label.orEmpty()) }
    var url by remember { mutableStateOf(current?.url.orEmpty()) }
    var menu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == null) "Nuevo enlace" else "Editar enlace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                    OutlinedTextField(
                        value = RivalLinkType.labelOf(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menu) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        RivalLinkType.GENERIC.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(RivalLinkType.labelOf(option)) },
                                onClick = {
                                    type = option
                                    menu = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Etiqueta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (extraError != null) Text(extraError, color = Color(0xFFFF8A80))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(type, label.trim(), url.trim()) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
