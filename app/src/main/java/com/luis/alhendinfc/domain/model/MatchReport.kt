package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType

/**
 * Informe de partido: el mismo [Attachment] con parentType MATCH.
 * No se copia al rival; se resuelve por [Match.opponentClubId] + [Match.syncId].
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

    fun forOpponent(
        matches: List<Match>,
        attachments: List<Attachment>,
        opponentClubId: Int,
        homeLabel: String = "Alhendín"
    ): List<MatchReportRef> {
        val byParent = attachments.filter { it.isLiveMatchReport }.groupBy { it.parentSyncId }
        return matches
            .filter { it.opponentClubId == opponentClubId && it.syncId.isNotBlank() }
            .flatMap { match ->
                (byParent[match.syncId] ?: emptyList()).map { att ->
                    MatchReportRef(match = match, attachment = att, homeLabel = homeLabel)
                }
            }
    }
}

private val Attachment.isLiveMatchReport: Boolean
    get() = parentType == AttachmentParentType.MATCH && deletedAt == null
