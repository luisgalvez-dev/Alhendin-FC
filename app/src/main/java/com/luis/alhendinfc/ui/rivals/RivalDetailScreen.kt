package com.luis.alhendinfc.ui.rivals

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.MatchReportRef
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.OpponentPlayer
import com.luis.alhendinfc.domain.model.OpponentPlayerRules
import com.luis.alhendinfc.domain.model.PlayRfaf
import com.luis.alhendinfc.domain.model.RivalAnalysis
import com.luis.alhendinfc.domain.model.RivalFicha
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

private val ScreenBg = Brush.verticalGradient(
    listOf(Color(0xFF0A2410), Color(0xFF0F2A14), Color(0xFF061408))
)
private val CardBg = Color(0xFF1A3A22)
private val TopBarBg = Color(0xFF0E2414)
private val Tabs = listOf("General", "Plantilla", "Análisis", "Archivos", "Enlaces")

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
    var tab by rememberSaveable { mutableStateOf(0) }
    var viewingPath by remember { mutableStateOf<String?>(null) }
    var creatingPlayer by remember { mutableStateOf(false) }
    var creatingLink by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
        onAddFile(uri, name, mime)
    }

    viewingPath?.let { path ->
        ImageViewer(source = path, title = club?.name ?: "Imagen") { viewingPath = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rival", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopBarBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (tab) {
                1 -> FloatingActionButton(
                    onClick = { creatingPlayer = true },
                    containerColor = GreenAccent,
                    contentColor = Color.Black
                ) { Icon(Icons.Default.Add, contentDescription = "Añadir jugador") }
                3 -> FloatingActionButton(
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
                    },
                    containerColor = GreenAccent,
                    contentColor = Color.Black
                ) { Icon(Icons.Default.Add, contentDescription = "Añadir archivo") }
                4 -> FloatingActionButton(
                    onClick = { creatingLink = true },
                    containerColor = GreenAccent,
                    contentColor = Color.Black
                ) { Icon(Icons.Default.Add, contentDescription = "Añadir enlace") }
                else -> {}
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .padding(padding)
        ) {
            val wide = maxWidth >= RivalFicha.TABLET_MIN_WIDTH_DP.dp
            val contentPad = if (wide) 24.dp else 16.dp
            Column(modifier = Modifier.fillMaxSize()) {
                if (club != null) {
                    RivalIdentityHeader(
                        club = club,
                        shieldPath = shieldPath,
                        wide = wide,
                        onViewShield = { viewingPath = it },
                        modifier = Modifier.padding(horizontal = contentPad, vertical = 10.dp)
                    )
                } else {
                    Text(
                        "Cargando rival…",
                        color = GreenMint,
                        modifier = Modifier.padding(contentPad)
                    )
                }
                ScrollableTabRow(
                    selectedTabIndex = tab,
                    containerColor = Color(0xFF143D1F),
                    contentColor = GreenLime,
                    edgePadding = contentPad,
                    indicator = { positions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(positions[tab]),
                            color = GreenAccent
                        )
                    }
                ) {
                    Tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(label, maxLines = 1) }
                        )
                    }
                }
                when (tab) {
                    0 -> if (club != null) GeneralTab(
                        club = club,
                        shieldPath = shieldPath,
                        wide = wide,
                        contentPad = contentPad,
                        onSave = onSaveClub,
                        onViewShield = { viewingPath = it }
                    )
                    1 -> PlantillaTab(
                        players = players,
                        query = playerQuery,
                        wide = wide,
                        contentPad = contentPad,
                        creating = creatingPlayer,
                        onCreatingChange = { creatingPlayer = it },
                        onQuery = onPlayerQuery,
                        onAdd = onAddPlayer,
                        onUpdate = onUpdatePlayer,
                        onDelete = onDeletePlayer
                    )
                    2 -> if (club != null) AnalysisTab(
                        clubId = club.id,
                        analysis = analysis,
                        wide = wide,
                        contentPad = contentPad,
                        onSave = {
                            onSaveAnalysis(it)
                            scope.launch { snackbarHostState.showSnackbar("Análisis guardado") }
                        }
                    )
                    3 -> FilesTab(
                        attachments = RivalFicha.sortAttachments(attachments),
                        matchReports = RivalFicha.sortReports(matchReports),
                        wide = wide,
                        contentPad = contentPad,
                        onDelete = onDeleteAttachment,
                        onViewImage = { viewingPath = it }
                    )
                    else -> LinksTab(
                        rivalName = club?.name.orEmpty(),
                        links = links,
                        wide = wide,
                        contentPad = contentPad,
                        creating = creatingLink,
                        onCreatingChange = { creatingLink = it },
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
}

@Composable
private fun RivalIdentityHeader(
    club: OpponentClub,
    shieldPath: String?,
    wide: Boolean,
    onViewShield: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stadium = RivalFicha.stadiumLine(club.stadium)
    val kit = RivalFicha.kitLine(club.kitColors)
    val swatches = RivalFicha.kitSwatches(club.kitColors)
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(if (wide) 18.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (wide) 16.dp else 12.dp)
        ) {
            RivalShieldThumb(
                shieldUri = shieldPath,
                fallback = RivalFicha.shieldInitials(club.name, club.shortName),
                size = if (wide) 84 else 64,
                onClick = if (!shieldPath.isNullOrBlank()) {
                    { onViewShield(shieldPath) }
                } else null
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    club.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = if (wide) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (club.shortName.isNotBlank() && !club.shortName.equals(club.name, ignoreCase = true)) {
                    Text(
                        club.shortName,
                        color = GreenMint,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (stadium != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = GreenMint,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(16.dp)
                        )
                        Text(
                            stadium,
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (kit != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        swatches.forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch.argb))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            )
                        }
                        Text(
                            kit,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralTab(
    club: OpponentClub,
    shieldPath: String?,
    wide: Boolean,
    contentPad: Dp,
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
            .padding(contentPad),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Datos del rival", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "El escudo se comparte entre dispositivos. Los cambios se guardan al pulsar Guardar.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RivalShieldThumb(
                shieldUri = shieldUri,
                fallback = RivalFicha.shieldInitials(name, shortName),
                size = if (wide) 72 else 56,
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
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("Nombre corto") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = stadium,
                    onValueChange = { stadium = it },
                    label = { Text("Estadio") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = kitColors,
                    onValueChange = { kitColors = it },
                    label = { Text("Equipación / colores") },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = shortName, onValueChange = { shortName = it }, label = { Text("Nombre corto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = stadium, onValueChange = { stadium = it }, label = { Text("Estadio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = kitColors, onValueChange = { kitColors = it }, label = { Text("Equipación / colores") }, modifier = Modifier.fillMaxWidth())
        }
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
        ) { Text("Guardar datos") }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun PlantillaTab(
    players: List<OpponentPlayer>,
    query: String,
    wide: Boolean,
    contentPad: Dp,
    creating: Boolean,
    onCreatingChange: (Boolean) -> Unit,
    onQuery: (String) -> Unit,
    onAdd: (String) -> Unit,
    onUpdate: (OpponentPlayer) -> Unit,
    onDelete: (OpponentPlayer) -> Unit
) {
    var editing by remember { mutableStateOf<OpponentPlayer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (creating || editing != null) {
        var name by remember { mutableStateOf(editing?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = {
                onCreatingChange(false)
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
                        onCreatingChange(false)
                        editing = null
                        error = null
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onCreatingChange(false)
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
            .padding(contentPad)
    ) {
        Text("Plantilla", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            RivalFicha.playerCountLabel(players.size) + if (query.isBlank()) " registrados" else " en la búsqueda",
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
                .padding(bottom = 12.dp)
        )
        if (players.isEmpty()) {
            EmptyHint(RivalFicha.emptyPlayers(query.isBlank()))
        } else if (wide) {
            players.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { player ->
                        PlayerRow(
                            player = player,
                            onEdit = { editing = player },
                            onDelete = { onDelete(player) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            players.forEach { player ->
                PlayerRow(
                    player = player,
                    onEdit = { editing = player },
                    onDelete = { onDelete(player) }
                )
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun PlayerRow(
    player: OpponentPlayer,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            player.name,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Acciones", tint = GreenMint)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        menu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = {
                        menu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun AnalysisTab(
    clubId: Int,
    analysis: RivalAnalysis?,
    wide: Boolean,
    contentPad: Dp,
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
            .padding(contentPad),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Análisis táctico", color = Color.White, fontWeight = FontWeight.Bold)
        if (!RivalFicha.analysisHasContent(analysis)) {
            EmptyHint(RivalFicha.emptyAnalysis())
        } else {
            RivalFicha.analysisUpdatedLabel(analysis?.updatedAt ?: 0L)?.let {
                Text(it, color = GreenMint, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${RivalFicha.analysisFilledCount(analysis)} apartados rellenados",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
            RivalFicha.analysisPreview(analysis)?.let { preview ->
                Text(preview, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (wide) {
            Section("Sistema")
            TwoFields("Sistema habitual", usualSystem, { usualSystem = it }, "Variantes", variants) { variants = it }
            Section("Con balón")
            TwoFields("Salida de balón", buildUp, { buildUp = it }, "Progresión", progression) { progression = it }
            Multi("Último tercio / finalización", finalThird) { finalThird = it }
            Section("Sin balón")
            TwoFields("Presión alta", highPress, { highPress = it }, "Bloque medio", midBlock) { midBlock = it }
            Multi("Bloque bajo", lowBlock) { lowBlock = it }
            Section("Transiciones")
            TwoFields("Ataque → defensa", transAd, { transAd = it }, "Defensa → ataque", transDa) { transDa = it }
            Section("A balón parado")
            TwoFields("Córners ofensivos", cornersOff, { cornersOff = it }, "Córners defensivos", cornersDef) { cornersDef = it }
            Multi("Faltas y otras acciones a balón parado", setPieces) { setPieces = it }
            Section("Evaluación")
            TwoFields("Fortalezas", strengths, { strengths = it }, "Debilidades", weaknesses) { weaknesses = it }
            TwoFields("Jugadores clave", keyPlayers, { keyPlayers = it }, "Notas generales", notes) { notes = it }
        } else {
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
        }
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
private fun Multi(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = 2,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun TwoFields(
    leftLabel: String,
    leftValue: String,
    onLeft: (String) -> Unit,
    rightLabel: String,
    rightValue: String,
    onRight: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Multi(leftLabel, leftValue, Modifier.weight(1f), onLeft)
        Multi(rightLabel, rightValue, Modifier.weight(1f), onRight)
    }
}

@Composable
private fun FilesTab(
    attachments: List<Attachment>,
    matchReports: List<MatchReportRef>,
    wide: Boolean,
    contentPad: Dp,
    onDelete: (Attachment) -> Unit,
    onViewImage: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPad)
    ) {
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ReportsBlock(matchReports, context, onViewImage)
                }
                Column(modifier = Modifier.weight(1f)) {
                    FilesBlock(attachments, context, onDelete, onViewImage)
                }
            }
        } else {
            ReportsBlock(matchReports, context, onViewImage)
            Spacer(Modifier.height(20.dp))
            FilesBlock(attachments, context, onDelete, onViewImage)
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ReportsBlock(
    matchReports: List<MatchReportRef>,
    context: android.content.Context,
    onViewImage: (String) -> Unit
) {
    Text("INFORMES", color = GreenMint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    Text(
        "Archivos adjuntos en un partido contra este rival. El mismo documento, no una copia.",
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
    )
    if (matchReports.isEmpty()) {
        EmptyHint(RivalFicha.emptyReports())
    } else {
        matchReports.forEach { report ->
            FileCard(
                title = RivalFicha.fileDisplayName(report.attachment),
                subtitle = listOfNotNull(
                    report.matchHeading,
                    report.attachment.typeLabel,
                    RivalFicha.fileDateLabel(report.attachment)
                ).joinToString(" · "),
                typeLabel = report.attachment.typeLabel,
                hint = report.attachment.transferHint,
                onOpen = { openOrDownloadAttachment(context, report.attachment, onViewImage) }
            )
        }
    }
}

@Composable
private fun FilesBlock(
    attachments: List<Attachment>,
    context: android.content.Context,
    onDelete: (Attachment) -> Unit,
    onViewImage: (String) -> Unit
) {
    Text("ARCHIVOS", color = GreenMint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    Text(
        "Documentos e imágenes propios de este rival.",
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
    )
    if (attachments.isEmpty()) {
        EmptyHint(RivalFicha.emptyFiles())
    } else {
        attachments.forEach { att ->
            FileCard(
                title = RivalFicha.fileDisplayName(att),
                subtitle = listOfNotNull(att.typeLabel, RivalFicha.fileDateLabel(att)).joinToString(" · "),
                typeLabel = att.typeLabel,
                hint = att.transferHint,
                onOpen = { openOrDownloadAttachment(context, att, onViewImage) },
                onDelete = { onDelete(att) }
            )
        }
    }
}

@Composable
private fun FileCard(
    title: String,
    subtitle: String,
    typeLabel: String,
    hint: String?,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    typeLabel.take(3).uppercase(),
                    color = GreenMint,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (hint != null) {
                    Text(hint, color = AmberAccent, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8A80))
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
    wide: Boolean,
    contentPad: Dp,
    creating: Boolean,
    onCreatingChange: (Boolean) -> Unit,
    onAdd: (type: String, label: String, url: String) -> Unit,
    onUpdate: (RivalLink) -> Unit,
    onDelete: (RivalLink) -> Unit,
    onMove: (linkId: Int, up: Boolean) -> Unit,
    onOpenFailed: () -> Unit
) {
    val context = LocalContext.current
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
                onCreatingChange(false)
                editing = null
            },
            onDismiss = {
                onCreatingChange(false)
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
            .padding(contentPad),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("PlayRFAF", color = GreenMint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
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
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = {
                        if (!openWebUrl(context, PlayRfaf.URL)) onOpenFailed()
                    }
                ) { Text("Abrir PlayRFAF") }
            }
        }

        Text("ENLACES", color = GreenMint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        if (links.isEmpty()) {
            EmptyHint(RivalFicha.emptyLinks())
        } else if (wide) {
            links.chunked(2).forEachIndexed { rowIndex, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEachIndexed { col, link ->
                        val index = rowIndex * 2 + col
                        LinkRow(
                            link = link,
                            canMoveUp = index > 0,
                            canMoveDown = index < links.lastIndex,
                            onOpen = {
                                if (!openWebUrl(context, link.url)) onOpenFailed()
                            },
                            onEdit = { editing = link },
                            onDelete = { onDelete(link) },
                            onMoveUp = { onMove(link.id, true) },
                            onMoveDown = { onMove(link.id, false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            links.forEachIndexed { index, link ->
                LinkRow(
                    link = link,
                    canMoveUp = index > 0,
                    canMoveDown = index < links.lastIndex,
                    onOpen = {
                        if (!openWebUrl(context, link.url)) onOpenFailed()
                    },
                    onEdit = { editing = link },
                    onDelete = { onDelete(link) },
                    onMoveUp = { onMove(link.id, true) },
                    onMoveDown = { onMove(link.id, false) }
                )
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun LinkRow(
    link: RivalLink,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    val glyph = when (RivalFicha.linkGlyph(link.type)) {
        RivalFicha.LinkGlyph.STAR -> Icons.Default.Star
        RivalFicha.LinkGlyph.PLAY -> Icons.Default.PlayArrow
        RivalFicha.LinkGlyph.INFO -> Icons.Default.Info
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(glyph, contentDescription = null, tint = AmberAccent, modifier = Modifier.padding(end = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    RivalFicha.linkTitle(link),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    RivalFicha.linkSubtitle(link),
                    color = GreenMint,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onOpen,
                enabled = RivalLinkRules.isOpenableUrl(link.url)
            ) { Text("Abrir") }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Acciones", tint = GreenMint)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text("Subir") },
                        enabled = canMoveUp,
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                        onClick = { menu = false; onMoveUp() }
                    )
                    DropdownMenuItem(
                        text = { Text("Bajar") },
                        enabled = canMoveDown,
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                        onClick = { menu = false; onMoveDown() }
                    )
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onDelete() })
                }
            }
        }
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

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        color = AmberAccent,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
