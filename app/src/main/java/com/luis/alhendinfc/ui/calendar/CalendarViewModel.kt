package com.luis.alhendinfc.ui.calendar

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
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchLifecycle
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.SeasonFixture
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val calendarRepository: SeasonCalendarRepository,
    private val matchRepository: MatchRepositoryImpl,
    private val attachments: AttachmentRepository,
    private val media: SharedMediaWriter,
    private val teamId: Int
) : ViewModel() {

    val fixtures: StateFlow<List<FixtureRow>> =
        calendarRepository.getFixtureRows(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clubs: StateFlow<List<OpponentClub>> =
        calendarRepository.getClubs(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val matches: StateFlow<List<Match>> =
        matchRepository.getMatchesByTeam(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val opponentShields: StateFlow<Map<String, Attachment>> =
        attachments.getActiveByType(AttachmentParentType.OPPONENT_SHIELD)
            .map { SharedMedia.byParentSyncId(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun addClub(
        name: String,
        shortName: String,
        stadium: String,
        shieldUri: String?,
        kitColors: String = "",
        pickedShield: Uri? = shieldUri?.takeIf { SharedMedia.isPickerUri(it) }?.let(Uri::parse)
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val order = (clubs.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val id = calendarRepository.addClub(
                OpponentClub(
                    teamId = teamId,
                    name = name.trim(),
                    shortName = shortName.trim(),
                    stadium = stadium.trim(),
                    shieldUri = SharedMedia.persistableLegacyUri(shieldUri),
                    kitColors = kitColors.trim(),
                    sortOrder = order
                )
            )
            val created = calendarRepository.getClubOnce(id)
            if (pickedShield != null && created != null && created.syncId.isNotBlank()) {
                runCatching { media.setOpponentShield(created.syncId, pickedShield) }
            }
        }
    }

    fun updateClub(club: OpponentClub, shield: Uri? = null, clearShield: Boolean = false) {
        viewModelScope.launch {
            calendarRepository.updateClub(
                club.copy(
                    shieldUri = if (clearShield) null else SharedMedia.persistableLegacyUri(club.shieldUri)
                )
            )
            val syncId = club.syncId.ifBlank { calendarRepository.getClubOnce(club.id)?.syncId }.orEmpty()
            media.applyPicked(AttachmentParentType.OPPONENT_SHIELD, syncId, shield, clearShield)
        }
    }

    fun deleteClub(club: OpponentClub) {
        viewModelScope.launch {
            if (club.syncId.isNotBlank()) {
                attachments.deleteByParent(AttachmentParentType.OPPONENT_SHIELD, club.syncId)
                attachments.deleteByParent(AttachmentParentType.OPPONENT, club.syncId)
            }
            calendarRepository.deleteClub(club)
        }
    }

    fun saveFixture(
        existingId: Int,
        matchday: Int,
        opponentClubId: Int,
        isHome: Boolean,
        date: String,
        time: String,
        stadiumOverride: String
    ) {
        if (opponentClubId <= 0 || matchday <= 0) return
        viewModelScope.launch {
            calendarRepository.upsertFixture(
                SeasonFixture(
                    id = existingId,
                    teamId = teamId,
                    matchday = matchday,
                    opponentClubId = opponentClubId,
                    isHome = isHome,
                    date = date.trim(),
                    time = time.trim(),
                    stadiumOverride = stadiumOverride.trim()
                )
            )
        }
    }

    fun addFixture(
        matchday: Int,
        opponentClubId: Int,
        isHome: Boolean,
        date: String,
        time: String,
        stadiumOverride: String
    ) {
        if (opponentClubId <= 0) return
        val day = if (matchday > 0) {
            matchday
        } else {
            (fixtures.value.maxOfOrNull { it.fixture.matchday } ?: 0) + 1
        }
        viewModelScope.launch {
            calendarRepository.upsertFixture(
                SeasonFixture(
                    teamId = teamId,
                    matchday = day,
                    opponentClubId = opponentClubId,
                    isHome = isHome,
                    date = date.trim(),
                    time = time.trim(),
                    stadiumOverride = stadiumOverride.trim()
                )
            )
        }
    }

    fun deleteFixture(row: FixtureRow) {
        viewModelScope.launch { calendarRepository.deleteFixture(row.fixture) }
    }

    /**
     * Abre el partido ya asociado a la jornada, o crea uno OPEN si aún no existe.
     * Reutiliza también un partido FINISHED: no se crea un segundo Match.
     * El escudo del rival vive en OpponentClub; no se copia a Match.rivalShieldUri.
     */
    fun openOrPrepareMatch(row: FixtureRow, onReady: (matchId: Int) -> Unit) {
        viewModelScope.launch {
            val matchday = row.fixture.matchday
            val existing = MatchLifecycle.resolveMatchForFixture(
                matchRepository.getMatchesByTeamOnce(teamId),
                matchday
            )
            if (existing != null) {
                onReady(existing.id)
                return@launch
            }
            val club = row.club
            val newId = matchRepository.createMatch(
                Match(
                    teamId = teamId,
                    rival = club?.name.orEmpty(),
                    stadium = row.stadium,
                    date = row.fixture.date,
                    time = row.fixture.time,
                    matchday = matchday,
                    isHome = row.fixture.isHome,
                    opponentClubId = club?.id,
                    rivalShieldUri = null,
                    status = MatchStatus.OPEN
                )
            )
            onReady(newId)
        }
    }

    companion object {
        fun factory(context: Context, teamId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    val attachments = AttachmentRepositoryImpl(db.attachmentDao())
                    return CalendarViewModel(
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        MatchRepositoryImpl(db.matchDao(), db.matchEventDao()),
                        attachments,
                        SharedMediaWriter(attachments, AndroidAttachmentStore(app)),
                        teamId
                    ) as T
                }
            }
    }
}
