package com.luis.alhendinfc.domain.model

/**
 * Plantilla de prueba (18 jugadores) para desarrollo.
 * Se inserta automáticamente si el equipo no tiene jugadores.
 */
object SampleSquad {

    fun createPlayers(teamId: Int): List<Player> = listOf(
        Player(teamId = teamId, name = "Carlos Gilarte", alias = "Gilarte", position = PlayerPosition.PORTERO, jerseyNumber = 1),
        Player(teamId = teamId, name = "Hugo Morales", alias = "Morales", position = PlayerPosition.LATERAL_DERECHO, jerseyNumber = 2),
        Player(teamId = teamId, name = "Ivan Ruiz", alias = "Ruiz", position = PlayerPosition.CENTRAL_DERECHO, jerseyNumber = 4),
        Player(teamId = teamId, name = "Pablo Serrano", alias = "Serrano", position = PlayerPosition.CENTRAL_IZQUIERDO, jerseyNumber = 5),
        Player(teamId = teamId, name = "Luis Navarro", alias = "Navarro", position = PlayerPosition.LATERAL_IZQUIERDO, jerseyNumber = 3),
        Player(teamId = teamId, name = "Marc Dominguez", alias = "Domi", position = PlayerPosition.MEDIOCENTRO_DEFENSIVO, jerseyNumber = 6),
        Player(teamId = teamId, name = "Andres Vega", alias = "Vega", position = PlayerPosition.INTERIOR_DERECHO, jerseyNumber = 8),
        Player(teamId = teamId, name = "Javier Ortiz", alias = "Ortiz", position = PlayerPosition.INTERIOR_IZQUIERDO, jerseyNumber = 10),
        Player(teamId = teamId, name = "Diego Castillo", alias = "Castillo", position = PlayerPosition.MEDIOCENTRO_OFENSIVO, jerseyNumber = 14),
        Player(teamId = teamId, name = "Sergio Pena", alias = "Pena", position = PlayerPosition.EXTREMO_DERECHO, jerseyNumber = 7),
        Player(teamId = teamId, name = "Nicolas Bravo", alias = "Bravo", position = PlayerPosition.EXTREMO_IZQUIERDO, jerseyNumber = 11),
        Player(teamId = teamId, name = "Adrian Molina", alias = "Molina", position = PlayerPosition.DELANTERO, jerseyNumber = 9),
        // Suplentes / rotacion
        Player(teamId = teamId, name = "Tomas Herrera", alias = "Herrera", position = PlayerPosition.PORTERO, jerseyNumber = 13),
        Player(teamId = teamId, name = "Raul Campos", alias = "Campos", position = PlayerPosition.CENTRAL_DERECHO, jerseyNumber = 15),
        Player(teamId = teamId, name = "Fernando Lozano", alias = "Lozano", position = PlayerPosition.MEDIOCENTRO_DEFENSIVO, jerseyNumber = 16),
        Player(teamId = teamId, name = "Miguel Santos", alias = "Santos", position = PlayerPosition.INTERIOR_DERECHO, jerseyNumber = 17),
        Player(teamId = teamId, name = "Oscar Delgado", alias = "Delgado", position = PlayerPosition.EXTREMO_IZQUIERDO, jerseyNumber = 18),
        Player(teamId = teamId, name = "Daniel Rios", alias = "Rios", position = PlayerPosition.DELANTERO, jerseyNumber = 19)
    )
}
