package com.luis.alhendinfc.ui.training

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.CalendarDate
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.Training
import com.luis.alhendinfc.domain.model.TrainingTask
import com.luis.alhendinfc.domain.model.TrainingTaskItem
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import com.luis.alhendinfc.domain.repository.TaskRepository
import com.luis.alhendinfc.domain.repository.TaskRepositoryImpl
import com.luis.alhendinfc.domain.repository.TrainingRepository
import com.luis.alhendinfc.domain.repository.TrainingRepositoryImpl
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingAttachment(
    val uri: Uri,
    val name: String,
    val mime: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingEditViewModel(
    private val trainingRepository: TrainingRepository,
    private val taskRepository: TaskRepository,
    private val attachmentRepository: AttachmentRepository,
    private val calendarRepository: SeasonCalendarRepository,
    private val fileStore: AndroidAttachmentStore,
    private val teamId: Int,
    private val trainingId: Int,
    epochDayArg: Long
) : ViewModel() {

    val isNew: Boolean = trainingId <= 0
    val epochDay: Long = epochDayArg
    val dateLabel: String = if (isNew) CalendarDate.format(epochDayArg) else ""

    private val _opponentClubId = MutableStateFlow<Int?>(null)
    val opponentClubId: StateFlow<Int?> = _opponentClubId

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _pendingTaskIds = MutableStateFlow<List<Int>>(emptyList())
    private val _pendingFiles = MutableStateFlow<List<PendingAttachment>>(emptyList())
    val pendingFiles: StateFlow<List<PendingAttachment>> = _pendingFiles

    val clubs: StateFlow<List<OpponentClub>> =
        calendarRepository.getClubs(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val libraryTasks: StateFlow<List<Task>> =
        taskRepository.getByTeam(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val training: StateFlow<Training?> =
        trainingRepository.getById(trainingId.coerceAtLeast(0))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sessionTasks: StateFlow<List<TrainingTaskItem>> =
        if (isNew) {
            combine(_pendingTaskIds, libraryTasks) { ids, library ->
                val byId = library.associateBy { it.id }
                ids.mapNotNull { id ->
                    val task = byId[id] ?: return@mapNotNull null
                    TrainingTaskItem(
                        relation = TrainingTask(
                            trainingId = 0,
                            taskId = id,
                            sortOrder = ids.indexOf(id)
                        ),
                        task = task
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        } else {
            trainingRepository.getTasks(trainingId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    val attachments: StateFlow<List<Attachment>> =
        if (isNew) {
            flowOf(emptyList())
        } else {
            training.flatMapLatest { current ->
                val syncId = current?.syncId.orEmpty()
                if (syncId.isBlank()) flowOf(emptyList())
                else attachmentRepository.getActiveByParent(AttachmentParentType.TRAINING, syncId)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (!isNew) {
            viewModelScope.launch {
                val current = trainingRepository.getById(trainingId).filterNotNull().first()
                _opponentClubId.value = current.opponentClubId
                _notes.value = current.notes
            }
        }
    }

    fun setOpponentClubId(id: Int?) {
        _opponentClubId.value = id
    }

    fun setNotes(value: String) {
        _notes.value = value
    }

    fun addTask(taskId: Int) {
        if (sessionTasks.value.any { it.task.id == taskId }) return
        if (isNew) {
            _pendingTaskIds.value = _pendingTaskIds.value + taskId
        } else {
            viewModelScope.launch { trainingRepository.addTask(trainingId, taskId) }
        }
    }

    fun removeTask(taskId: Int) {
        if (isNew) {
            _pendingTaskIds.value = _pendingTaskIds.value.filterNot { it == taskId }
        } else {
            viewModelScope.launch { trainingRepository.removeTask(trainingId, taskId) }
        }
    }

    fun moveTask(taskId: Int, up: Boolean) {
        if (isNew) {
            val ids = _pendingTaskIds.value.toMutableList()
            val index = ids.indexOf(taskId)
            val swap = if (up) index - 1 else index + 1
            if (index < 0 || swap !in ids.indices) return
            val tmp = ids[index]
            ids[index] = ids[swap]
            ids[swap] = tmp
            _pendingTaskIds.value = ids
        } else {
            viewModelScope.launch { trainingRepository.moveTask(trainingId, taskId, up) }
        }
    }

    fun addPendingFile(uri: Uri, name: String, mime: String) {
        if (isNew) {
            _pendingFiles.value = _pendingFiles.value + PendingAttachment(uri, name, mime)
            return
        }
        viewModelScope.launch {
            val current = training.value ?: return@launch
            importAndAttach(current.syncId, uri, name, mime)
        }
    }

    fun removePendingFile(uri: Uri) {
        _pendingFiles.value = _pendingFiles.value.filterNot { it.uri == uri }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch { attachmentRepository.delete(attachment) }
    }

    fun save(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val savedId = if (isNew) {
                    if (!trainingRepository.canCreate(teamId, epochDay)) {
                        _error.value =
                            "No se puede crear un entrenamiento: ese día ya tiene partido, jornada o entrenamiento"
                        return@launch
                    }
                    val id = trainingRepository.add(
                        Training(
                            teamId = teamId,
                            date = dateLabel,
                            dateEpochDay = epochDay,
                            opponentClubId = _opponentClubId.value,
                            notes = _notes.value
                        )
                    )
                    _pendingTaskIds.value.forEach { trainingRepository.addTask(id, it) }
                    val created = trainingRepository.getById(id).filterNotNull().first()
                    _pendingFiles.value.forEach { pending ->
                        importAndAttach(created.syncId, pending.uri, pending.name, pending.mime)
                    }
                    id
                } else {
                    val current = training.value ?: trainingRepository.getById(trainingId).filterNotNull().first()
                    trainingRepository.update(
                        current.copy(
                            opponentClubId = _opponentClubId.value,
                            notes = _notes.value
                        )
                    )
                    trainingId
                }
                onDone(savedId)
            } catch (t: Throwable) {
                _error.value = t.message ?: "No se pudo guardar el entrenamiento"
            } finally {
                _busy.value = false
            }
        }
    }

    fun deleteTraining(onDone: () -> Unit) {
        if (isNew) {
            onDone()
            return
        }
        viewModelScope.launch {
            val current = training.value ?: return@launch
            trainingRepository.delete(current)
            onDone()
        }
    }

    private suspend fun importAndAttach(parentSyncId: String, uri: Uri, name: String, mime: String) {
        if (parentSyncId.isBlank()) return
        val attachmentSync = UUID.randomUUID().toString()
        val path = fileStore.importUri(uri, name.ifBlank { "archivo" }, attachmentSync)
        attachmentRepository.add(
            parentType = AttachmentParentType.TRAINING,
            parentSyncId = parentSyncId,
            mimeType = mime.ifBlank { "application/octet-stream" },
            name = name.ifBlank { "archivo" },
            localPath = path,
            syncId = attachmentSync
        )
    }

    companion object {
        fun factory(
            context: Context,
            teamId: Int,
            trainingId: Int,
            epochDay: Long
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    return TrainingEditViewModel(
                        TrainingRepositoryImpl(
                            db.trainingDao(),
                            db.trainingTaskDao(),
                            db.taskDao(),
                            db.matchDao(),
                            db.seasonFixtureDao(),
                            db.attachmentDao()
                        ),
                        TaskRepositoryImpl(db.taskDao()),
                        AttachmentRepositoryImpl(db.attachmentDao()),
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        AndroidAttachmentStore(app),
                        teamId,
                        trainingId,
                        epochDay
                    ) as T
                }
            }
    }
}
