package com.luis.alhendinfc.ui.pizarra

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.files.DiskFileStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.domain.model.BoardScene
import com.luis.alhendinfc.domain.repository.BoardRepository
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoardListViewModel(
    private val teamId: Int,
    private val boards: BoardRepository
) : ViewModel() {

    val boardsState: StateFlow<List<Board>> =
        boards.getByTeam(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val id = boards.add(
                Board(
                    teamId = teamId,
                    name = name.trim().ifBlank { "Nueva pizarra" },
                    sceneJson = BoardScene.empty().toJson()
                )
            )
            onCreated(id)
        }
    }

    fun rename(board: Board, name: String) {
        viewModelScope.launch { boards.rename(board.id, name) }
    }

    fun duplicate(board: Board, onCreated: (Int) -> Unit) {
        viewModelScope.launch { onCreated(boards.duplicate(board)) }
    }

    suspend fun tasksUsing(board: Board): Int = boards.countTasksUsing(board.syncId)

    fun delete(board: Board) {
        viewModelScope.launch { boards.delete(board) }
    }

    companion object {
        fun factory(context: Context, teamId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    return BoardListViewModel(
                        teamId,
                        BoardRepository(
                            db.boardDao(),
                            db.taskDao(),
                            db.attachmentDao(),
                            DiskFileStore(File(app.filesDir, AndroidAttachmentStore.DIR))
                        )
                    ) as T
                }
            }
    }
}
