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
    /** Stats personalizadas del equipo (robos, paradas, etc.), sin rival. */
    val customStats: List<PlayerCustomStatCount> = emptyList()
)
