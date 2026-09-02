package com.luis.alhendinfc.domain.model

data class MatchPlayer(
    val id: Int = 0,
    val matchId: Int,
    val playerId: Int,
    val callupStatus: CallupStatus = CallupStatus.NONE
)
