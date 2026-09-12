package com.luis.alhendinfc.domain.model

enum class CalendarMatchMode { PENDING, RESULT }

/**
 * Presentación de un partido en la celda del calendario mensual.
 * No altera la semántica de ocupación del día.
 */
data class CalendarMatchVisual(
    val displayName: String,
    val initials: String,
    val shieldUri: String?,
    val hasRival: Boolean,
    val mode: CalendarMatchMode = CalendarMatchMode.PENDING,
    val ourGoals: Int? = null,
    val rivalGoals: Int? = null,
    val ourInitials: String = "ALH",
    val ourShieldUri: String? = null
) {
    val showsResult: Boolean
        get() = mode == CalendarMatchMode.RESULT && ourGoals != null && rivalGoals != null

    val scoreLabel: String
        get() = if (showsResult) "$ourGoals - $rivalGoals" else ""

    val contentDescription: String
        get() = when {
            showsResult && hasRival -> "Finalizado $scoreLabel contra $displayName"
            showsResult -> "Finalizado $scoreLabel"
            hasRival -> "Partido contra $displayName"
            else -> "Partido"
        }
}

object CalendarDayVisual {

    fun matchVisual(
        entry: CalendarDayEntry?,
        clubsById: Map<Int, OpponentClub> = emptyMap(),
        team: Team? = null,
        rivalShields: Map<String, Attachment> = emptyMap(),
        teamShields: Map<String, Attachment> = emptyMap(),
        clubsByMatchday: Map<Int, OpponentClub?> = emptyMap()
    ): CalendarMatchVisual? = when (entry) {
        is CalendarDayEntry.MatchEntry -> fromMatch(
            entry.match, clubsById, team, rivalShields, teamShields, clubsByMatchday
        )
        is CalendarDayEntry.FixtureEntry -> fromFixture(entry.row, team, rivalShields, teamShields)
        else -> null
    }

    fun fromMatch(
        match: Match,
        clubsById: Map<Int, OpponentClub> = emptyMap(),
        team: Team? = null,
        rivalShields: Map<String, Attachment> = emptyMap(),
        teamShields: Map<String, Attachment> = emptyMap(),
        clubsByMatchday: Map<Int, OpponentClub?> = emptyMap()
    ): CalendarMatchVisual {
        val club = resolveOpponentClub(match, clubsById, clubsByMatchday)
        val rivalPath = SharedMedia.rivalDisplayPath(
            opponentClubId = club?.id ?: match.opponentClubId,
            clubLegacyUri = club?.shieldUri,
            matchLegacyUri = match.rivalShieldUri,
            clubShared = club?.syncId?.takeIf { it.isNotBlank() }?.let { rivalShields[it] }
        )
        val pending = visual(
            name = club?.name?.ifBlank { null } ?: match.rival,
            shortName = club?.shortName.orEmpty(),
            shieldUri = rivalPath,
            team = team,
            teamShared = team?.syncId?.takeIf { it.isNotBlank() }?.let { teamShields[it] }
        )
        val score = scoredFromOurPerspective(match)
        if (!isFinishedWithScore(match) || score == null) return pending
        return pending.copy(
            mode = CalendarMatchMode.RESULT,
            ourGoals = score.first,
            rivalGoals = score.second
        )
    }

    fun fromFixture(
        row: FixtureRow,
        team: Team? = null,
        rivalShields: Map<String, Attachment> = emptyMap(),
        teamShields: Map<String, Attachment> = emptyMap()
    ): CalendarMatchVisual {
        val club = row.club
        return visual(
            name = club?.name.orEmpty(),
            shortName = club?.shortName.orEmpty(),
            shieldUri = SharedMedia.displayPath(
                club?.syncId?.takeIf { it.isNotBlank() }?.let { rivalShields[it] },
                club?.shieldUri
            ),
            team = team,
            teamShared = team?.syncId?.takeIf { it.isNotBlank() }?.let { teamShields[it] }
        )
    }

    fun visual(
        name: String,
        shortName: String = "",
        shieldUri: String?,
        team: Team? = null,
        teamShared: Attachment? = null
    ): CalendarMatchVisual {
        val fullName = name.trim()
        val short = shortName.trim()
        val hasRival = fullName.isNotEmpty() || short.isNotEmpty()
        val display = when {
            short.isNotEmpty() -> short
            fullName.isNotEmpty() -> fullName
            else -> "Partido"
        }
        return CalendarMatchVisual(
            displayName = display,
            initials = if (hasRival) initialsOf(fullName.ifBlank { short }) else "P",
            shieldUri = shieldUri?.trim()?.takeIf { it.isNotEmpty() },
            hasRival = hasRival,
            ourInitials = ownInitials(team?.name),
            ourShieldUri = SharedMedia.displayPath(teamShared, team?.shieldUri)
        )
    }

    /**
     * Misma fuente que la ficha del rival: OpponentClub del partido.
     * Si [Match.opponentClubId] no resuelve (IDs locales distintos, 0, o partido
     * sin club), reutiliza el club de la jornada o un único rival con el mismo nombre.
     */
    fun resolveOpponentClub(
        match: Match,
        clubsById: Map<Int, OpponentClub>,
        clubsByMatchday: Map<Int, OpponentClub?> = emptyMap()
    ): OpponentClub? {
        match.opponentClubId?.takeIf { it > 0 }?.let { clubsById[it] }?.let { return it }
        clubsByMatchday[match.matchday]?.let { return it }
        val rival = match.rival.trim()
        if (rival.isEmpty()) return null
        return clubsById.values.singleOrNull { club ->
            club.name.equals(rival, ignoreCase = true) ||
                club.shortName.equals(rival, ignoreCase = true)
        }
    }

    fun isFinishedWithScore(match: Match): Boolean =
        match.status == MatchStatus.FINISHED &&
            match.homeScore != null &&
            match.awayScore != null

    /**
     * Goles a favor y en contra desde la perspectiva de nuestro equipo.
     * [Match.homeScore]/[Match.awayScore] son local/visitante, no "nosotros/ellos".
     */
    fun scoredFromOurPerspective(match: Match): Pair<Int, Int>? {
        val home = match.homeScore ?: return null
        val away = match.awayScore ?: return null
        return if (match.isHome) home to away else away to home
    }

    /**
     * Dos iniciales a partir de las primeras palabras con letra.
     * "U.D. Maracena" → "UM"; "C.D. Huétor Vega" → "CH"; un solo nombre → una letra.
     */
    fun initialsOf(name: String): String {
        val letters = name.trim()
            .split(Regex("\\s+"))
            .mapNotNull { token ->
                token.firstOrNull { it.isLetter() }?.uppercaseChar()
            }
        return when {
            letters.size >= 2 -> "${letters[0]}${letters[1]}"
            letters.size == 1 -> letters[0].toString()
            else -> "P"
        }
    }

    /** Fallback del escudo propio: "Alhendín FC" → "ALH". */
    fun ownInitials(name: String?): String {
        val firstWord = name.orEmpty().trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        val letters = firstWord.filter { it.isLetter() }
        return when {
            letters.length >= 3 -> letters.take(3).uppercase()
            letters.isNotEmpty() -> letters.uppercase()
            else -> "ALH"
        }
    }
}
