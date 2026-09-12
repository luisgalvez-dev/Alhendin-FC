package com.luis.alhendinfc.ui.matches

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.EventLabels
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepositoryImpl
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MatchViewModel(
    private val matchRepository: MatchRepositoryImpl,
    private val playerRepository: PlayerRepositoryImpl,
    private val calendarRepository: SeasonCalendarRepository,
    private val customStatTypeRepository: CustomStatTypeRepositoryImpl,
    private val attachmentRepository: AttachmentRepository,
    private val fileStore: AndroidAttachmentStore,
    private val teamId: Int
) : ViewModel() {

    val matches: StateFlow<List<Match>> = matchRepository.getMatchesByTeam(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamPlayers: StateFlow<List<Player>> = playerRepository.getPlayersByTeam(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fixtures: StateFlow<List<FixtureRow>> = calendarRepository.getFixtureRows(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val customStatLabels: StateFlow<Map<String, String>> =
        customStatTypeRepository.getByTeam(teamId)
            .map { list -> list.associate { it.code to it.label } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _activeMatchId = MutableStateFlow<Int?>(null)

    val currentMatch: StateFlow<Match?> = _activeMatchId
        .flatMapLatest { id -> if (id != null) matchRepository.getMatchById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val matchPlayers: StateFlow<List<MatchPlayer>> = _activeMatchId
        .flatMapLatest { id -> if (id != null) matchRepository.getMatchPlayers(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matchEvents: StateFlow<List<MatchEvent>> = _activeMatchId
        .flatMapLatest { id -> if (id != null) matchRepository.getMatchEvents(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<Attachment>> = currentMatch
        .flatMapLatest { match ->
            val syncId = match?.syncId.orEmpty()
            if (syncId.isBlank()) flowOf(emptyList())
            else attachmentRepository.getActiveByParent(AttachmentParentType.MATCH, syncId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun eventLabel(event: MatchEvent): String =
        EventLabels.resolve(event, customStatLabels.value)

    fun openMatch(matchId: Int) {
        _activeMatchId.value = matchId
    }

    fun closeMatch() {
        _activeMatchId.value = null
    }

    fun createNewMatch(onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val newId = matchRepository.createMatch(
                Match(teamId = teamId, rival = "", status = MatchStatus.OPEN)
            )
            _activeMatchId.value = newId
            onCreated(newId)
        }
    }

    fun saveMatch(match: Match) {
        viewModelScope.launch {
            val stored = if (match.opponentClubId != null) {
                match.copy(rivalShieldUri = match.rivalShieldUri)
            } else {
                match.copy(rivalShieldUri = SharedMedia.persistableLegacyUri(match.rivalShieldUri))
            }
            matchRepository.updateMatch(stored)
        }
    }

    fun deleteCurrentMatch() {
        val match = currentMatch.value ?: return
        viewModelScope.launch {
            matchRepository.deleteMatch(match)
            _activeMatchId.value = null
        }
    }

    fun addReport(uri: Uri, name: String, mime: String) {
        viewModelScope.launch {
            val syncId = currentMatch.value?.syncId.orEmpty()
            if (syncId.isBlank()) return@launch
            val attachmentSync = UUID.randomUUID().toString()
            try {
                val mimeType = mime.ifBlank { "application/pdf" }
                val path = fileStore.importUriValidated(uri, name.ifBlank { "informe" }, attachmentSync, mimeType)
                attachmentRepository.add(
                    parentType = AttachmentParentType.MATCH,
                    parentSyncId = syncId,
                    mimeType = mimeType,
                    name = name.ifBlank { "informe" },
                    localPath = path,
                    syncId = attachmentSync
                )
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    fun deleteReport(attachment: Attachment) {
        viewModelScope.launch { attachmentRepository.delete(attachment) }
    }

    fun setPlayerCallup(playerId: Int, status: CallupStatus) {
        val matchId = _activeMatchId.value ?: return
        viewModelScope.launch {
            matchRepository.setPlayerCallup(matchId, playerId, status)
        }
    }

    fun fixtureForMatchday(matchday: Int): FixtureRow? =
        fixtures.value.firstOrNull { it.fixture.matchday == matchday }

    companion object {
        fun factory(context: Context, teamId: Int) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AlhendinDatabase.getInstance(context.applicationContext)
                @Suppress("UNCHECKED_CAST")
                return MatchViewModel(
                    MatchRepositoryImpl(db.matchDao(), db.matchEventDao(), db.attachmentDao(), db.opponentClubDao()),
                    PlayerRepositoryImpl(db.playerDao(), db.matchDao()),
                    SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                    CustomStatTypeRepositoryImpl(db.customStatTypeDao()),
                    AttachmentRepositoryImpl(db.attachmentDao()),
                    AndroidAttachmentStore(context.applicationContext),
                    teamId
                ) as T
            }
        }
    }
}
