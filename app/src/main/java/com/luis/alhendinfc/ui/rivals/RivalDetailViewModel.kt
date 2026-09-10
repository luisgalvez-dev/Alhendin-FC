package com.luis.alhendinfc.ui.rivals

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.MatchReportRef
import com.luis.alhendinfc.domain.model.MatchReports
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.OpponentPlayer
import com.luis.alhendinfc.domain.model.RivalAnalysis
import com.luis.alhendinfc.domain.model.RivalLink
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.RivalRepository
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RivalDetailViewModel(
    private val calendarRepository: SeasonCalendarRepository,
    private val rivalRepository: RivalRepository,
    private val attachmentRepository: AttachmentRepository,
    private val matchRepository: MatchRepositoryImpl,
    private val fileStore: AndroidAttachmentStore,
    private val clubId: Int
) : ViewModel() {

    val club: StateFlow<OpponentClub?> =
        calendarRepository.getClub(clubId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val analysis: StateFlow<RivalAnalysis?> =
        rivalRepository.getAnalysis(clubId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val links: StateFlow<List<RivalLink>> =
        rivalRepository.getLinks(clubId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val playerQuery = MutableStateFlow("")
    val playerSearch: StateFlow<String> = playerQuery

    val players: StateFlow<List<OpponentPlayer>> =
        playerQuery.flatMapLatest { rivalRepository.searchPlayers(clubId, it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attachments: StateFlow<List<Attachment>> =
        club.flatMapLatest { current ->
            val syncId = current?.syncId.orEmpty()
            if (syncId.isBlank()) flowOf(emptyList())
            else attachmentRepository.getActiveByParent(AttachmentParentType.OPPONENT, syncId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val matchReports: StateFlow<List<MatchReportRef>> =
        club.flatMapLatest { current ->
            if (current == null) flowOf(emptyList())
            else combine(
                matchRepository.getMatchesByTeam(current.teamId),
                attachmentRepository.getActiveByType(AttachmentParentType.MATCH)
            ) { matches, attachments ->
                MatchReports.forOpponent(matches, attachments, current.id)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveClub(club: OpponentClub) {
        viewModelScope.launch { calendarRepository.updateClub(club) }
    }

    fun saveAnalysis(analysis: RivalAnalysis) {
        viewModelScope.launch {
            rivalRepository.saveAnalysis(analysis.copy(opponentClubId = clubId))
        }
    }

    fun addLink(type: String, label: String, url: String) {
        viewModelScope.launch {
            rivalRepository.addLink(
                RivalLink(opponentClubId = clubId, type = type, label = label.trim(), url = url.trim())
            )
        }
    }

    fun updateLink(link: RivalLink) {
        viewModelScope.launch { rivalRepository.updateLink(link) }
    }

    fun deleteLink(link: RivalLink) {
        viewModelScope.launch { rivalRepository.deleteLink(link) }
    }

    fun moveLink(linkId: Int, up: Boolean) {
        viewModelScope.launch { rivalRepository.moveLink(clubId, linkId, up) }
    }

    fun addFile(uri: Uri, name: String, mime: String) {
        viewModelScope.launch {
            val syncId = club.value?.syncId.orEmpty()
            if (syncId.isBlank()) return@launch
            val attachmentSync = UUID.randomUUID().toString()
            val path = fileStore.importUri(uri, name.ifBlank { "archivo" }, attachmentSync)
            attachmentRepository.add(
                parentType = AttachmentParentType.OPPONENT,
                parentSyncId = syncId,
                mimeType = mime.ifBlank { "application/octet-stream" },
                name = name.ifBlank { "archivo" },
                localPath = path,
                syncId = attachmentSync
            )
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch { attachmentRepository.delete(attachment) }
    }

    fun setPlayerQuery(value: String) {
        playerQuery.value = value
    }

    fun addPlayer(name: String) {
        viewModelScope.launch {
            rivalRepository.addPlayer(OpponentPlayer(opponentClubId = clubId, name = name.trim()))
        }
    }

    fun updatePlayer(player: OpponentPlayer) {
        viewModelScope.launch { rivalRepository.updatePlayer(player) }
    }

    fun deletePlayer(player: OpponentPlayer) {
        viewModelScope.launch { rivalRepository.deletePlayer(player) }
    }

    companion object {
        fun factory(context: Context, clubId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    return RivalDetailViewModel(
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        RivalRepository(db.rivalAnalysisDao(), db.rivalLinkDao(), db.opponentPlayerDao()),
                        AttachmentRepositoryImpl(db.attachmentDao()),
                        MatchRepositoryImpl(db.matchDao(), db.matchEventDao(), db.attachmentDao()),
                        AndroidAttachmentStore(app),
                        clubId
                    ) as T
                }
            }
    }
}
