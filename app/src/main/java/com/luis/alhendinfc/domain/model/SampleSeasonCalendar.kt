package com.luis.alhendinfc.domain.model

/**
 * Clubs y calendario de prueba (Andalucía / Granada área ficticia).
 */
object SampleSeasonCalendar {

    data class ClubSeed(
        val name: String,
        val shortName: String,
        val stadium: String
    )

    private val clubs = listOf(
        ClubSeed("Atlético Monachil", "Monachil", "Municipal de Monachil"),
        ClubSeed("CD Santa Fe", "Santa Fe", "Estadio Municipal Santa Fe"),
        ClubSeed("UD Maracena", "Maracena", "Ciudad Deportiva Maracena"),
        ClubSeed("CD Huétor Vega", "Huétor V.", "Municipal Huétor Vega"),
        ClubSeed("Atlético Granadino", "At. Granadino", "Campo de la Juventud"),
        ClubSeed("CD Chauchina", "Chauchina", "Municipal de Chauchina"),
        ClubSeed("UD Ogíjares", "Ogíjares", "Polideportivo Ogíjares"),
        ClubSeed("CD Pinos Puente", "Pinos P.", "Municipal Pinos Puente"),
        ClubSeed("CD Güejar Sierra", "Güejar", "Municipal Güejar Sierra"),
        ClubSeed("CD Armilla", "Armilla", "Campo de Armilla")
    )

    fun createClubs(teamId: Int): List<OpponentClub> =
        clubs.mapIndexed { index, c ->
            OpponentClub(
                teamId = teamId,
                name = c.name,
                shortName = c.shortName,
                stadium = c.stadium,
                sortOrder = index
            )
        }

    /**
     * Genera 10 jornadas alternando local/visitante.
     * [clubIds] debe tener el mismo orden que [createClubs].
     */
    fun createFixtures(teamId: Int, clubIds: List<Int>): List<SeasonFixture> {
        if (clubIds.isEmpty()) return emptyList()
        return clubIds.mapIndexed { index, clubId ->
            SeasonFixture(
                teamId = teamId,
                matchday = index + 1,
                opponentClubId = clubId,
                isHome = index % 2 == 0,
                date = "",
                time = "12:00"
            )
        }
    }
}
