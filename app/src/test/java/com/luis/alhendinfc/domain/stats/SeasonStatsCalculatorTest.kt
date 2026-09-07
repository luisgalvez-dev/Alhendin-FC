package com.luis.alhendinfc.domain.stats

import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
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
