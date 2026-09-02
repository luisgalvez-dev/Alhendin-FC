package com.luis.alhendinfc.domain.model

data class Match(
    val id: Int = 0,
    val teamId: Int,
    val rival: String = "",
    val stadium: String = "",
    val date: String = "",
    val time: String = "",
    val matchday: Int = 1,
    val isHome: Boolean = true,
    val durationPerPart: Int = 45,
    val numParts: Int = 2,
    val formation: String = "",
    val notes: String = "",
    val status: MatchStatus = MatchStatus.OPEN,
    val homeScore: Int? = null,
    val awayScore: Int? = null
)
