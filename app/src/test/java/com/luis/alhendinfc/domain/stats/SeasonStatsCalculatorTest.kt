package com.luis.alhendinfc.domain.stats

import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.StatisticType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonStatsCalculatorTest {

    private val player = Player(id = 1, teamId = 10, name = "Jugador")

    @Test
    fun twoYellowsInDifferentMatches_areNotARed() {
        val events = listOf(
            yellow(matchId = 1),
            yellow(matchId = 2)
        )
        val totals = SeasonStatsCalculator.cardTotals(events)
        assertEquals(2, totals.yellowCards)
        assertEquals(0, totals.redCards)
    }

    @Test
    fun twoYellowsInSameMatch_countAsOneExpulsion() {
        val events = listOf(
            yellow(matchId = 1),
            yellow(matchId = 1)
        )
        val totals = SeasonStatsCalculator.cardTotals(events)
        assertEquals(2, totals.yellowCards)
        assertEquals(1, totals.redCards)
        assertTrue(SeasonStatsCalculator.sentOffInMatch(events))
    }

    @Test
    fun directRed_countsAsOneExpulsion() {
        val events = listOf(red(matchId = 1))
        val totals = SeasonStatsCalculator.cardTotals(events)
        assertEquals(0, totals.yellowCards)
        assertEquals(1, totals.redCards)
    }

    @Test
    fun directRedAndDoubleYellowInSameMatch_countAsAtMostOneExpulsion() {
        val events = listOf(
            yellow(matchId = 1),
            yellow(matchId = 1),
            red(matchId = 1)
        )
        val totals = SeasonStatsCalculator.cardTotals(events)
        assertEquals(2, totals.yellowCards)
        assertEquals(1, totals.redCards)
    }

    @Test
    fun expulsionsInDifferentMatches_areCountedSeparately() {
        val events = listOf(
            red(matchId = 1),
            yellow(matchId = 2),
            yellow(matchId = 2)
        )
        val totals = SeasonStatsCalculator.cardTotals(events)
        assertEquals(2, totals.yellowCards)
        assertEquals(2, totals.redCards)
    }

    @Test
    fun eventsFromAnotherTeam_doNotAffectStats() {
        val teamMatchIds = setOf(1)
        val events = listOf(
            yellow(matchId = 1),
            yellow(matchId = 99),
            red(matchId = 99)
        )
        val stats = SeasonStatsCalculator.forPlayer(
            player = player,
            events = events,
            callups = listOf(
                MatchPlayer(id = 1, matchId = 1, playerId = 1),
                MatchPlayer(id = 2, matchId = 99, playerId = 1)
            ),
            customTypes = emptyList(),
            teamMatchIds = teamMatchIds
        )
        assertEquals(1, stats.yellowCards)
        assertEquals(0, stats.redCards)
        assertEquals(1, stats.matchesPlayed)
        assertEquals(0, stats.minutesPlayed)
        assertEquals(0, stats.secondsPlayed)
    }

    @Test
    fun otherTeamEventsWithSameTypeCode_areExcludedFromTeamCount() {
        val events = listOf(
            MatchEvent(matchId = 1, typeCode = "CUSTOM_X", playerId = 1),
            MatchEvent(matchId = 2, typeCode = "CUSTOM_X", playerId = 8)
        )
        val matchIdToTeamId = mapOf(1 to 10, 2 to 20)
        val count = SeasonStatsCalculator.countEventsWithCodeForTeam(
            typeCode = "CUSTOM_X",
            teamId = 10,
            events = events,
            matchIdToTeamId = matchIdToTeamId
        )
        assertEquals(1, count)
        assertFalse(
            SeasonStatsCalculator.countEventsWithCodeForTeam(
                "CUSTOM_X",
                10,
                events,
                matchIdToTeamId
            ) == 2
        )
    }

    @Test
    fun nineHundredSeconds_areFifteenMinutes() {
        val stats = statsFor(
            callups = listOf(titular(matchId = 1)),
            matches = listOf(finished(1, "1:900"))
        )
        assertEquals(900, stats.secondsPlayed)
        assertEquals(15, stats.minutesPlayed)
        assertEquals(1, stats.matchesPlayed)
        assertEquals(1, stats.starts)
        assertEquals(1, stats.callUps)
    }

    @Test
    fun minutes_sumAcrossFinishedMatches() {
        val stats = statsFor(
            callups = listOf(titular(matchId = 1), suplente(matchId = 2)),
            matches = listOf(finished(1, "1:600"), finished(2, "1:300"))
        )
        assertEquals(900, stats.secondsPlayed)
        assertEquals(15, stats.minutesPlayed)
        assertEquals(1, stats.starts)
        assertEquals(2, stats.callUps)
        assertEquals(2, stats.matchesPlayed)
    }

    @Test
    fun substituteWhoEnters_countsMinutesButNotStart() {
        val stats = statsFor(
            callups = listOf(suplente(matchId = 1)),
            matches = listOf(finished(1, "1:420"))
        )
        assertEquals(420, stats.secondsPlayed)
        assertEquals(7, stats.minutesPlayed)
        assertEquals(0, stats.starts)
        assertEquals(1, stats.callUps)
        assertEquals(1, stats.matchesPlayed)
    }

    @Test
    fun starterWhoLeaves_keepsPartialMinutesAndStart() {
        val stats = statsFor(
            callups = listOf(titular(matchId = 1)),
            matches = listOf(finished(1, "1:510"))
        )
        assertEquals(510, stats.secondsPlayed)
        assertEquals(8, stats.minutesPlayed)
        assertEquals(1, stats.starts)
    }

    @Test
    fun playerWithoutMinutes_staysAtZero() {
        val stats = statsFor(
            callups = listOf(titular(matchId = 1)),
            matches = listOf(finished(1, "8:900"))
        )
        assertEquals(0, stats.secondsPlayed)
        assertEquals(0, stats.minutesPlayed)
        assertEquals(1, stats.starts)
    }

    @Test
    fun unfinishedMatch_doesNotAddMinutes() {
        val live = Match(
            id = 2,
            teamId = 10,
            status = MatchStatus.LIVE,
            fieldSecondsJson = "1:900"
        )
        val stats = statsFor(
            teamMatchIds = setOf(1, 2),
            callups = listOf(titular(matchId = 1), titular(matchId = 2)),
            matches = listOf(finished(1, "1:60"), live)
        )
        assertEquals(60, stats.secondsPlayed)
        assertEquals(1, stats.minutesPlayed)
    }

    @Test
    fun emptyOrInvalidFieldSecondsJson_doesNotCrash() {
        val matches = listOf(
            finished(1, ""),
            finished(2, "   "),
            finished(3, "foo,1:abc,2:,1:120,bar")
        )
        val stats = statsFor(
            teamMatchIds = setOf(1, 2, 3),
            callups = listOf(titular(1), titular(2), titular(3)),
            matches = matches
        )
        assertEquals(120, stats.secondsPlayed)
        assertEquals(2, stats.minutesPlayed)
    }

    @Test
    fun previousPjGoalsAssistsCards_stayUnchanged() {
        val events = listOf(
            MatchEvent.builtin(matchId = 1, type = StatisticType.GOAL, playerId = 1),
            MatchEvent.builtin(matchId = 1, type = StatisticType.ASSIST, playerId = 1),
            yellow(matchId = 1)
        )
        val stats = SeasonStatsCalculator.forPlayer(
            player = player,
            events = events,
            callups = listOf(titular(matchId = 1)),
            customTypes = emptyList(),
            teamMatchIds = setOf(1),
            finishedMatches = listOf(finished(1, "1:900"))
        )
        assertEquals(1, stats.matchesPlayed)
        assertEquals(1, stats.goals)
        assertEquals(1, stats.assists)
        assertEquals(1, stats.yellowCards)
        assertEquals(0, stats.redCards)
        assertEquals(15, stats.minutesPlayed)
    }

    private fun statsFor(
        teamMatchIds: Set<Int> = setOf(1, 2, 3),
        callups: List<MatchPlayer>,
        matches: List<Match>
    ) = SeasonStatsCalculator.forPlayer(
        player = player,
        events = emptyList(),
        callups = callups,
        customTypes = emptyList(),
        teamMatchIds = teamMatchIds,
        finishedMatches = matches
    )

    private fun finished(id: Int, fieldSecondsJson: String) = Match(
        id = id,
        teamId = 10,
        status = MatchStatus.FINISHED,
        fieldSecondsJson = fieldSecondsJson
    )

    private fun titular(matchId: Int) = MatchPlayer(
        id = matchId,
        matchId = matchId,
        playerId = player.id,
        callupStatus = CallupStatus.TITULAR
    )

    private fun suplente(matchId: Int) = MatchPlayer(
        id = matchId + 10,
        matchId = matchId,
        playerId = player.id,
        callupStatus = CallupStatus.SUPLENTE
    )

    private fun yellow(matchId: Int) = MatchEvent.builtin(
        matchId = matchId,
        type = StatisticType.YELLOW_CARD,
        playerId = player.id
    )

    private fun red(matchId: Int) = MatchEvent.builtin(
        matchId = matchId,
        type = StatisticType.RED_CARD,
        playerId = player.id
    )
}
