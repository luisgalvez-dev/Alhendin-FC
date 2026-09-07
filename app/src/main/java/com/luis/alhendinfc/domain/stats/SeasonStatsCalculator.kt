package com.luis.alhendinfc.domain.stats

import com.luis.alhendinfc.domain.model.CustomStatAppliesTo
import com.luis.alhendinfc.domain.model.CustomStatType
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.PlayerCustomStatCount
import com.luis.alhendinfc.domain.model.PlayerSeasonStats
import com.luis.alhendinfc.domain.model.StatisticType

/**
 * Estadísticas de temporada. Las tarjetas se agrupan por partido:
 * dos amarillas en partidos distintos no son una roja.
 * Un partido aporta como máximo una expulsión por jugador.
 */
object SeasonStatsCalculator {

    data class CardTotals(
        val yellowCards: Int,
        val redCards: Int
    )

    fun eventsForTeamPlayer(
        events: List<MatchEvent>,
        playerId: Int,
        teamMatchIds: Set<Int>
    ): List<MatchEvent> =
        events.filter { event ->
            event.playerId == playerId && event.matchId in teamMatchIds
        }

    fun countEventsWithCodeForTeam(
        typeCode: String,
        teamId: Int,
        events: List<MatchEvent>,
        matchIdToTeamId: Map<Int, Int>
    ): Int = events.count { event ->
        event.typeCode == typeCode && matchIdToTeamId[event.matchId] == teamId
    }

    fun cardTotals(playerEvents: List<MatchEvent>): CardTotals {
        val yellowCards = playerEvents
            .filter { it.type == StatisticType.YELLOW_CARD }
            .sumOf { it.value }

        val redCards = playerEvents
            .groupBy { it.matchId }
            .values
            .count { matchEvents -> sentOffInMatch(matchEvents) }

        return CardTotals(yellowCards = yellowCards, redCards = redCards)
    }

    /** Roja directa o doble amarilla; nunca más de una expulsión por partido. */
    fun sentOffInMatch(matchEvents: List<MatchEvent>): Boolean {
        val yellowInMatch = matchEvents
            .filter { it.type == StatisticType.YELLOW_CARD }
            .sumOf { it.value }
        val hasDirectRed = matchEvents.any { it.type == StatisticType.RED_CARD }
        return hasDirectRed || yellowInMatch >= 2
    }

    fun forPlayer(
        player: Player,
        events: List<MatchEvent>,
        callups: List<MatchPlayer>,
        customTypes: List<CustomStatType>,
        teamMatchIds: Set<Int>
    ): PlayerSeasonStats {
        val playerEvents = eventsForTeamPlayer(events, player.id, teamMatchIds)
        val cards = cardTotals(playerEvents)

        val playerCustomTypes = customTypes
            .filter { it.appliesTo != CustomStatAppliesTo.RIVAL }
            .sortedBy { it.sortOrder }

        val customStats = playerCustomTypes
            .filter { type ->
                type.appliesTo.matches(player.position) ||
                    playerEvents.any { it.typeCode == type.code }
            }
            .filter { type -> type.isActive || playerEvents.any { it.typeCode == type.code } }
            .map { type ->
                PlayerCustomStatCount(
                    code = type.code,
                    label = type.label,
                    shortLabel = type.shortLabel.ifBlank { type.label },
                    value = playerEvents
                        .filter { it.typeCode == type.code }
                        .sumOf { it.value }
                )
            }

        return PlayerSeasonStats(
            player = player,
            matchesPlayed = callups
                .filter { it.playerId == player.id && it.matchId in teamMatchIds }
                .map { it.matchId }
                .distinct()
                .size,
            goals = playerEvents.filter { it.type == StatisticType.GOAL }.sumOf { it.value },
            assists = playerEvents.filter { it.type == StatisticType.ASSIST }.sumOf { it.value },
            yellowCards = cards.yellowCards,
            redCards = cards.redCards,
            customStats = customStats
        )
    }
}
