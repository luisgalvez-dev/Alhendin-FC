package com.luis.alhendinfc.domain.model

data class OpponentClub(
    val id: Int = 0,
    val teamId: Int,
    val name: String,
    val shortName: String = "",
    val stadium: String = "",
    val shieldUri: String? = null,
    /** Texto libre: color(es) de la equipación. */
    val kitColors: String = "",
    val sortOrder: Int = 0
) {
    val displayShort: String get() = shortName.ifBlank { name.take(12) }
}

data class SeasonFixture(
    val id: Int = 0,
    val teamId: Int,
    val matchday: Int,
    val opponentClubId: Int,
    val isHome: Boolean = true,
    val date: String = "",
    val time: String = "",
    val stadiumOverride: String = ""
)

/** Fila enriquecida para la UI del calendario. */
data class FixtureRow(
    val fixture: SeasonFixture,
    val club: OpponentClub?
) {
    val stadium: String
        get() = fixture.stadiumOverride.ifBlank {
            if (fixture.isHome) "" else (club?.stadium ?: "")
        }.ifBlank { club?.stadium.orEmpty() }
}
