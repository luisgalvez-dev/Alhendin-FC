package com.luis.alhendinfc.ui.tasks

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.files.DiskFileStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.domain.model.BoardScene
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.BoardRepository
import com.luis.alhendinfc.domain.repository.TaskRepository
import com.luis.alhendinfc.domain.repository.TaskRepositoryImpl
import java.io.File
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val attachments: AttachmentRepository,
    private val boards: BoardRepository,
    private val fileStore: AndroidAttachmentStore,
    private val teamId: Int
) : ViewModel() {

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query

    val tasks: StateFlow<List<Task>> = query
        .flatMapLatest { needle -> repository.searchByName(teamId, needle) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val taskImages: StateFlow<Map<String, Attachment>> =
        attachments.getActiveByType(AttachmentParentType.TASK)
            .map { list ->
                list.filter { it.isImage }.associateBy { it.parentSyncId }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val boardsState: StateFlow<List<Board>> =
        boards.getByTeam(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun add(task: Task, imageUri: Uri? = null) {
        viewModelScope.launch {
            val id = repository.add(task.copy(teamId = teamId))
            val created = repository.getOnce(id) ?: return@launch
            if (imageUri != null) applyImage(created.syncId, imageUri)
        }
    }

    fun update(task: Task, imageUri: Uri? = null, removeImage: Boolean = false) {
        viewModelScope.launch {
            repository.update(task.copy(teamId = teamId))
            val current = repository.getOnce(task.id) ?: return@launch
            when {
                removeImage -> attachments.clearTaskImages(current.syncId)
                imageUri != null -> applyImage(current.syncId, imageUri)
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            if (task.syncId.isNotBlank()) attachments.deleteByParent(AttachmentParentType.TASK, task.syncId)
            repository.delete(task)
        }
    }

    fun createBoardForTask(task: Task, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val id = boards.add(
                Board(
                    teamId = teamId,
                    name = task.name.ifBlank { "Pizarra" },
                    sceneJson = BoardScene.empty().toJson()
                )
            )
            val created = boards.getOnce(id) ?: return@launch
            repository.setBoardSyncId(task.id, created.syncId)
            onCreated(id)
        }
    }

    fun assignBoard(task: Task, board: Board) {
        viewModelScope.launch { repository.setBoardSyncId(task.id, board.syncId) }
    }

    fun clearBoard(task: Task) {
        viewModelScope.launch { repository.setBoardSyncId(task.id, null) }
    }

    private suspend fun applyImage(taskSyncId: String, uri: Uri) {
        val attachmentSync = UUID.randomUUID().toString()
        val name = fileStore.queryDisplayName(uri) ?: "imagen"
        val mime = fileStore.queryMimeType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        try {
            val path = fileStore.importUriValidated(uri, name, attachmentSync, mime)
            attachments.setTaskImage(taskSyncId, mime, name, path)
        } catch (_: IllegalArgumentException) {
        }
    }

    companion object {
        fun factory(context: Context, teamId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    return TaskViewModel(
                        TaskRepositoryImpl(db.taskDao()),
                        AttachmentRepositoryImpl(db.attachmentDao()),
                        BoardRepository(
                            db.boardDao(),
                            db.taskDao(),
                            db.attachmentDao(),
                            DiskFileStore(File(app.filesDir, AndroidAttachmentStore.DIR))
                        ),
                        AndroidAttachmentStore(app),
                        teamId
                    ) as T
                }
            }
    }
}
