package com.luis.alhendinfc.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.preferences.HomePreferencesRepository
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.HomeLayoutConfig
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchLifecycle
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val preferences: HomePreferencesRepository,
    private val calendarRepository: SeasonCalendarRepository,
    private val matchRepository: MatchRepositoryImpl
) : ViewModel() {

    private val teamIdFlow = MutableStateFlow<Int?>(null)

    val layoutConfig: StateFlow<HomeLayoutConfig> = preferences.layoutConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeLayoutConfig.defaults())

    val nextFixture: StateFlow<FixtureRow?> = teamIdFlow
        .flatMapLatest { teamId ->
            if (teamId == null) flowOf(null)
            else combine(
                calendarRepository.getFixtureRows(teamId),
                matchRepository.getMatchesByTeam(teamId)
            ) { fixtures, matches ->
                findNextFixture(fixtures, matches)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val liveMatch: StateFlow<Match?> = teamIdFlow
        .flatMapLatest { teamId ->
            if (teamId == null) flowOf(null)
            else matchRepository.getMatchesByTeam(teamId).map { list ->
                list.firstOrNull { MatchLifecycle.isShownAsLive(it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTeamId(teamId: Int?) {
        if (teamIdFlow.value != teamId) {
            teamIdFlow.value = teamId
        }
    }

    companion object {
        fun findNextFixture(fixtures: List<FixtureRow>, matches: List<Match>): FixtureRow? {
            if (fixtures.isEmpty()) return null
            val finishedDays = matches
                .filter { it.status == MatchStatus.FINISHED }
                .map { it.matchday }
                .toSet()
            return fixtures
                .sortedBy { it.fixture.matchday }
                .firstOrNull { it.fixture.matchday !in finishedDays }
                ?: fixtures.maxByOrNull { it.fixture.matchday }
        }

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    return HomeViewModel(
                        HomePreferencesRepository.getInstance(app),
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        MatchRepositoryImpl(db.matchDao(), db.matchEventDao())
                    ) as T
                }
            }
    }
}
