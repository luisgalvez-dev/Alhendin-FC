package com.luis.alhendinfc.domain.model

/**
 * Catálogo fijo de estadísticas del partido (gol, tarjetas, etc.).
 * Los tipos personalizados viven en CustomStatType por equipo.
 */
enum class StatisticType(
    val code: String,
    val label: String,
    val shortLabel: String,
    val affectsTeamScore: Boolean = false,
    val requiresPlayer: Boolean = true,
    val isQuickAction: Boolean = true
) {
    GOAL(
        code = "GOAL",
        label = "Gol",
        shortLabel = "Gol",
        affectsTeamScore = true
    ),
    ASSIST(
        code = "ASSIST",
        label = "Asistencia",
        shortLabel = "Asist."
    ),
    YELLOW_CARD(
        code = "YELLOW_CARD",
        label = "Tarjeta amarilla",
        shortLabel = "Amarilla"
    ),
    RED_CARD(
        code = "RED_CARD",
        label = "Tarjeta roja",
        shortLabel = "Roja"
    ),
    SUBSTITUTION(
        code = "SUBSTITUTION",
        label = "Cambio",
        shortLabel = "Cambio",
        isQuickAction = true
    ),
    RIVAL_GOAL(
        code = "RIVAL_GOAL",
        label = "Gol rival",
        shortLabel = "Gol riv.",
        affectsTeamScore = true,
        requiresPlayer = false
    );

    companion object {
        fun fromCodeOrNull(code: String): StatisticType? =
            entries.firstOrNull { it.code == code }

        /** Acciones rápidas del partido en vivo (sin el cambio, que tiene UI propia). */
        fun quickActions(): List<StatisticType> = listOf(
            GOAL, ASSIST, YELLOW_CARD, RED_CARD, RIVAL_GOAL
        )
    }
}
