package com.luis.alhendinfc.ui.statistics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.PlayerSeasonStats
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepositoryImpl
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import com.luis.alhendinfc.domain.stats.SeasonStatsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatisticsViewModel(
    matchRepository: MatchRepositoryImpl,
    playerRepository: PlayerRepositoryImpl,
    customStatTypeRepository: CustomStatTypeRepositoryImpl,
    teamId: Int
) : ViewModel() {

    val playerStats: StateFlow<List<PlayerSeasonStats>> = combine(
        playerRepository.getPlayersByTeam(teamId),
        matchRepository.getTeamEvents(teamId),
        matchRepository.getFinishedCallupsByTeam(teamId),
        customStatTypeRepository.getByTeam(teamId),
        matchRepository.getMatchesByTeam(teamId)
    ) { players, events, callups, customTypes, matches ->
        val teamMatchIds = matches
            .filter { it.teamId == teamId && it.status == MatchStatus.FINISHED }
            .map { it.id }
            .toSet()

        players.map { player ->
            SeasonStatsCalculator.forPlayer(
                player = player,
                events = events,
                callups = callups,
                customTypes = customTypes,
                teamMatchIds = teamMatchIds,
                finishedMatches = matches.filter {
                    it.teamId == teamId && it.status == MatchStatus.FINISHED
                }
            )
        }.sortedWith(
            compareByDescending<PlayerSeasonStats> { it.goals }
                .thenByDescending { it.assists }
                .thenByDescending { it.matchesPlayed }
                .thenBy { it.player.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finishedMatches: StateFlow<List<Match>> = matchRepository.getMatchesByTeam(teamId)
        .map { matches -> matches.filter { it.status == MatchStatus.FINISHED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun factory(context: Context, teamId: Int) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AlhendinDatabase.getInstance(context.applicationContext)
                @Suppress("UNCHECKED_CAST")
                return StatisticsViewModel(
                    MatchRepositoryImpl(db.matchDao(), db.matchEventDao()),
                    PlayerRepositoryImpl(db.playerDao(), db.matchDao()),
                    CustomStatTypeRepositoryImpl(db.customStatTypeDao()),
                    teamId
                ) as T
            }
        }
    }
}
