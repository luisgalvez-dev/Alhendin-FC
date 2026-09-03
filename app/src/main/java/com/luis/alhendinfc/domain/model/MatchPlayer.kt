package com.luis.alhendinfc.domain.model

data class MatchPlayer(
    val id: Int = 0,
    val matchId: Int,
    val playerId: Int,
    val callupStatus: CallupStatus = CallupStatus.NONE,
    /** true = está en el campo ahora mismo (titular o entró de cambio) */
    val isOnField: Boolean = false
)
