package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarMonthRulesTest {

    private val day = CalendarDate.toEpochDay("08/09/2026")!!

    @Test
    fun matchAppearsOnItsDate() {
        val match = Match(id = 1, teamId = 1, rival = "Jaén", date = "08/09/2026")
        val content = CalendarMonthRules.contentForDay(day, listOf(match), emptyList(), emptyList())
        assertTrue(content.matchOrFixture is CalendarDayEntry.MatchEntry)
        assertEquals(1, (content.matchOrFixture as CalendarDayEntry.MatchEntry).match.id)
        assertFalse(content.canAddTraining)
    }

    @Test
    fun fixtureWithoutMatchAppears() {
        val row = FixtureRow(
            fixture = SeasonFixture(id = 3, teamId = 1, matchday = 2, opponentClubId = 9, date = "08/09/2026"),
            club = OpponentClub(id = 9, teamId = 1, name = "Úbeda")
        )
        val content = CalendarMonthRules.contentForDay(day, emptyList(), listOf(row), emptyList())
        assertTrue(content.matchOrFixture is CalendarDayEntry.FixtureEntry)
        assertFalse(content.canAddTraining)
    }

    @Test
    fun matchAndEquivalentFixtureAppearOncePreferringMatch() {
        val match = Match(id = 4, teamId = 1, rival = "Jaén", date = "08/09/2026", matchday = 2)
        val row = FixtureRow(
            fixture = SeasonFixture(id = 3, teamId = 1, matchday = 2, opponentClubId = 9, date = "08/09/2026"),
            club = OpponentClub(id = 9, teamId = 1, name = "Jaén")
        )
        val content = CalendarMonthRules.contentForDay(day, listOf(match), listOf(row), emptyList())
        assertTrue(content.matchOrFixture is CalendarDayEntry.MatchEntry)
        assertEquals(4, (content.matchOrFixture as CalendarDayEntry.MatchEntry).match.id)
    }

    @Test
    fun trainingAppearsAndEmptyDayAllowsTraining() {
        val training = Training(id = 7, teamId = 1, date = "08/09/2026", dateEpochDay = day)
        val withTraining = CalendarMonthRules.contentForDay(day, emptyList(), emptyList(), listOf(training))
        assertTrue(withTraining.hasTraining)
        assertFalse(withTraining.canAddTraining)
        val empty = CalendarMonthRules.contentForDay(day, emptyList(), emptyList(), emptyList())
        assertTrue(empty.isEmpty)
        assertTrue(empty.canAddTraining)
    }

    @Test
    fun matchDayDoesNotAllowTraining() {
        val match = Match(id = 1, teamId = 1, rival = "X", date = "08/09/2026")
        val content = CalendarMonthRules.contentForDay(day, listOf(match), emptyList(), emptyList())
        assertFalse(content.canAddTraining)
    }
}

class TrainingRulesTest {

    @Test
    fun onlyOneActiveTrainingPerTeamDay() {
        val day = 20_000L
        assertFalse(TrainingRules.canCreateTraining(day, hasActiveTraining = true, emptySet(), emptySet()))
        assertTrue(TrainingRules.canCreateTraining(day, hasActiveTraining = false, emptySet(), emptySet()))
    }

    @Test
    fun cannotCreateWhenMatchOrFixtureOccupiesDay() {
        val day = 20_000L
        assertFalse(TrainingRules.canCreateTraining(day, false, setOf(day), emptySet()))
        assertFalse(TrainingRules.canCreateTraining(day, false, emptySet(), setOf(day)))
        assertTrue(TrainingRules.canCreateTraining(day, false, setOf(day + 1), setOf(day - 1)))
    }
}
