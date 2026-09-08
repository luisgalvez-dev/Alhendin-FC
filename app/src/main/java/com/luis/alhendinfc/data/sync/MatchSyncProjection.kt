package com.luis.alhendinfc.data.sync

import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.domain.model.MatchStatus

/**
 * Campos durables de un partido (ficha, rival, resultado).
 * El estado LIVE no entra aquí y no se sincroniza en tiempo real.
 * Un FINISHED sí expone resultado y ficha; convocatoria y eventos van en sus propias proyecciones.
 */
data class MatchSyncProjection(
    val syncId: String,
    val teamId: Int,
    val rival: String,
    val stadium: String,
    val date: String,
    val time: String,
    val matchday: Int,
    val isHome: Boolean,
    val durationPerPart: Int,
    val numParts: Int,
    val formation: String,
    val notes: String,
    val status: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val opponentClubId: Int?,
    val rivalShieldUri: String?,
    val dateEpochDay: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

/** Estado transitorio del dispositivo del analista mientras el partido está LIVE. */
data class MatchLiveLocalState(
    val livePeriod: Int,
    val liveElapsedSeconds: Int,
    val liveClockRunning: Boolean,
    val liveClockAnchorWallMs: Long,
    val fieldSecondsJson: String,
    val fieldPositionsJson: String
)

/** Convocatoria durable. [MatchPlayerEntity.isOnField] es estado LIVE local. */
data class MatchPlayerSyncProjection(
    val syncId: String,
    val matchId: Int,
    val playerId: Int,
    val callupStatus: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

object MatchSyncMapper {
    private val liveLocalKeys = setOf(
        "livePeriod",
        "liveElapsedSeconds",
        "liveClockRunning",
        "liveClockAnchorWallMs",
        "fieldSecondsJson",
        "fieldPositionsJson"
    )

    fun toProjection(match: MatchEntity): MatchSyncProjection =
        MatchSyncProjection(
            syncId = match.syncId,
            teamId = match.teamId,
            rival = match.rival,
            stadium = match.stadium,
            date = match.date,
            time = match.time,
            matchday = match.matchday,
            isHome = match.isHome,
            durationPerPart = match.durationPerPart,
            numParts = match.numParts,
            formation = match.formation,
            notes = match.notes,
            status = match.status,
            homeScore = match.homeScore,
            awayScore = match.awayScore,
            opponentClubId = match.opponentClubId,
            rivalShieldUri = match.rivalShieldUri,
            dateEpochDay = match.dateEpochDay,
            createdAt = match.createdAt,
            updatedAt = match.updatedAt,
            deletedAt = match.deletedAt
        )

    fun liveLocalState(match: MatchEntity): MatchLiveLocalState =
        MatchLiveLocalState(
            livePeriod = match.livePeriod,
            liveElapsedSeconds = match.liveElapsedSeconds,
            liveClockRunning = match.liveClockRunning,
            liveClockAnchorWallMs = match.liveClockAnchorWallMs,
            fieldSecondsJson = match.fieldSecondsJson,
            fieldPositionsJson = match.fieldPositionsJson
        )

    fun toPlayerProjection(row: MatchPlayerEntity): MatchPlayerSyncProjection =
        MatchPlayerSyncProjection(
            syncId = row.syncId,
            matchId = row.matchId,
            playerId = row.playerId,
            callupStatus = row.callupStatus,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            deletedAt = row.deletedAt
        )

    fun liveLocalFieldNames(): Set<String> = liveLocalKeys

    fun shouldPushLiveLocalState(): Boolean = false

    fun finishedExposesDurableResult(match: MatchEntity): Boolean =
        match.status == MatchStatus.FINISHED.name
}
