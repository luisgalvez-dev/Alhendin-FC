package com.luis.alhendinfc.ui.players

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.files.SharedMediaWriter
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepository
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: PlayerRepository,
    private val attachments: AttachmentRepository,
    private val media: SharedMediaWriter,
    private val teamId: Int
) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.getPlayersByTeam(teamId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val playerPhotos: StateFlow<Map<String, Attachment>> =
        attachments.getActiveByType(AttachmentParentType.PLAYER_PHOTO)
            .map { SharedMedia.byParentSyncId(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _selectedPlayer = MutableStateFlow<Player?>(null)
    val selectedPlayer: StateFlow<Player?> = _selectedPlayer

    fun photoPath(player: Player): String? =
        SharedMedia.displayPath(playerPhotos.value[player.syncId], player.photoUri)

    fun addPlayer(player: Player, photo: Uri? = null) {
        viewModelScope.launch {
            val id = repository.addPlayer(player.copy(photoUri = player.photoUri.takeUnless { isPicker(it) }))
            val created = repository.getOnce(id) ?: return@launch
            if (photo != null) runCatching { media.setPlayerPhoto(created.syncId, photo) }
        }
    }

    fun updatePlayer(player: Player, photo: Uri? = null, clearPhoto: Boolean = false) {
        viewModelScope.launch {
            if (clearPhoto) {
                repository.updatePlayer(player.copy(photoUri = null))
                media.clear(AttachmentParentType.PLAYER_PHOTO, player.syncId)
            } else {
                repository.updatePlayer(player.copy(photoUri = player.photoUri.takeUnless { isPicker(it) }))
                if (photo != null && player.syncId.isNotBlank()) {
                    runCatching { media.setPlayerPhoto(player.syncId, photo) }
                }
            }
        }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            if (player.syncId.isNotBlank()) {
                attachments.deleteByParent(AttachmentParentType.PLAYER_PHOTO, player.syncId)
            }
            repository.deletePlayer(player)
        }
    }

    fun selectPlayer(player: Player?) {
        _selectedPlayer.value = player
    }

    private fun isPicker(uri: String?): Boolean = SharedMedia.isPickerUri(uri)

    companion object {
        fun factory(context: Context, teamId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    val attachments = AttachmentRepositoryImpl(db.attachmentDao())
                    val files = AndroidAttachmentStore(app)
                    return PlayerViewModel(
                        PlayerRepositoryImpl(db.playerDao(), db.matchDao()),
                        attachments,
                        SharedMediaWriter(attachments, files),
                        teamId
                    ) as T
                }
            }
    }
}
