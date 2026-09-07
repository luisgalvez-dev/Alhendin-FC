package com.luis.alhendinfc.ui.statistics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.CustomStatAppliesTo
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.PlayerCustomStatCount
import com.luis.alhendinfc.domain.model.PlayerSeasonStats
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepositoryImpl
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
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
        customStatTypeRepository.getByTeam(teamId)
    ) { players, events, callups, customTypes ->
        // events ya vienen filtrados a partidos FINISHED desde Room
        val playerCustomTypes = customTypes
            .filter { it.appliesTo != CustomStatAppliesTo.RIVAL }
            .sortedBy { it.sortOrder }

        players.map { player ->
            val playerEvents = events.filter { it.playerId == player.id }
            val yellow = playerEvents
                .filter { it.type == StatisticType.YELLOW_CARD }
                .sumOf { it.value }
            val redDirect = playerEvents
                .filter { it.type == StatisticType.RED_CARD }
                .sumOf { it.value }
            val redFromYellow = if (yellow >= 2) 1 else 0

            val customStats = playerCustomTypes
                .filter { type ->
                    type.appliesTo.matches(player.position) ||
                        playerEvents.any { it.typeCode == type.code }
                }
                .filter { type -> type.isActive || playerEvents.any { it.typeCode == type.code } }
                .map { type ->
                    PlayerCustomStatCount(
                        code = type.code,
                        label = type.label,
                        shortLabel = type.shortLabel.ifBlank { type.label },
                        value = playerEvents
                            .filter { it.typeCode == type.code }
                            .sumOf { it.value }
                    )
                }

            PlayerSeasonStats(
                player = player,
                matchesPlayed = callups
                    .filter { it.playerId == player.id }
                    .map { it.matchId }
                    .distinct()
                    .size,
                goals = playerEvents.filter { it.type == StatisticType.GOAL }.sumOf { it.value },
                assists = playerEvents.filter { it.type == StatisticType.ASSIST }.sumOf { it.value },
                yellowCards = yellow,
                redCards = maxOf(redDirect, redFromYellow),
                customStats = customStats
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
