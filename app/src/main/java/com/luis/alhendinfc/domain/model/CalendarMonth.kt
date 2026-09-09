package com.luis.alhendinfc.domain.model

/**
 * Un día del calendario mensual muestra un partido (Match o Fixture equivalente)
 * y, si existiera un entrenamiento histórico, también el training.
 * Crear un training nuevo solo está permitido si el día está vacío.
 */
sealed class CalendarDayEntry {
    data class MatchEntry(val match: Match) : CalendarDayEntry()
    data class FixtureEntry(val row: FixtureRow) : CalendarDayEntry()
    data class TrainingEntry(val training: Training) : CalendarDayEntry()
}

data class CalendarDayContent(
    val epochDay: Long,
    val matchOrFixture: CalendarDayEntry?,
    val training: Training?
) {
    val hasMatchOrFixture: Boolean get() = matchOrFixture != null
    val hasTraining: Boolean get() = training != null
    val isEmpty: Boolean get() = !hasMatchOrFixture && !hasTraining
    val canAddTraining: Boolean get() = isEmpty
}

object CalendarMonthRules {

    fun contentForDay(
        epochDay: Long,
        matches: List<Match>,
        fixtures: List<FixtureRow>,
        trainings: List<Training>
    ): CalendarDayContent {
        val matchesThatDay = matches.filter { CalendarDate.toEpochDay(it.date) == epochDay }
        val fixturesThatDay = fixtures.filter {
            CalendarDate.toEpochDay(it.fixture.date) == epochDay
        }
        val matchOrFixture = when {
            matchesThatDay.isNotEmpty() ->
                CalendarDayEntry.MatchEntry(pickMatch(matchesThatDay)!!)
            else -> unmatchedFixtures(epochDay, fixturesThatDay, matches)
                .firstOrNull()
                ?.let { CalendarDayEntry.FixtureEntry(it) }
        }
        val training = trainings
            .filter { it.dateEpochDay == epochDay || CalendarDate.toEpochDay(it.date) == epochDay }
            .minByOrNull { it.id }
        return CalendarDayContent(epochDay, matchOrFixture, training)
    }

    private fun pickMatch(sameDay: List<Match>): Match? {
        if (sameDay.isEmpty()) return null
        return sameDay.firstOrNull { it.status == MatchStatus.LIVE }
            ?: sameDay.firstOrNull { it.status == MatchStatus.OPEN }
            ?: sameDay.firstOrNull { it.status == MatchStatus.FINISHED }
            ?: sameDay.maxByOrNull { it.id }
    }

    private fun unmatchedFixtures(
        epochDay: Long,
        fixturesThatDay: List<FixtureRow>,
        allMatches: List<Match>
    ): List<FixtureRow> = fixturesThatDay.filter { row ->
        val linked = MatchLifecycle.resolveMatchForFixture(allMatches, row.fixture.matchday)
        linked == null || CalendarDate.toEpochDay(linked.date) != epochDay
    }
}
