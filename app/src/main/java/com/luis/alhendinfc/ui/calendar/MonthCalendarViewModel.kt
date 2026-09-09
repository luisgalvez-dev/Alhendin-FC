package com.luis.alhendinfc.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.CalendarDate
import com.luis.alhendinfc.domain.model.CalendarDayContent
import com.luis.alhendinfc.domain.model.CalendarMonthRules
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchLifecycle
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Training
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import com.luis.alhendinfc.domain.repository.TrainingRepositoryImpl
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonthCalendarViewModel(
    private val calendarRepository: SeasonCalendarRepository,
    private val matchRepository: MatchRepositoryImpl,
    private val trainingRepository: TrainingRepositoryImpl,
    private val teamId: Int
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = month

    val fixtures: StateFlow<List<FixtureRow>> =
        calendarRepository.getFixtureRows(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val matches: StateFlow<List<Match>> =
        matchRepository.getMatchesByTeam(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trainings: StateFlow<List<Training>> =
        trainingRepository.getByTeam(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dayContents: StateFlow<Map<Long, CalendarDayContent>> =
        combine(matches, fixtures, trainings) { matchList, fixtureList, trainingList ->
            val epochs = buildSet {
                matchList.forEach { CalendarDate.toEpochDay(it.date)?.let(::add) }
                fixtureList.forEach { CalendarDate.toEpochDay(it.fixture.date)?.let(::add) }
                trainingList.forEach { add(it.dateEpochDay) }
            }
            epochs.associateWith { epoch ->
                CalendarMonthRules.contentForDay(epoch, matchList, fixtureList, trainingList)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun goToToday() {
        month.value = YearMonth.now()
    }

    fun content(epochDay: Long): CalendarDayContent =
        dayContents.value[epochDay]
            ?: CalendarMonthRules.contentForDay(
                epochDay,
                matches.value,
                fixtures.value,
                trainings.value
            )

    fun openOrPrepareMatch(row: FixtureRow, onReady: (Int) -> Unit) {
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
                    return MonthCalendarViewModel(
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        MatchRepositoryImpl(db.matchDao(), db.matchEventDao()),
                        TrainingRepositoryImpl(
                            db.trainingDao(),
                            db.trainingTaskDao(),
                            db.taskDao(),
                            db.matchDao(),
                            db.seasonFixtureDao(),
                            db.attachmentDao()
                        ),
                        teamId
                    ) as T
                }
            }
    }
}
