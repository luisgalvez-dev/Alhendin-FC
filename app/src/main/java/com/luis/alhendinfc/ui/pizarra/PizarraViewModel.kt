package com.luis.alhendinfc.ui.pizarra

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.files.DiskFileStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.domain.model.BoardBackgroundType
import com.luis.alhendinfc.domain.model.BoardNormPoint
import com.luis.alhendinfc.domain.model.BoardObject
import com.luis.alhendinfc.domain.model.BoardObjectType
import com.luis.alhendinfc.domain.model.BoardScene
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.BoardRepository
import com.luis.alhendinfc.domain.repository.TaskRepositoryImpl
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DrawTool {
    PEN,
    ARROW,
    OVAL,
    ERASER
}

enum class BoardEditorTool {
    SELECT,
    PEN,
    ARROW,
    OVAL,
    ERASER,
    BLUE_PLAYER,
    RED_PLAYER,
    BALL,
    CONE,
    GOAL,
    MINI_GOAL,
    TEXT
}

enum class BoardBackground {
    FIELD,
    IMAGE,
    VIDEO
}

data class BoardPoint(val x: Float, val y: Float)

data class DrawStroke(
    val tool: DrawTool,
    val colorArgb: Long,
    val width: Float,
    val points: List<BoardPoint>
)

object PizarraColors {
    const val WHITE = 0xFFFFFFFFL
    const val YELLOW = 0xFFFFEB3BL
    const val RED = 0xFFFF5252L
    const val BLUE = 0xFF40C4FFL
    const val BLACK = 0xFF212121L
}

data class PizarraUiState(
    val boardId: Int = 0,
    val boardName: String = "Pizarra",
    val dirty: Boolean = false,
    val saved: Boolean = false,
    val tool: BoardEditorTool = BoardEditorTool.PEN,
    val colorArgb: Long = PizarraColors.WHITE,
    val strokeWidth: Float = 8f,
    val background: BoardBackground = BoardBackground.FIELD,
    val mediaUri: Uri? = null,
    val objects: List<BoardObject> = emptyList(),
    val currentStroke: DrawStroke? = null,
    val selectedObjectId: String? = null,
    val isVideoPlaying: Boolean = false,
    val videoSeekPulse: VideoSeekPulse? = null,
    val pendingText: String? = null
)

data class VideoSeekPulse(val deltaMs: Long, val id: Long = System.nanoTime())

