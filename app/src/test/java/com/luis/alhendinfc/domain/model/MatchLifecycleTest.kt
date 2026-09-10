package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchLifecycleTest {

    @Test
    fun openToLive_changesStatusAndResetsLiveFields() {
        val open = sampleMatch(status = MatchStatus.OPEN, liveElapsedSeconds = 99, homeScore = 3)
        val live = MatchLifecycle.markLive(open)
        assertEquals(MatchStatus.LIVE, live.status)
        assertEquals(0, live.homeScore)
        assertEquals(0, live.awayScore)
        assertEquals(0, live.liveElapsedSeconds)
        assertFalse(live.liveClockRunning)
        assertEquals("", live.fieldPositionsJson)
    }

    @Test
    fun liveToFinished_persistsFinalScoreAndStopsClock() {
        val live = sampleMatch(
            status = MatchStatus.LIVE,
            liveElapsedSeconds = 2700,
            liveClockRunning = true,
            liveClockAnchorWallMs = 123L,
            homeScore = 1,
            awayScore = 0
        )
        val finished = MatchLifecycle.markFinished(
            current = live,
            homeScore = 2,
            awayScore = 1,
            livePeriod = 2,
            liveElapsedSeconds = 2700,
            fieldSecondsJson = "1:90",
            fieldPositionsJson = "1:0.5:0.5"
        )
        assertEquals(MatchStatus.FINISHED, finished.status)
        assertEquals(2, finished.homeScore)
        assertEquals(1, finished.awayScore)
        assertEquals(2, finished.livePeriod)
        assertEquals(2700, finished.liveElapsedSeconds)
        assertFalse(finished.liveClockRunning)
        assertEquals(0L, finished.liveClockAnchorWallMs)
        assertEquals("1:90", finished.fieldSecondsJson)
        assertEquals("1:0.5:0.5", finished.fieldPositionsJson)
    }

    @Test
    fun finishedStaysFinished_whenReopenedOrMarkedLiveAgain() {
        val finished = MatchLifecycle.markFinished(
            sampleMatch(status = MatchStatus.LIVE),
            homeScore = 2,
            awayScore = 1,
            livePeriod = 2,
            liveElapsedSeconds = 2700,
            fieldSecondsJson = "",
            fieldPositionsJson = ""
        )
        val reopened = MatchLifecycle.markLive(finished)
        assertEquals(MatchStatus.FINISHED, reopened.status)
        assertEquals(2, reopened.homeScore)
        assertEquals(1, reopened.awayScore)
        val again = MatchLifecycle.markFinished(
            finished,
            homeScore = 9,
            awayScore = 9,
            livePeriod = 1,
            liveElapsedSeconds = 0,
            fieldSecondsJson = "stale",
            fieldPositionsJson = "stale"
        )
        assertEquals(MatchStatus.FINISHED, again.status)
        assertEquals(2, again.homeScore)
        assertEquals(1, again.awayScore)
        assertEquals("", again.fieldSecondsJson)
    }

    @Test
    fun fixtureWithExistingMatch_reusesSameMatch() {
        val existing = sampleMatch(id = 42, matchday = 7, status = MatchStatus.OPEN)
        val resolved = MatchLifecycle.resolveMatchForFixture(listOf(existing), 7)
        assertEquals(42, resolved?.id)
        assertFalse(MatchLifecycle.shouldCreateMatch(resolved))
    }

    @Test
    fun fixtureWithFinishedMatch_doesNotCreateAnother() {
        val finished = sampleMatch(id = 11, matchday = 4, status = MatchStatus.FINISHED)
        val resolved = MatchLifecycle.resolveMatchForFixture(listOf(finished), 4)
        assertEquals(11, resolved?.id)
        assertEquals(MatchStatus.FINISHED, resolved?.status)
        assertFalse(MatchLifecycle.shouldCreateMatch(resolved))
        assertNull(MatchLifecycle.resolveMatchForFixture(listOf(finished), 99))
        assertTrue(MatchLifecycle.shouldCreateMatch(null))
    }

    @Test
    fun fixturePrefersLiveThenOpenThenFinished() {
        val matches = listOf(
            sampleMatch(id = 1, matchday = 2, status = MatchStatus.FINISHED),
            sampleMatch(id = 2, matchday = 2, status = MatchStatus.OPEN),
            sampleMatch(id = 3, matchday = 2, status = MatchStatus.LIVE)
        )
        assertEquals(3, MatchLifecycle.resolveMatchForFixture(matches, 2)?.id)
        assertEquals(listOf(2), MatchLifecycle.duplicateMatchdays(matches))
    }

    @Test
    fun fixtureActionLabel_matchesAssociatedStatus() {
        assertEquals("Preparar", MatchLifecycle.fixtureActionLabel(null))
        assertEquals(
            "Continuar preparación",
            MatchLifecycle.fixtureActionLabel(sampleMatch(status = MatchStatus.OPEN))
        )
        assertEquals(
            "Ir al partido",
            MatchLifecycle.fixtureActionLabel(
                sampleMatch(status = MatchStatus.LIVE, liveElapsedSeconds = 12)
            )
        )
        assertEquals(
            "Continuar preparación",
            MatchLifecycle.fixtureActionLabel(
                sampleMatch(status = MatchStatus.LIVE, liveElapsedSeconds = 0)
            )
        )
        assertEquals(
            "Ver partido",
            MatchLifecycle.fixtureActionLabel(sampleMatch(status = MatchStatus.FINISHED))
        )
    }

    @Test
    fun fixtureFinishedAction_reusesExistingAndDoesNotCreate() {
        val finished = sampleMatch(id = 11, matchday = 4, status = MatchStatus.FINISHED)
        val resolved = MatchLifecycle.resolveMatchForFixture(listOf(finished), 4)
        assertEquals("Ver partido", MatchLifecycle.fixtureActionLabel(resolved))
        assertFalse(MatchLifecycle.shouldCreateMatch(resolved))
        assertEquals(MatchStatus.FINISHED, resolved?.status)
    }

    @Test
    fun persistPositions_doesNotOverwriteStatus() {
        val live = sampleMatch(
            status = MatchStatus.LIVE,
            liveElapsedSeconds = 120,
            homeScore = 1,
            awayScore = 0,
            fieldPositionsJson = ""
        )
        val next = MatchLifecycle.applyFieldPositions(live, "1:0.2:0.3")
        assertEquals(MatchStatus.LIVE, next.status)
        assertEquals("1:0.2:0.3", next.fieldPositionsJson)
    }

    @Test
    fun persistPositions_doesNotOverwriteClock() {
        val live = sampleMatch(
            status = MatchStatus.LIVE,
            liveElapsedSeconds = 845,
            liveClockRunning = true,
            liveClockAnchorWallMs = 999L,
            livePeriod = 2,
            fieldSecondsJson = "7:100"
        )
        val next = MatchLifecycle.applyFieldPositions(live, "1:0.4:0.5")
        assertEquals(845, next.liveElapsedSeconds)
        assertTrue(next.liveClockRunning)
        assertEquals(999L, next.liveClockAnchorWallMs)
        assertEquals(2, next.livePeriod)
        assertEquals("7:100", next.fieldSecondsJson)
    }

    @Test
    fun persistPositions_doesNotOverwriteScore() {
        val live = sampleMatch(
            status = MatchStatus.LIVE,
            homeScore = 3,
            awayScore = 2
        )
        val next = MatchLifecycle.applyFieldPositions(live, "8:0.1:0.9")
        assertEquals(3, next.homeScore)
        assertEquals(2, next.awayScore)
    }

    @Test
    fun liveWithoutClock_isPreparationNotShownAsLive() {
        val idle = sampleMatch(status = MatchStatus.LIVE, liveElapsedSeconds = 0)
        assertFalse(MatchLifecycle.isShownAsLive(idle))
        assertEquals("Preparación", MatchLifecycle.listBadgeLabel(idle))
        val running = sampleMatch(
            status = MatchStatus.LIVE,
            liveElapsedSeconds = 0,
            liveClockRunning = true
        )
        assertTrue(MatchLifecycle.isShownAsLive(running))
        val played = sampleMatch(status = MatchStatus.LIVE, liveElapsedSeconds = 15)
        assertTrue(MatchLifecycle.isShownAsLive(played))
        assertEquals("En vivo", MatchLifecycle.listBadgeLabel(played))
        assertEquals("Preparación", MatchLifecycle.listBadgeLabel(sampleMatch(status = MatchStatus.OPEN)))
    }

    @Test
    fun persistPositionsOnFinished_isNoOp() {
        val finished = sampleMatch(
            status = MatchStatus.FINISHED,
            homeScore = 2,
            awayScore = 1,
            liveElapsedSeconds = 2700,
            fieldPositionsJson = "1:0.5:0.5"
        )
        val staleUi = finished.copy(
            status = MatchStatus.LIVE,
            liveElapsedSeconds = 10,
            homeScore = 0,
            awayScore = 0
        )
        val buggyFullCopy = staleUi.copy(fieldPositionsJson = "9:0.1:0.1")
        assertEquals(MatchStatus.LIVE, buggyFullCopy.status)
        assertEquals(10, buggyFullCopy.liveElapsedSeconds)
        assertEquals(0, buggyFullCopy.homeScore)

        val safe = MatchLifecycle.applyFieldPositions(finished, "9:0.1:0.1")
        assertEquals(MatchStatus.FINISHED, safe.status)
        assertEquals(2700, safe.liveElapsedSeconds)
        assertEquals(2, safe.homeScore)
        assertEquals(1, safe.awayScore)
        assertEquals("1:0.5:0.5", safe.fieldPositionsJson)
    }

    @Test
    fun persistClockOrScoreOnFinished_isNoOp() {
        val finished = sampleMatch(
            status = MatchStatus.FINISHED,
            homeScore = 2,
            awayScore = 1,
            liveElapsedSeconds = 2700
        )
        val clocked = MatchLifecycle.applyLiveClock(
            finished,
            elapsedSeconds = 1,
            running = true,
            anchorWallMs = 50L,
            period = 1,
            fieldSecondsJson = "x"
        )
        val scored = MatchLifecycle.applyLiveScore(finished, 8, 8)
        assertEquals(finished, clocked)
        assertEquals(finished, scored)
    }

    private fun sampleMatch(
        id: Int = 1,
        matchday: Int = 1,
        status: MatchStatus = MatchStatus.OPEN,
        homeScore: Int? = null,
        awayScore: Int? = null,
        livePeriod: Int = 1,
        liveElapsedSeconds: Int = 0,
        liveClockRunning: Boolean = false,
        liveClockAnchorWallMs: Long = 0L,
        fieldSecondsJson: String = "",
        fieldPositionsJson: String = ""
    ) = Match(
        id = id,
        teamId = 10,
        rival = "Rival",
        matchday = matchday,
        status = status,
        homeScore = homeScore,
        awayScore = awayScore,
        livePeriod = livePeriod,
        liveElapsedSeconds = liveElapsedSeconds,
        liveClockRunning = liveClockRunning,
        liveClockAnchorWallMs = liveClockAnchorWallMs,
        fieldSecondsJson = fieldSecondsJson,
        fieldPositionsJson = fieldPositionsJson
    )
}
