package com.luis.alhendinfc.domain.model

data class PlayerCustomStatCount(
    val code: String,
    val label: String,
    val shortLabel: String,
    val value: Int = 0
)

data class PlayerSeasonStats(
    val player: Player,
    val matchesPlayed: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    /** Segundos reales en campo, sumados de partidos FINISHED. */
    val secondsPlayed: Int = 0,
    /** Minutos enteros derivados de [secondsPlayed] (900 s → 15). */
    val minutesPlayed: Int = 0,
    /** Partidos FINISHED alineado como TITULAR (once inicial). */
    val starts: Int = 0,
    /**
     * Partidos FINISHED convocado (TITULAR o SUPLENTE).
     * Con el filtro actual de convocatorias coincide con [matchesPlayed] (PJ).
     */
    val callUps: Int = 0,
    /** Stats personalizadas del equipo (robos, paradas, etc.), sin rival. */
    val customStats: List<PlayerCustomStatCount> = emptyList()
)
