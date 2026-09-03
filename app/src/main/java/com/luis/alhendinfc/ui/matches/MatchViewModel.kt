package com.luis.alhendinfc.ui.matches

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MatchViewModel(
    private val matchRepository: MatchRepositoryImpl,
    private val playerRepository: PlayerRepositoryImpl,
    private val teamId: Int
) : ViewModel() {

    val matches: StateFlow<List<Match>> = matchRepository.getMatchesByTeam(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamPlayers: StateFlow<List<Player>> = playerRepository.getPlayersByTeam(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeMatchId = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch {
            playerRepository.ensureSampleSquad(teamId)
        }
    }

    val currentMatch: StateFlow<Match?> = _activeMatchId
        .flatMapLatest { id -> if (id != null) matchRepository.getMatchById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val matchPlayers: StateFlow<List<MatchPlayer>> = _activeMatchId
        .flatMapLatest { id -> if (id != null) matchRepository.getMatchPlayers(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        viewModelScope.launch { matchRepository.updateMatch(match) }
    }

    fun deleteCurrentMatch() {
        val match = currentMatch.value ?: return
        viewModelScope.launch {
            matchRepository.deleteMatch(match)
            _activeMatchId.value = null
        }
    }

    fun setPlayerCallup(playerId: Int, status: CallupStatus) {
        val matchId = _activeMatchId.value ?: return
        viewModelScope.launch {
            matchRepository.setPlayerCallup(matchId, playerId, status)
        }
    }

    companion object {
        fun factory(context: Context, teamId: Int) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AlhendinDatabase.getInstance(context.applicationContext)
                @Suppress("UNCHECKED_CAST")
                return MatchViewModel(
                    MatchRepositoryImpl(db.matchDao(), db.matchEventDao()),
                    PlayerRepositoryImpl(db.playerDao(), db.matchDao()),
                    teamId
                ) as T
            }
        }
    }
}
