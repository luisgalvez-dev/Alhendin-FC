package com.luis.alhendinfc.ui.players

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.repository.PlayerRepository
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: PlayerRepository,
    private val teamId: Int
) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.getPlayersByTeam(teamId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _selectedPlayer = MutableStateFlow<Player?>(null)
    val selectedPlayer: StateFlow<Player?> = _selectedPlayer

    fun addPlayer(player: Player) {
        viewModelScope.launch { repository.addPlayer(player) }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch { repository.updatePlayer(player) }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch { repository.deletePlayer(player) }
    }

    fun selectPlayer(player: Player?) {
        _selectedPlayer.value = player
    }

    companion object {
        fun factory(context: Context, teamId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AlhendinDatabase.getInstance(context)
                    val repository = PlayerRepositoryImpl(db.playerDao(), db.matchDao())
                    return PlayerViewModel(repository, teamId) as T
                }
            }
    }
}
