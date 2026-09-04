package com.luis.alhendinfc.domain.model

/**
 * Tipos de evento de prueba por equipo.
 * Incluye campo, portero y rival.
 */
object SampleCustomStats {

    fun createTypes(teamId: Int): List<CustomStatType> = listOf(
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_SHOT_ON_TARGET",
            label = "Tiro a puerta",
            shortLabel = "Tiro",
            appliesTo = CustomStatAppliesTo.OUTFIELD,
            sortOrder = 0
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_FOUL",
            label = "Falta",
            shortLabel = "Falta",
            appliesTo = CustomStatAppliesTo.ALL,
            sortOrder = 1
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_CORNER",
            label = "Corner",
            shortLabel = "Corner",
            appliesTo = CustomStatAppliesTo.OUTFIELD,
            sortOrder = 2
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_KEY_PASS",
            label = "Pase clave",
            shortLabel = "P.clave",
            appliesTo = CustomStatAppliesTo.OUTFIELD,
            sortOrder = 3
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_SAVE",
            label = "Parada",
            shortLabel = "Parada",
            appliesTo = CustomStatAppliesTo.GOALKEEPER,
            sortOrder = 4
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_GK_CLAIM",
            label = "Salida / despeje",
            shortLabel = "Salida",
            appliesTo = CustomStatAppliesTo.GOALKEEPER,
            sortOrder = 5
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_GK_CATCH",
            label = "Bloqueo",
            shortLabel = "Bloqueo",
            appliesTo = CustomStatAppliesTo.GOALKEEPER,
            sortOrder = 6
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_RIVAL_WING",
            label = "Ataque por banda",
            shortLabel = "Banda",
            appliesTo = CustomStatAppliesTo.RIVAL,
            sortOrder = 10
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_RIVAL_PRESS",
            label = "Presión alta",
            shortLabel = "Presión",
            appliesTo = CustomStatAppliesTo.RIVAL,
            sortOrder = 11
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_RIVAL_STEAL",
            label = "Robo / recuperación",
            shortLabel = "Robo",
            appliesTo = CustomStatAppliesTo.RIVAL,
            sortOrder = 12
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_RIVAL_COUNTER",
            label = "Contraataque",
            shortLabel = "Contra",
            appliesTo = CustomStatAppliesTo.RIVAL,
            sortOrder = 13
        ),
        CustomStatType(
            teamId = teamId,
            code = "SAMPLE_RIVAL_SET_PIECE",
            label = "Balón parado peligroso",
            shortLabel = "B.parado",
            appliesTo = CustomStatAppliesTo.RIVAL,
            sortOrder = 14
        )
    )
}
