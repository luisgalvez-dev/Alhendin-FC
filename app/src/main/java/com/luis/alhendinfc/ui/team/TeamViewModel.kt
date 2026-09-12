package com.luis.alhendinfc.ui.team

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
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.TeamRepository
import com.luis.alhendinfc.domain.repository.TeamRepositoryImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeamViewModel(
    private val repository: TeamRepository,
    private val attachments: AttachmentRepository,
    private val media: SharedMediaWriter
) : ViewModel() {

    val teams: StateFlow<List<Team>> = repository.getAllTeams()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val selectedTeam: StateFlow<Team?> = repository.getSelectedTeam()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val teamShields: StateFlow<Map<String, Attachment>> =
        attachments.getActiveByType(AttachmentParentType.TEAM_SHIELD)
            .map { SharedMedia.byParentSyncId(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun shieldPath(team: Team?): String? {
        if (team == null) return null
        return SharedMedia.displayPath(teamShields.value[team.syncId], team.shieldUri)
    }

    fun addTeam(team: Team, shield: Uri? = null) {
        viewModelScope.launch {
            val id = repository.addTeam(team.copy(shieldUri = team.shieldUri.takeUnless { isPicker(it) }))
            val created = repository.getOnce(id) ?: return@launch
            if (shield != null) runCatching { media.setTeamShield(created.syncId, shield) }
        }
    }

    fun updateTeam(team: Team, shield: Uri? = null, clearShield: Boolean = false) {
        viewModelScope.launch {
            if (clearShield) {
                repository.updateTeam(team.copy(shieldUri = null))
                media.clear(AttachmentParentType.TEAM_SHIELD, team.syncId)
            } else {
                repository.updateTeam(team.copy(shieldUri = team.shieldUri.takeUnless { isPicker(it) }))
                if (shield != null && team.syncId.isNotBlank()) {
                    runCatching { media.setTeamShield(team.syncId, shield) }
                }
            }
        }
    }

    fun deleteTeam(team: Team) {
        viewModelScope.launch {
            if (team.syncId.isNotBlank()) {
                attachments.deleteByParent(AttachmentParentType.TEAM_SHIELD, team.syncId)
            }
            repository.deleteTeam(team)
        }
    }

    fun selectTeam(teamId: Int) {
        viewModelScope.launch { repository.selectTeam(teamId) }
    }

    private fun isPicker(uri: String?): Boolean = SharedMedia.isPickerUri(uri)

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    val attachments = AttachmentRepositoryImpl(db.attachmentDao())
                    val files = AndroidAttachmentStore(app)
                    return TeamViewModel(
                        TeamRepositoryImpl(db.teamDao()),
                        attachments,
                        SharedMediaWriter(attachments, files)
                    ) as T
                }
            }
    }
}
