package com.luis.alhendinfc.ui.rivals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RivalListViewModel(
    private val calendarRepository: SeasonCalendarRepository,
    private val teamId: Int
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val clubs: StateFlow<List<OpponentClub>> =
        _query.flatMapLatest { calendarRepository.searchClubs(teamId, it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun addClub(
        name: String,
        shortName: String,
        stadium: String,
        shieldUri: String?,
        kitColors: String,
        onCreated: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val id = calendarRepository.addClub(
                OpponentClub(
                    teamId = teamId,
                    name = name.trim(),
                    shortName = shortName.trim(),
                    stadium = stadium.trim(),
                    shieldUri = shieldUri,
                    kitColors = kitColors.trim()
                )
            )
            onCreated(id)
        }
    }

    fun deleteClub(club: OpponentClub) {
        viewModelScope.launch { calendarRepository.deleteClub(club) }
    }

    companion object {
        fun factory(context: Context, teamId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AlhendinDatabase.getInstance(context.applicationContext)
                    return RivalListViewModel(
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        teamId
                    ) as T
                }
            }
    }
}
