package com.luis.alhendinfc.ui.team

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.domain.repository.TeamRepository
import com.luis.alhendinfc.domain.repository.TeamRepositoryImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeamViewModel(private val repository: TeamRepository) : ViewModel() {

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
        viewModelScope.launch { repository.addTeam(team) }
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

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AlhendinDatabase.getInstance(context)
                    val repository = TeamRepositoryImpl(db.teamDao())
                    return TeamViewModel(repository) as T
                }
            }
    }
}
