package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "match_table",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["teamId", "status"])
    ]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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
    val status: String = "OPEN",
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val opponentClubId: Int? = null,
    val rivalShieldUri: String? = null,
    /** Parte actual del partido en vivo (1-based). */
    val livePeriod: Int = 1,
    /** Segundos transcurridos en la parte actual. */
    val liveElapsedSeconds: Int = 0,
    /** Si el cronómetro seguía en marcha al salir (sigue contando en tiempo real). */
    val liveClockRunning: Boolean = false,
    /**
     * Ancla de reloj de pared: `elapsed ≈ (now - anchor) / 1000` mientras corre.
     * 0 si está pausado.
     */
    val liveClockAnchorWallMs: Long = 0L,
    /** Mapa "playerId:seconds,playerId:seconds" de minutos en campo. */
    val fieldSecondsJson: String = "",
    /** Posiciones en campo "playerId:x:y,..." para no recalcular (ni solapar) al reabrir. */
    val fieldPositionsJson: String = ""
)
