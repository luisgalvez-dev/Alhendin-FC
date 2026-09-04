package com.luis.alhendinfc.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val calendarRepository: SeasonCalendarRepository,
    private val matchRepository: MatchRepositoryImpl,
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

    init {
        viewModelScope.launch {
            calendarRepository.ensureSampleCalendar(teamId)
        }
    }

    fun addClub(name: String, shortName: String, stadium: String, shieldUri: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val order = (clubs.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            calendarRepository.addClub(
                OpponentClub(
                    teamId = teamId,
                    name = name.trim(),
                    shortName = shortName.trim(),
                    stadium = stadium.trim(),
                    shieldUri = shieldUri?.trim()?.ifBlank { null },
                    sortOrder = order
                )
            )
        }
    }

    fun updateClub(club: OpponentClub) {
        viewModelScope.launch { calendarRepository.updateClub(club) }
    }

    fun deleteClub(club: OpponentClub) {
        viewModelScope.launch { calendarRepository.deleteClub(club) }
    }

    /**
     * Crea un partido OPEN con datos de la jornada, o reutiliza uno OPEN/LIVE existente
     * de esa misma jornada. Devuelve el id para abrir el setup.
     */
    fun openOrPrepareMatch(row: FixtureRow, onReady: (matchId: Int) -> Unit) {
        viewModelScope.launch {
            val matchday = row.fixture.matchday
            val existing = matches.value.firstOrNull {
                it.matchday == matchday &&
                    (it.status == MatchStatus.OPEN || it.status == MatchStatus.LIVE)
            }
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
                    rivalShieldUri = club?.shieldUri,
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
                    val db = AlhendinDatabase.getInstance(context.applicationContext)
                    return CalendarViewModel(
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        MatchRepositoryImpl(db.matchDao(), db.matchEventDao()),
                        teamId
                    ) as T
                }
            }
    }
}
