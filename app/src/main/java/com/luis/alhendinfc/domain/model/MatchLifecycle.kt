package com.luis.alhendinfc.domain.model

/**
 * Reglas de ciclo de vida de un partido y de persistencia parcial (columnas).
 *
 * Las cláusulas WHERE de [com.luis.alhendinfc.data.local.MatchDao] deben permanecer
 * alineadas con esta lógica: no escribir la entidad completa desde una copia de UI
 * potencialmente obsoleta.
 */
object MatchLifecycle {

    fun shouldCreateMatch(existing: Match?): Boolean = existing == null

    /**
     * Una jornada se asocia a un único partido. Si hay varios históricos,
     * se reutiliza LIVE, luego OPEN, luego FINISHED, y por último el de mayor id.
     */
    fun resolveMatchForFixture(matches: List<Match>, matchday: Int): Match? {
        val sameDay = matches.filter { it.matchday == matchday }
        if (sameDay.isEmpty()) return null
        return sameDay.firstOrNull { it.status == MatchStatus.LIVE }
            ?: sameDay.firstOrNull { it.status == MatchStatus.OPEN }
            ?: sameDay.firstOrNull { it.status == MatchStatus.FINISHED }
            ?: sameDay.maxByOrNull { it.id }
    }

    /** Jornadas con más de un Match (duplicados históricos; no se borran). */
    fun duplicateMatchdays(matches: List<Match>): List<Int> =
        matches.groupBy { it.matchday }
            .filter { it.value.size > 1 }
            .keys
            .sorted()

    fun markLive(current: Match): Match {
        if (current.status != MatchStatus.OPEN) return current
        return current.copy(
            status = MatchStatus.LIVE,
            homeScore = 0,
            awayScore = 0,
            livePeriod = 1,
            liveElapsedSeconds = 0,
            liveClockRunning = false,
            liveClockAnchorWallMs = 0L,
            fieldSecondsJson = "",
            fieldPositionsJson = ""
        )
    }

    fun markFinished(
        current: Match,
        homeScore: Int,
        awayScore: Int,
        livePeriod: Int,
        liveElapsedSeconds: Int,
        fieldSecondsJson: String,
        fieldPositionsJson: String
    ): Match {
        if (current.status == MatchStatus.FINISHED) return current
        return current.copy(
            status = MatchStatus.FINISHED,
            homeScore = homeScore,
            awayScore = awayScore,
            livePeriod = livePeriod,
            liveElapsedSeconds = liveElapsedSeconds,
            liveClockRunning = false,
            liveClockAnchorWallMs = 0L,
            fieldSecondsJson = fieldSecondsJson,
            fieldPositionsJson = fieldPositionsJson
        )
    }

    /** Solo cambia JSON de posiciones; no toca status, cronómetro ni resultado. */
    fun applyFieldPositions(current: Match, fieldPositionsJson: String): Match {
        if (current.status != MatchStatus.LIVE) return current
        return current.copy(fieldPositionsJson = fieldPositionsJson)
    }

    fun applyLiveClock(
        current: Match,
        elapsedSeconds: Int,
        running: Boolean,
        anchorWallMs: Long,
        period: Int,
        fieldSecondsJson: String
    ): Match {
        if (current.status != MatchStatus.LIVE) return current
        return current.copy(
            liveElapsedSeconds = elapsedSeconds,
            liveClockRunning = running,
            liveClockAnchorWallMs = anchorWallMs,
            livePeriod = period,
            fieldSecondsJson = fieldSecondsJson
        )
    }

    fun applyLiveScore(current: Match, homeScore: Int, awayScore: Int): Match {
        if (current.status != MatchStatus.LIVE) return current
        return current.copy(homeScore = homeScore, awayScore = awayScore)
    }

    /** Texto del botón de jornada según el partido asociado (o su ausencia). */
    fun fixtureActionLabel(match: Match?): String = when (match?.status) {
        null -> "Preparar"
        MatchStatus.OPEN -> "Continuar preparación"
        MatchStatus.LIVE -> "Ir al partido"
        MatchStatus.FINISHED -> "Ver partido"
    }
}
