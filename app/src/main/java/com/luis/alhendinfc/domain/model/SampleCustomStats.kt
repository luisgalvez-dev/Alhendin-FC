package com.luis.alhendinfc.domain.model

/**
 * Tipos de evento de prueba por equipo.
 * Incluye estadísticas de campo y específicas de portero.
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
        )
    )
}
