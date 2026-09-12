package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDayVisualTest {

    @Test
    fun matchWithRivalAndShield_keepsUriAndShortName() {
        val club = OpponentClub(
            id = 9,
            teamId = 1,
            name = "U.D. Maracena",
            shortName = "Maracena",
            shieldUri = "content://media/maracena.png"
        )
        val match = Match(
            id = 1,
            teamId = 1,
            rival = "U.D. Maracena",
            date = "14/09/2026",
            opponentClubId = 9,
            rivalShieldUri = "content://media/maracena.png"
        )
        val visual = CalendarDayVisual.fromMatch(match, mapOf(9 to club))
        assertTrue(visual.hasRival)
        assertEquals("Maracena", visual.displayName)
        assertEquals("UM", visual.initials)
        assertEquals("content://media/maracena.png", visual.shieldUri)
        assertEquals(CalendarMatchMode.PENDING, visual.mode)
        assertFalse(visual.showsResult)
        assertEquals("Partido contra Maracena", visual.contentDescription)
    }

    @Test
    fun matchWithRivalWithoutShield_usesInitials() {
        val match = Match(id = 2, teamId = 1, rival = "C.D. Huétor Vega", date = "14/09/2026")
        val visual = CalendarDayVisual.fromMatch(match)
        assertTrue(visual.hasRival)
        assertEquals("C.D. Huétor Vega", visual.displayName)
        assertEquals("CH", visual.initials)
        assertNull(visual.shieldUri)
        assertEquals(CalendarMatchMode.PENDING, visual.mode)
        assertFalse(visual.showsResult)
    }

    @Test
    fun matchWithoutRival_genericFallback() {
        val match = Match(id = 3, teamId = 1, rival = "  ", date = "14/09/2026")
        val visual = CalendarDayVisual.fromMatch(match)
        assertFalse(visual.hasRival)
        assertEquals("Partido", visual.displayName)
        assertEquals("P", visual.initials)
        assertNull(visual.shieldUri)
        assertEquals(CalendarMatchMode.PENDING, visual.mode)
        assertEquals("Partido", visual.contentDescription)
    }

    @Test
    fun longRivalName_isKeptForUiTruncation() {
        val longName = "Club Deportivo Nombre Excesivamente Largo"
        val visual = CalendarDayVisual.visual(name = longName, shieldUri = null)
        assertEquals(longName, visual.displayName)
        assertEquals("CD", visual.initials)
        assertTrue(visual.hasRival)
    }

    @Test
    fun fixtureUsesClubShieldAndShortName() {
        val row = FixtureRow(
            fixture = SeasonFixture(id = 1, teamId = 1, matchday = 4, opponentClubId = 9, date = "14/09/2026"),
            club = OpponentClub(id = 9, teamId = 1, name = "U.D. Maracena", shortName = "Maracena", shieldUri = "file:///escudo.png")
        )
        val visual = CalendarDayVisual.fromFixture(row)
        assertEquals("Maracena", visual.displayName)
        assertEquals("UM", visual.initials)
        assertEquals("file:///escudo.png", visual.shieldUri)
        assertEquals(CalendarMatchMode.PENDING, visual.mode)
        assertFalse(visual.showsResult)
    }

    @Test
    fun fixtureWithoutClub_genericMatch() {
        val row = FixtureRow(
            fixture = SeasonFixture(id = 1, teamId = 1, matchday = 4, opponentClubId = 9, date = "14/09/2026"),
            club = null
        )
        val visual = CalendarDayVisual.fromFixture(row)
        assertFalse(visual.hasRival)
        assertEquals("Partido", visual.displayName)
        assertEquals("P", visual.initials)
        assertNull(visual.shieldUri)
    }

    @Test
    fun trainingDay_hasNoMatchVisual() {
        val day = CalendarDate.toEpochDay("14/09/2026")!!
        val training = Training(id = 7, teamId = 1, date = "14/09/2026", dateEpochDay = day)
        val content = CalendarMonthRules.contentForDay(day, emptyList(), emptyList(), listOf(training))
        assertTrue(content.hasTraining)
        assertFalse(content.hasMatchOrFixture)
        assertNull(CalendarDayVisual.matchVisual(content.matchOrFixture))
        assertFalse(content.canAddTraining)
    }

    @Test
    fun matchVisual_doesNotChangeCalendarSemantics() {
        val day = CalendarDate.toEpochDay("08/09/2026")!!
        val match = Match(id = 4, teamId = 1, rival = "Jaén", date = "08/09/2026", matchday = 2)
        val row = FixtureRow(
            fixture = SeasonFixture(id = 3, teamId = 1, matchday = 2, opponentClubId = 9, date = "08/09/2026"),
            club = OpponentClub(id = 9, teamId = 1, name = "Jaén")
        )
        val content = CalendarMonthRules.contentForDay(day, listOf(match), listOf(row), emptyList())
        assertTrue(content.matchOrFixture is CalendarDayEntry.MatchEntry)
        assertNotNull(CalendarDayVisual.matchVisual(content.matchOrFixture))
        assertFalse(content.canAddTraining)
    }

    @Test
    fun initialsOf_coversTypicalClubNames() {
        assertEquals("UM", CalendarDayVisual.initialsOf("U.D. Maracena"))
        assertEquals("CH", CalendarDayVisual.initialsOf("C.D. Huétor Vega"))
        assertEquals("M", CalendarDayVisual.initialsOf("Maracena"))
        assertEquals("P", CalendarDayVisual.initialsOf("   "))
    }

    @Test
    fun finishedHome_2_1_showsResultFromOurPerspective() {
        val team = Team(id = 1, name = "Alhendín FC", category = "", season = "", shieldUri = "content://media/alh.png")
        val match = Match(
            id = 10,
            teamId = 1,
            rival = "U.D. Maracena",
            date = "20/09/2026",
            isHome = true,
            status = MatchStatus.FINISHED,
            homeScore = 2,
            awayScore = 1,
            rivalShieldUri = "content://media/maracena.png"
        )
        val visual = CalendarDayVisual.fromMatch(match, team = team)
        assertTrue(visual.showsResult)
        assertEquals(CalendarMatchMode.RESULT, visual.mode)
        assertEquals(2, visual.ourGoals)
        assertEquals(1, visual.rivalGoals)
        assertEquals("2 - 1", visual.scoreLabel)
        assertEquals("content://media/alh.png", visual.ourShieldUri)
        assertEquals("content://media/maracena.png", visual.shieldUri)
        assertEquals("ALH", visual.ourInitials)
    }

    @Test
    fun finished_0_0_isARealScore() {
        val match = Match(
            id = 11,
            teamId = 1,
            rival = "Jaén",
            date = "21/09/2026",
            isHome = true,
            status = MatchStatus.FINISHED,
            homeScore = 0,
            awayScore = 0
        )
        val visual = CalendarDayVisual.fromMatch(match)
        assertTrue(visual.showsResult)
        assertEquals("0 - 0", visual.scoreLabel)
        assertEquals(0, visual.ourGoals)
        assertEquals(0, visual.rivalGoals)
    }

    @Test
    fun pastDateButNotFinished_doesNotShowScore() {
        val match = Match(
            id = 12,
            teamId = 1,
            rival = "Maracena",
            date = "01/01/2020",
            isHome = true,
            status = MatchStatus.OPEN,
            homeScore = 3,
            awayScore = 1
        )
        val visual = CalendarDayVisual.fromMatch(match)
        assertEquals(CalendarMatchMode.PENDING, visual.mode)
        assertFalse(visual.showsResult)
        assertEquals("", visual.scoreLabel)
    }

    @Test
    fun liveWithScore_doesNotShowResult() {
        val match = Match(
            id = 13,
            teamId = 1,
            rival = "Maracena",
            date = "01/01/2020",
            isHome = true,
            status = MatchStatus.LIVE,
            homeScore = 1,
            awayScore = 0
        )
        val visual = CalendarDayVisual.fromMatch(match)
        assertFalse(visual.showsResult)
        assertEquals(CalendarMatchMode.PENDING, visual.mode)
    }

    @Test
    fun finishedAway_keepsOurGoalsOnTheLeft() {
        val match = Match(
            id = 14,
            teamId = 1,
            rival = "Maracena",
            date = "22/09/2026",
            isHome = false,
            status = MatchStatus.FINISHED,
            homeScore = 1,
            awayScore = 2
        )
        val visual = CalendarDayVisual.fromMatch(match)
        assertTrue(visual.showsResult)
        assertEquals(2, visual.ourGoals)
        assertEquals(1, visual.rivalGoals)
        assertEquals("2 - 1", visual.scoreLabel)
    }

    @Test
    fun finishedWithoutOwnShield_usesAlhFallback() {
        val team = Team(id = 1, name = "Alhendín FC", category = "", season = "", shieldUri = null)
        val match = Match(
            id = 15,
            teamId = 1,
            rival = "Maracena",
            date = "22/09/2026",
            status = MatchStatus.FINISHED,
            homeScore = 1,
            awayScore = 0
        )
        val visual = CalendarDayVisual.fromMatch(match, team = team)
        assertTrue(visual.showsResult)
        assertNull(visual.ourShieldUri)
        assertEquals("ALH", visual.ourInitials)
    }

    @Test
    fun finishedWithoutScores_staysPending() {
        val match = Match(
            id = 16,
            teamId = 1,
            rival = "Maracena",
            date = "22/09/2026",
            status = MatchStatus.FINISHED,
            homeScore = null,
            awayScore = null
        )
        val visual = CalendarDayVisual.fromMatch(match)
        assertFalse(visual.showsResult)
        assertEquals(CalendarMatchMode.PENDING, visual.mode)
    }

    @Test
    fun calendarResolver_prefersOpponentSharedShieldOverLegacyAndMatchCopy() {
        val club = OpponentClub(
            id = 9,
            teamId = 1,
            name = "U.D. Maracena",
            shortName = "Maracena",
            shieldUri = "content://legacy-club.png",
            syncId = "club-sync"
        )
        val match = Match(
            id = 20,
            teamId = 1,
            rival = "U.D. Maracena",
            date = "14/09/2026",
            opponentClubId = 9,
            rivalShieldUri = "content://match-copy.png"
        )
        val shared = Attachment(
            syncId = "att-club",
            parentType = AttachmentParentType.OPPONENT_SHIELD,
            parentSyncId = "club-sync",
            mimeType = "image/png",
            name = "escudo.png",
            localPath = "/data/files/maracena.png"
        )
        val visual = CalendarDayVisual.fromMatch(
            match,
            mapOf(9 to club),
            rivalShields = mapOf("club-sync" to shared)
        )
        assertEquals("/data/files/maracena.png", visual.shieldUri)
    }

    @Test
    fun matchWithoutClub_keepsLegacyRivalShield() {
        val match = Match(
            id = 21,
            teamId = 1,
            rival = "Amistoso",
            date = "14/09/2026",
            opponentClubId = null,
            rivalShieldUri = "content://amistoso.png"
        )
        val visual = CalendarDayVisual.fromMatch(match)
        assertEquals("content://amistoso.png", visual.shieldUri)
    }

    @Test
    fun ownTeamSharedShield_beatsLegacy() {
        val team = Team(
            id = 1,
            name = "Alhendín FC",
            category = "",
            season = "",
            shieldUri = "content://old-alh.png",
            syncId = "team-sync"
        )
        val match = Match(
            id = 22,
            teamId = 1,
            rival = "Maracena",
            date = "20/09/2026",
            isHome = true,
            status = MatchStatus.FINISHED,
            homeScore = 1,
            awayScore = 0
        )
        val shared = Attachment(
            parentType = AttachmentParentType.TEAM_SHIELD,
            parentSyncId = "team-sync",
            mimeType = "image/png",
            name = "alh.png",
            localPath = "/data/files/alh.png"
        )
        val visual = CalendarDayVisual.fromMatch(
            match,
            team = team,
            teamShields = mapOf("team-sync" to shared)
        )
        assertEquals("/data/files/alh.png", visual.ourShieldUri)
    }

    @Test
    fun matchWithoutClubId_reusesFixtureClubSharedShield() {
        val club = OpponentClub(
            id = 3,
            teamId = 1,
            name = "U.D. Maracena",
            shortName = "Maracena",
            shieldUri = null,
            syncId = "club-sync"
        )
        val match = Match(
            id = 30,
            teamId = 1,
            rival = "U.D. Maracena",
            date = "14/09/2026",
            matchday = 4,
            opponentClubId = null,
            rivalShieldUri = null
        )
        val shared = Attachment(
            parentType = AttachmentParentType.OPPONENT_SHIELD,
            parentSyncId = "club-sync",
            mimeType = "image/png",
            name = "escudo.png",
            localPath = "/data/files/maracena.png"
        )
        val visual = CalendarDayVisual.fromMatch(
            match,
            clubsById = mapOf(3 to club),
            rivalShields = mapOf("club-sync" to shared),
            clubsByMatchday = mapOf(4 to club)
        )
        assertEquals("/data/files/maracena.png", visual.shieldUri)
        assertEquals("Maracena", visual.displayName)
    }

    @Test
    fun matchWithUnknownClubId_findsClubByRivalNameForSharedShield() {
        val club = OpponentClub(
            id = 3,
            teamId = 1,
            name = "U.D. Maracena",
            shortName = "Maracena",
            syncId = "club-sync"
        )
        val match = Match(
            id = 31,
            teamId = 1,
            rival = "U.D. Maracena",
            date = "14/09/2026",
            opponentClubId = 99,
            rivalShieldUri = "content://match-copy.png"
        )
        val shared = Attachment(
            parentType = AttachmentParentType.OPPONENT_SHIELD,
            parentSyncId = "club-sync",
            mimeType = "image/png",
            name = "escudo.png",
            localPath = "/data/files/maracena.png"
        )
        val visual = CalendarDayVisual.fromMatch(
            match,
            clubsById = mapOf(3 to club),
            rivalShields = mapOf("club-sync" to shared)
        )
        assertEquals("/data/files/maracena.png", visual.shieldUri)
    }
}
