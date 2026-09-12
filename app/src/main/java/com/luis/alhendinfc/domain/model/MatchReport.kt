package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType

/**
 * Informe de partido: el mismo [Attachment] con parentType MATCH.
 * No se copia al rival; se resuelve por [Match.syncId] + identidad portable del club.
 */
data class MatchReportRef(
    val match: Match,
    val attachment: Attachment,
    val homeLabel: String = "Alhendín"
) {
    val matchHeading: String
        get() {
            val them = match.rival.ifBlank { "Rival" }
            val left = if (match.isHome) homeLabel else them
            val right = if (match.isHome) them else homeLabel
            return if (match.date.isBlank()) "$left - $right" else "$left - $right · ${match.date}"
        }
}

object MatchReports {

    fun activeForMatch(attachments: List<Attachment>, matchSyncId: String): List<Attachment> {
        if (matchSyncId.isBlank()) return emptyList()
        return attachments.filter { it.isLiveMatchReport && it.parentSyncId == matchSyncId }
    }

    fun portableClubSyncId(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * 1. [Match.opponentClubSyncId] vs [OpponentClub.syncId] si ambos existen.
     * 2. Id local del club (mismo dispositivo / post-apply).
     * 3. El id local del partido apunta a un club con el mismo syncId.
     */
    fun belongsToOpponent(
        match: Match,
        club: OpponentClub,
        clubsById: Map<Int, OpponentClub> = emptyMap()
    ): Boolean {
        val clubSync = portableClubSyncId(club.syncId)
        val matchSync = portableClubSyncId(match.opponentClubSyncId)
        if (matchSync != null && clubSync != null) return matchSync == clubSync
        val localId = match.opponentClubId?.takeIf { it > 0 }
        if (localId != null && localId == club.id) return true
        if (localId != null && clubSync != null) {
            val mapped = clubsById[localId]
            if (mapped != null && portableClubSyncId(mapped.syncId) == clubSync) return true
        }
        return false
    }

    fun forOpponent(
        matches: List<Match>,
        attachments: List<Attachment>,
        club: OpponentClub,
        clubsById: Map<Int, OpponentClub> = emptyMap(),
        homeLabel: String = "Alhendín"
    ): List<MatchReportRef> {
        val byParent = attachments.filter { it.isLiveMatchReport }.groupBy { it.parentSyncId }
        return matches
            .filter { it.syncId.isNotBlank() && belongsToOpponent(it, club, clubsById) }
            .flatMap { match ->
                (byParent[match.syncId] ?: emptyList()).map { att ->
                    MatchReportRef(match = match, attachment = att, homeLabel = homeLabel)
                }
            }
    }

    /** Compatibilidad: filtro solo por id local cuando no hay club portable. */
    fun forOpponent(
        matches: List<Match>,
        attachments: List<Attachment>,
        opponentClubId: Int,
        homeLabel: String = "Alhendín"
    ): List<MatchReportRef> = forOpponent(
        matches,
        attachments,
        OpponentClub(id = opponentClubId, teamId = 0, name = ""),
        homeLabel = homeLabel
    )
}

private val Attachment.isLiveMatchReport: Boolean
    get() = parentType == AttachmentParentType.MATCH && deletedAt == null
