package com.luis.alhendinfc.ui.team

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepository
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepository
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import com.luis.alhendinfc.domain.repository.TeamRepository
import com.luis.alhendinfc.domain.repository.TeamRepositoryImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeamViewModel(
    private val repository: TeamRepository,
    private val playerRepository: PlayerRepository,
    private val customStatRepository: CustomStatTypeRepository,
    private val calendarRepository: SeasonCalendarRepository
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

    fun addTeam(team: Team) {
        viewModelScope.launch {
            val teamId = repository.addTeam(team)
            playerRepository.ensureSampleSquad(teamId)
            customStatRepository.ensureSampleCustomStats(teamId)
            calendarRepository.ensureSampleCalendar(teamId)
        }
    }

    fun updateTeam(team: Team) {
        viewModelScope.launch { repository.updateTeam(team) }
    }

    fun deleteTeam(team: Team) {
        viewModelScope.launch { repository.deleteTeam(team) }
    }

    fun selectTeam(teamId: Int) {
        viewModelScope.launch { repository.selectTeam(teamId) }
    }

    fun ensureSampleSquadForSelectedTeam() {
        viewModelScope.launch {
            val teamId = selectedTeam.value?.id ?: return@launch
            playerRepository.ensureSampleSquad(teamId)
            customStatRepository.ensureSampleCustomStats(teamId)
            calendarRepository.ensureSampleCalendar(teamId)
        }
    }

    init {
        viewModelScope.launch {
            selectedTeam.collect { team ->
                val id = team?.id ?: return@collect
                playerRepository.ensureSampleSquad(id)
                customStatRepository.ensureSampleCustomStats(id)
                calendarRepository.ensureSampleCalendar(id)
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AlhendinDatabase.getInstance(context)
                    return TeamViewModel(
                        TeamRepositoryImpl(db.teamDao()),
                        PlayerRepositoryImpl(db.playerDao(), db.matchDao()),
                        CustomStatTypeRepositoryImpl(db.customStatTypeDao()),
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao())
                    ) as T
                }
            }
    }
}