class PizarraViewModel(
    private val boardId: Int,
    private val teamId: Int,
    private val taskId: Int,
    private val boards: BoardRepository,
    private val tasks: TaskRepositoryImpl,
    private val attachments: AttachmentRepositoryImpl,
    private val fileStore: AndroidAttachmentStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PizarraUiState())
    val uiState: StateFlow<PizarraUiState> = _uiState.asStateFlow()

    private var mediaAttachmentSyncId: String? = null
    private var loadedSyncId: String = ""
    /** Evita un segundo sello en el mismo gesto (press + drag/release). */
    private var stampConsumedThisGesture: Boolean = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (boardId <= 0) return
        val board = boards.getOnce(boardId) ?: return
        loadedSyncId = board.syncId
        val scene = BoardScene.fromJson(board.sceneJson)
        mediaAttachmentSyncId = scene.mediaAttachmentSyncId
        val mediaUri = resolveMediaUri(scene)
        _uiState.update {
            it.copy(
                boardId = board.id,
                boardName = board.name,
                dirty = false,
                background = when (scene.background) {
                    BoardBackgroundType.IMAGE -> BoardBackground.IMAGE
                    BoardBackgroundType.VIDEO -> BoardBackground.VIDEO
                    else -> BoardBackground.FIELD
                },
                mediaUri = mediaUri,
                objects = scene.objects,
                currentStroke = null,
                selectedObjectId = null,
                isVideoPlaying = false
            )
        }
    }

    private suspend fun resolveMediaUri(scene: BoardScene): Uri? {
        val syncId = scene.mediaAttachmentSyncId ?: return null
        val items = attachments.getActiveByParent(AttachmentParentType.BOARD, loadedSyncId).first()
        val path = items.firstOrNull { it.syncId == syncId }?.localPath ?: return null
        return Uri.fromFile(File(path))
    }

    fun setTool(tool: BoardEditorTool) {
        stampConsumedThisGesture = false
        _uiState.update { it.copy(tool = tool, selectedObjectId = if (tool == BoardEditorTool.SELECT) it.selectedObjectId else null) }
    }

    fun setColor(colorArgb: Long) {
        _uiState.update { it.copy(colorArgb = colorArgb) }
    }

    fun setStrokeWidth(width: Float) {
        _uiState.update { it.copy(strokeWidth = width) }
    }

    fun setPendingText(text: String?) {
        _uiState.update { it.copy(pendingText = text) }
    }

    fun useDefaultField() {
        mediaAttachmentSyncId = null
        _uiState.update {
            it.copy(
                background = BoardBackground.FIELD,
                mediaUri = null,
                isVideoPlaying = false,
                dirty = true
            )
        }
    }

    fun setImage(uri: Uri) {
        importMedia(uri, isVideo = false)
    }

    fun setVideo(uri: Uri) {
        importMedia(uri, isVideo = true)
    }

    private fun importMedia(uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            val board = currentBoard() ?: return@launch
            val attachmentSync = UUID.randomUUID().toString()
            val name = fileStore.queryDisplayName(uri) ?: if (isVideo) "video" else "imagen"
            val mime = fileStore.queryMimeType(uri)
                ?: if (isVideo) "video/mp4" else "image/jpeg"
            val path = fileStore.importUri(uri, name, attachmentSync)
            attachments.add(
                parentType = AttachmentParentType.BOARD,
                parentSyncId = board.syncId,
                mimeType = mime,
                name = name,
                localPath = path,
                syncId = attachmentSync
            )
            mediaAttachmentSyncId = attachmentSync
            _uiState.update {
                it.copy(
                    background = if (isVideo) BoardBackground.VIDEO else BoardBackground.IMAGE,
                    mediaUri = Uri.fromFile(File(path)),
                    isVideoPlaying = false,
                    dirty = true
                )
            }
        }
    }

    fun setVideoPlaying(playing: Boolean) {
        _uiState.update { it.copy(isVideoPlaying = playing) }
    }

    fun seekVideo(deltaMs: Long) {
        _uiState.update { it.copy(videoSeekPulse = VideoSeekPulse(deltaMs = deltaMs)) }
    }

    fun onPress(point: BoardPoint) {
        val state = _uiState.value
        if (BoardEditorLogic.shouldPlaceStamp(state.tool, stampConsumedThisGesture, state.pendingText)) {
            placeStamp(state, point)
            return
        }
        when (state.tool) {
            BoardEditorTool.SELECT -> {
                val hit = BoardEditorLogic.hitTest(state.objects, point)
                _uiState.update { it.copy(selectedObjectId = hit?.objectId, currentStroke = null) }
            }
            BoardEditorTool.PEN, BoardEditorTool.ARROW, BoardEditorTool.OVAL, BoardEditorTool.ERASER -> {
                val drawTool = when (state.tool) {
                    BoardEditorTool.ARROW -> DrawTool.ARROW
                    BoardEditorTool.OVAL -> DrawTool.OVAL
                    BoardEditorTool.ERASER -> DrawTool.ERASER
                    else -> DrawTool.PEN
                }
                _uiState.update {
                    it.copy(
                        currentStroke = DrawStroke(drawTool, state.colorArgb, state.strokeWidth, listOf(point)),
                        selectedObjectId = null
                    )
                }
            }
            else -> return
        }
    }

    fun onDrag(point: BoardPoint) {
        val state = _uiState.value
        if (state.tool == BoardEditorTool.SELECT) {
            val id = state.selectedObjectId ?: return
            _uiState.update { current ->
                current.copy(
                    objects = current.objects.map { obj ->
                        if (obj.objectId != id) obj
                        else if (obj.type in BoardObjectType.STROKE) {
                            val dx = point.x - obj.x
                            val dy = point.y - obj.y
                            obj.copy(
                                x = point.x,
                                y = point.y,
                                points = obj.points.map { BoardNormPoint(it.x + dx, it.y + dy) }
                            )
                        } else obj.copy(x = point.x, y = point.y)
                    },
                    dirty = true
                )
            }
            return
        }
        val current = state.currentStroke ?: return
        val updated = when (current.tool) {
            DrawTool.PEN, DrawTool.ERASER -> current.copy(points = current.points + point)
            DrawTool.ARROW, DrawTool.OVAL -> {
                val start = current.points.firstOrNull() ?: point
                current.copy(points = listOf(start, point))
            }
        }
        _uiState.update { it.copy(currentStroke = updated) }
    }

    fun onRelease() {
        stampConsumedThisGesture = false
        val current = _uiState.value.currentStroke ?: return
        if (current.points.size < 2 &&
            (current.tool == DrawTool.ARROW || current.tool == DrawTool.OVAL)
        ) {
            _uiState.update { it.copy(currentStroke = null) }
            return
        }
        if (current.points.isEmpty()) {
            _uiState.update { it.copy(currentStroke = null) }
            return
        }
        val type = when (current.tool) {
            DrawTool.ARROW -> BoardObjectType.ARROW
            DrawTool.OVAL -> BoardObjectType.OVAL
            DrawTool.ERASER -> BoardObjectType.ERASER
            DrawTool.PEN -> BoardObjectType.PATH
        }
        val first = current.points.first()
        addObject(
            BoardObject(
                objectId = UUID.randomUUID().toString(),
                type = type,
                x = first.x,
                y = first.y,
                colorArgb = current.colorArgb,
                width = current.width,
                points = current.points.map { BoardNormPoint(it.x, it.y) }
            )
        )
        _uiState.update { it.copy(currentStroke = null) }
    }

    fun undo() {
        _uiState.update { state ->
            when {
                state.currentStroke != null -> state.copy(currentStroke = null)
                state.objects.isNotEmpty() -> state.copy(objects = state.objects.dropLast(1), dirty = true, selectedObjectId = null)
                else -> state
            }
        }
    }

    fun clearAll() {
        _uiState.update { it.copy(objects = emptyList(), currentStroke = null, selectedObjectId = null, dirty = true) }
    }

    fun deleteSelected() {
        val id = _uiState.value.selectedObjectId ?: return
        _uiState.update { it.copy(objects = it.objects.filterNot { obj -> obj.objectId == id }, selectedObjectId = null, dirty = true) }
    }

    fun updateSelectedNumber(number: String) {
        val id = _uiState.value.selectedObjectId ?: return
        _uiState.update { current ->
            current.copy(
                objects = current.objects.map { if (it.objectId == id) it.copy(number = number.trim()) else it },
                dirty = true
            )
        }
    }

    fun updateSelectedText(text: String) {
        val id = _uiState.value.selectedObjectId ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            deleteSelected()
            return
        }
        _uiState.update { current ->
            current.copy(
                objects = current.objects.map { if (it.objectId == id) it.copy(text = trimmed) else it },
                dirty = true
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            persist()
            _uiState.update { it.copy(dirty = false, saved = true) }
        }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    suspend fun persist() {
        val state = _uiState.value
        val scene = BoardScene(
            version = BoardScene.CURRENT_VERSION,
            background = when (state.background) {
                BoardBackground.IMAGE -> BoardBackgroundType.IMAGE
                BoardBackground.VIDEO -> BoardBackgroundType.VIDEO
                BoardBackground.FIELD -> BoardBackgroundType.FIELD
            },
            mediaAttachmentSyncId = if (state.background == BoardBackground.FIELD) null else mediaAttachmentSyncId,
            objects = state.objects
        )
        val existing = boards.getOnce(state.boardId) ?: return
        boards.saveScene(existing.id, scene)
        if (taskId > 0 && existing.syncId.isNotBlank()) {
            tasks.setBoardSyncId(taskId, existing.syncId)
        }
    }

    private fun placeStamp(state: PizarraUiState, point: BoardPoint) {
        val type = BoardEditorLogic.stampObjectType(state.tool) ?: return
        val text = state.pendingText?.trim().orEmpty()
        val obj = BoardObject(
            objectId = UUID.randomUUID().toString(),
            type = type,
            x = point.x,
            y = point.y,
            number = if (type == BoardObjectType.BLUE_PLAYER || type == BoardObjectType.RED_PLAYER) "" else null,
            text = if (type == BoardObjectType.TEXT) text else null,
            colorArgb = if (type == BoardObjectType.TEXT) state.colorArgb else null
        )
        stampConsumedThisGesture = true
        _uiState.update {
            it.copy(
                objects = it.objects + obj,
                dirty = true,
                selectedObjectId = obj.objectId,
                tool = BoardEditorLogic.toolAfterStamp(),
                pendingText = if (type == BoardObjectType.TEXT) null else it.pendingText,
                currentStroke = null
            )
        }
    }

    private fun addObject(obj: BoardObject) {
        _uiState.update {
            it.copy(objects = it.objects + obj, dirty = true, selectedObjectId = obj.objectId)
        }
    }

    private suspend fun currentBoard(): Board? {
        val id = _uiState.value.boardId
        return if (id > 0) boards.getOnce(id) else null
    }

    companion object {
        fun factory(
            context: Context,
            boardId: Int,
            teamId: Int,
            taskId: Int
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    val files = AndroidAttachmentStore(app)
                    return PizarraViewModel(
                        boardId,
                        teamId,
                        taskId,
                        BoardRepository(
                            db.boardDao(),
                            db.taskDao(),
                            db.attachmentDao(),
                            DiskFileStore(File(app.filesDir, AndroidAttachmentStore.DIR))
                        ),
                        TaskRepositoryImpl(db.taskDao()),
                        AttachmentRepositoryImpl(db.attachmentDao()),
                        files
                    ) as T
                }
            }
    }
}
