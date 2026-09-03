package com.luis.alhendinfc.domain.model

data class MatchEvent(
    val id: Int = 0,
    val matchId: Int,
    val typeCode: String,
    val playerId: Int? = null,
    /** Jugador relacionado: asistencia → goleador, o cambio → quien entra */
    val relatedPlayerId: Int? = null,
    val minute: Int = 0,
    val period: Int = 1,
    val value: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Tipo built-in, o null si es estadística personalizada. */
    val type: StatisticType? get() = StatisticType.fromCodeOrNull(typeCode)

    companion object {
        fun builtin(
            matchId: Int,
            type: StatisticType,
            playerId: Int? = null,
            relatedPlayerId: Int? = null,
            minute: Int = 0,
            period: Int = 1,
            value: Int = 1
        ) = MatchEvent(
            matchId = matchId,
            typeCode = type.code,
            playerId = playerId,
            relatedPlayerId = relatedPlayerId,
            minute = minute,
            period = period,
            value = value
        )

        fun custom(
            matchId: Int,
            typeCode: String,
            playerId: Int? = null,
            minute: Int = 0,
            period: Int = 1,
            value: Int = 1
        ) = MatchEvent(
            matchId = matchId,
            typeCode = typeCode,
            playerId = playerId,
            minute = minute,
            period = period,
            value = value
        )
    }
}
