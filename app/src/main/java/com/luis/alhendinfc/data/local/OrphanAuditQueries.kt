package com.luis.alhendinfc.data.local

/** Consultas de diagnóstico de huérfanos. Solo lectura; no reparan datos. */
object OrphanAuditQueries {
    const val PLAYERS_WITHOUT_TEAM =
        "SELECT p.id FROM player p LEFT JOIN team t ON t.id = p.teamId WHERE t.id IS NULL"
    const val MATCHES_WITHOUT_TEAM =
        "SELECT m.id FROM match_table m LEFT JOIN team t ON t.id = m.teamId WHERE t.id IS NULL"
    const val MATCH_PLAYERS_ORPHAN =
        """
        SELECT mp.id FROM match_player mp
        LEFT JOIN match_table m ON m.id = mp.matchId
        LEFT JOIN player p ON p.id = mp.playerId
        WHERE m.id IS NULL OR p.id IS NULL
        """
    const val EVENTS_WITHOUT_MATCH =
        "SELECT e.id FROM match_event e LEFT JOIN match_table m ON m.id = e.matchId WHERE m.id IS NULL"
    const val EVENTS_PLAYER_ORPHAN =
        """
        SELECT e.id FROM match_event e
        LEFT JOIN player p ON p.id = e.playerId
        WHERE e.playerId IS NOT NULL AND p.id IS NULL
        """
    const val EVENTS_RELATED_PLAYER_ORPHAN =
        """
        SELECT e.id FROM match_event e
        LEFT JOIN player p ON p.id = e.relatedPlayerId
        WHERE e.relatedPlayerId IS NOT NULL AND p.id IS NULL
        """
    const val CLUBS_WITHOUT_TEAM =
        "SELECT c.id FROM opponent_club c LEFT JOIN team t ON t.id = c.teamId WHERE t.id IS NULL"
    const val FIXTURES_ORPHAN =
        """
        SELECT f.id FROM season_fixture f
        LEFT JOIN team t ON t.id = f.teamId
        LEFT JOIN opponent_club c ON c.id = f.opponentClubId
        WHERE t.id IS NULL OR c.id IS NULL
        """
    const val MATCHES_CLUB_ORPHAN =
        """
        SELECT m.id FROM match_table m
        LEFT JOIN opponent_club c ON c.id = m.opponentClubId
        WHERE m.opponentClubId IS NOT NULL AND c.id IS NULL
        """
}
